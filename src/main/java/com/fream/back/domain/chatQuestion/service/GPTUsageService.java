package com.fream.back.domain.chatQuestion.service;

import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTDailyUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTModelUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTRequestTypeUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTUsageLogDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTUsageStatsDto;
import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.domain.chatQuestion.entity.GPTUsageLog;
import com.fream.back.domain.chatQuestion.exception.ChatPermissionException;
import com.fream.back.domain.chatQuestion.exception.ChatQueryException;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.chatQuestion.exception.GPTUsageException;
import com.fream.back.domain.chatQuestion.repository.GPTUsageLogRepository;
import com.fream.back.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GPT 사용량 관리 서비스
 * GPT API 사용량 로깅 및 통계 조회를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GPTUsageService {

    private final GPTUsageLogRepository gptUsageLogRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * GPT API 사용 후 사용량 로그 기록
     * 서비스 실패 시에도 로그 기록 실패로 인한 예외 전파를 방지하기 위해 독립적인 트랜잭션으로 처리
     *
     * @param response GPT 응답 DTO
     * @param user 사용자 엔티티
     * @param requestType 요청 유형
     * @param chatQuestion 채팅 질문 엔티티
     */
    @Transactional(rollbackFor = Exception.class)
    public void logGPTUsage(GPTResponseDto response, User user, String requestType, ChatQuestion chatQuestion) {
        if (response == null || response.getUsage() == null) {
            log.warn("GPT 사용량 기록 실패: 응답 객체가 null이거나 사용량 정보가 없습니다.");
            return;
        }

        try {
            // GPT 응답에서 사용량 정보 추출
            int promptTokens = response.getUsage().getPrompt_tokens();
            int completionTokens = response.getUsage().getCompletion_tokens();
            int totalTokens = response.getUsage().getTotal_tokens();

            // 사용량 로그 저장
            GPTUsageLog usageLog = GPTUsageLog.builder()
                    .user(user)
                    .requestType(requestType)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .modelName(response.getModel())
                    .requestId(response.getId())
                    .chatQuestion(chatQuestion)
                    .build();

            gptUsageLogRepository.save(usageLog);

            log.info("GPT 사용량 기록 완료: 모델={}, 총 토큰={}, 사용자={}, 요청 ID={}",
                    response.getModel(),
                    totalTokens,
                    (user != null ? user.getEmail() : "시스템"),
                    response.getId());

        } catch (Exception e) {
            // 로그 기록 실패 시 서비스 전체가 실패하지 않도록 예외 처리
            log.error("GPT 사용량 기록 중 오류 발생: {}", e.getMessage(), e);
            // 이 메서드의 예외는 상위로 전파하지 않음 (비즈니스 로직에 영향을 주지 않도록)
        }
    }

    /**
     * 특정 기간의 사용량 통계 조회 (관리자용)
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return GPT 사용량 통계 DTO
     * @throws GPTUsageException 유효하지 않은 날짜 범위 또는 통계 조회 실패 시
     */
    @Transactional(readOnly = true)
    public GPTUsageStatsDto getUsageStatistics(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        try {
            // 날짜 범위 설정 (시작일 00:00:00부터 종료일 23:59:59까지)
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

            // 총 사용량 조회
            Integer totalTokens = gptUsageLogRepository.getTotalTokensUsedBetweenDates(startDateTime, endDateTime);
            if (totalTokens == null) totalTokens = 0;

            // 일별 사용량 조회 - QueryDSL로 개선
            List<GPTDailyUsageDto> dailyStats = gptUsageLogRepository.getDailyTokenUsage(startDateTime, endDateTime);
            List<GPTUsageStatsDto.DailyUsage> dailyUsageList = dailyStats.stream()
                    .map(dto -> new GPTUsageStatsDto.DailyUsage(dto.getDateAsLocalDate(), dto.getTokenCount()))
                    .collect(Collectors.toList());

            // 모델별 사용량 조회 - QueryDSL로 개선
            List<GPTModelUsageDto> modelStats = gptUsageLogRepository.getTokenUsageByModelBetweenDates(startDateTime, endDateTime);
            Map<String, Integer> usageByModel = modelStats.stream()
                    .collect(Collectors.toMap(
                            GPTModelUsageDto::getModelName,
                            GPTModelUsageDto::getTokenCount,
                            (a, b) -> a + b, // 중복 키 처리
                            HashMap::new
                    ));

            // 요청 유형별 사용량 조회 - QueryDSL로 개선
            List<GPTRequestTypeUsageDto> typeStats = gptUsageLogRepository.getTokenUsageByRequestTypeBetweenDates(startDateTime, endDateTime);
            Map<String, Integer> usageByType = typeStats.stream()
                    .collect(Collectors.toMap(
                            GPTRequestTypeUsageDto::getRequestType,
                            GPTRequestTypeUsageDto::getTokenCount,
                            (a, b) -> a + b, // 중복 키 처리
                            HashMap::new
                    ));

            // 비용 계산
            int estimatedCost = calculateEstimatedCost(usageByModel);

            // 결과 반환
            return GPTUsageStatsDto.builder()
                    .totalTokensUsed(totalTokens)
                    .estimatedCost(estimatedCost)
                    .dailyUsage(dailyUsageList)
                    .usageByModel(usageByModel)
                    .usageByRequestType(usageByType)
                    .build();

        } catch (DataAccessException e) {
            log.error("GPT 사용량 통계 조회 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            throw new ChatQueryException(ChatQuestionErrorCode.USAGE_STATS_QUERY_ERROR,
                    "GPT 사용량 통계를 조회하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (GPTUsageException e) {
            // 이미 처리된 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("GPT 사용량 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new GPTUsageException(ChatQuestionErrorCode.USAGE_STATS_QUERY_ERROR,
                    "GPT 사용량 통계를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 날짜 범위 유효성 검사
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @throws GPTUsageException 유효하지 않은 날짜 범위인 경우
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new GPTUsageException(ChatQuestionErrorCode.INVALID_DATE_RANGE,
                    "시작 날짜와 종료 날짜는 필수입니다.");
        }

        if (startDate.isAfter(endDate)) {
            throw new GPTUsageException(ChatQuestionErrorCode.INVALID_DATE_RANGE,
                    "시작 날짜는 종료 날짜보다 이전이어야 합니다.");
        }

        // 너무 긴 기간의 조회 제한 (선택 사항)
        if (startDate.plusMonths(6).isBefore(endDate)) {
            throw new GPTUsageException(ChatQuestionErrorCode.INVALID_DATE_RANGE,
                    "통계 조회 기간은 최대 6개월까지 가능합니다.");
        }
    }

    /**
     * 모델별 예상 비용 계산
     *
     * @param usageByModel 모델별 사용량 맵
     * @return 예상 비용 (센트 단위)
     */
    private int calculateEstimatedCost(Map<String, Integer> usageByModel) {
        int estimatedCost = 0;
        for (Map.Entry<String, Integer> entry : usageByModel.entrySet()) {
            String model = entry.getKey();
            int tokens = entry.getValue();

            if (model.contains("gpt-3.5")) {
                // GPT-3.5 기준 비용 (대략적인 평균값)
                estimatedCost += (int) (tokens * 0.0018); // 약 $0.0018 / 1K 토큰
            } else if (model.contains("gpt-4")) {
                // GPT-4 기준 비용
                estimatedCost += (int) (tokens * 0.03); // 약 $0.03 / 1K 토큰
            } else {
                // 기타 모델은 GPT-3.5와 동일하게 계산
                estimatedCost += (int) (tokens * 0.0018);
            }
        }
        return estimatedCost;
    }

    /**
     * 사용량 로그 페이징 조회 (관리자용)
     *
     * @param pageable 페이징 정보
     * @return GPT 사용량 로그 DTO 페이지
     * @throws ChatQueryException 로그 조회 실패 시
     */
    @Transactional(readOnly = true)
    public Page<GPTUsageLogDto> getUsageLogs(Pageable pageable) {
        try {
            Page<GPTUsageLog> logs = gptUsageLogRepository.findAllByOrderByCreatedDateDesc(pageable);
            return logs.map(this::convertToDto);
        } catch (DataAccessException e) {
            log.error("GPT 사용량 로그 조회 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            throw new ChatQueryException(ChatQuestionErrorCode.USAGE_STATS_QUERY_ERROR,
                    "GPT 사용량 로그를 조회하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("GPT 사용량 로그 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new ChatQueryException(ChatQuestionErrorCode.USAGE_STATS_QUERY_ERROR,
                    "GPT 사용량 로그를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * GPTUsageLog 엔티티를 DTO로 변환
     *
     * @param log GPT 사용량 로그 엔티티
     * @return GPT 사용량 로그 DTO
     */
    private GPTUsageLogDto convertToDto(GPTUsageLog log) {
        String userName = log.getUser() != null ? log.getUser().getEmail() : "System";
        String questionContent = extractQuestionContent(log.getChatQuestion());

        return GPTUsageLogDto.builder()
                .id(log.getId())
                .userName(userName)
                .requestType(log.getRequestType())
                .promptTokens(log.getPromptTokens())
                .completionTokens(log.getCompletionTokens())
                .totalTokens(log.getTotalTokens())
                .modelName(log.getModelName())
                .requestDate(log.getCreatedDate().format(FORMATTER))
                .questionContent(questionContent)
                .build();
    }

    /**
     * 질문 내용 추출 및 요약
     *
     * @param chatQuestion 채팅 질문 엔티티
     * @return 질문 내용 요약 (최대 30자, 초과 시 생략)
     */
    private String extractQuestionContent(ChatQuestion chatQuestion) {
        if (chatQuestion == null || chatQuestion.getQuestion() == null) {
            return "-";
        }

        String question = chatQuestion.getQuestion();
        return question.length() > 30 ? question.substring(0, 30) + "..." : question;
    }

    /**
     * 총 누적 토큰 사용량 조회
     *
     * @return 총 토큰 사용량
     * @throws ChatQueryException 조회 실패 시
     */
    @Transactional(readOnly = true)
    public int getTotalTokensUsed() {
        try {
            Integer totalTokens = gptUsageLogRepository.getTotalTokensUsedBetweenDates(
                    LocalDateTime.of(2000, 1, 1, 0, 0), // 과거 날짜
                    LocalDateTime.now() // 현재까지
            );
            return totalTokens != null ? totalTokens : 0;
        } catch (DataAccessException e) {
            log.error("총 토큰 사용량 조회 중 데이터베이스 오류 발생: {}", e.getMessage(), e);
            throw new ChatQueryException(ChatQuestionErrorCode.USAGE_STATS_QUERY_ERROR,
                    "총 토큰 사용량을 조회하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("총 토큰 사용량 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new ChatQueryException(ChatQuestionErrorCode.USAGE_STATS_QUERY_ERROR,
                    "총 토큰 사용량을 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}