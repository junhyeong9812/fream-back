package com.fream.back.domain.chatQuestion.service;


import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTUsageLogDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTUsageStatsDto;
import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.domain.chatQuestion.entity.GPTUsageLog;
import com.fream.back.domain.chatQuestion.repository.GPTUsageLogRepository;
import com.fream.back.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTUsageService {

    private final GPTUsageLogRepository gptUsageLogRepository;

    // GPT API 사용 후 사용량 로그 기록
    @Transactional
    public void logGPTUsage(GPTResponseDto response, User user, String requestType, ChatQuestion chatQuestion) {
        try {
            // GPT 응답에서 사용량 정보 추출
            int promptTokens = (response.getUsage() != null) ? response.getUsage().getPrompt_tokens() : 0;
            int completionTokens = (response.getUsage() != null) ? response.getUsage().getCompletion_tokens() : 0;
            int totalTokens = (response.getUsage() != null) ? response.getUsage().getTotal_tokens() : 0;

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

            log.info("GPT 사용량 기록 완료: 모델={}, 총 토큰={}, 사용자={}",
                    response.getModel(), totalTokens, (user != null ? user.getEmail() : "시스템"));

        } catch (Exception e) {
            // 로그 기록 실패 시 서비스 전체가 실패하지 않도록 예외 처리
            log.error("GPT 사용량 기록 중 오류 발생: ", e);
        }
    }

    // 특정 기간의 사용량 통계 조회 (관리자용)
    @Transactional(readOnly = true)
    public GPTUsageStatsDto getUsageStatistics(LocalDate startDate, LocalDate endDate) {
        // 날짜 범위 설정 (시작일 00:00:00부터 종료일 23:59:59까지)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

        // 총 사용량 조회
        Integer totalTokens = gptUsageLogRepository.getTotalTokensUsedBetweenDates(startDateTime, endDateTime);
        if (totalTokens == null) totalTokens = 0;

        // 일별 사용량 조회
        List<Object[]> dailyStats = gptUsageLogRepository.getDailyTokenUsage(startDateTime, endDateTime);
        List<GPTUsageStatsDto.DailyUsage> dailyUsageList = dailyStats.stream()
                .map(row -> {
                    LocalDate date = LocalDate.parse(row[0].toString());
                    int tokens = ((Number) row[1]).intValue();
                    return new GPTUsageStatsDto.DailyUsage(date, tokens);
                })
                .collect(Collectors.toList());

        // 모델별 사용량 조회
        List<Object[]> modelStats = gptUsageLogRepository.getTokenUsageByModelBetweenDates(startDateTime, endDateTime);
        Map<String, Integer> usageByModel = new HashMap<>();
        for (Object[] row : modelStats) {
            String model = (String) row[0];
            Integer tokens = ((Number) row[1]).intValue();
            usageByModel.put(model, tokens);
        }

        // 요청 유형별 사용량 조회
        List<Object[]> typeStats = gptUsageLogRepository.getTokenUsageByRequestTypeBetweenDates(startDateTime, endDateTime);
        Map<String, Integer> usageByType = new HashMap<>();
        for (Object[] row : typeStats) {
            String type = (String) row[0];
            Integer tokens = ((Number) row[1]).intValue();
            usageByType.put(type, tokens);
        }

        // 비용 계산 (모델에 따라 다름, 여기서는 GPT-3.5를 기준으로 계산)
        // 가격 정보: https://openai.com/pricing
        // GPT-3.5 Turbo: 입력 $0.0015, 출력 $0.002 / 1K 토큰
        int estimatedCost = 0;
        for (Map.Entry<String, Integer> entry : usageByModel.entrySet()) {
            String model = entry.getKey();
            int tokens = entry.getValue();

            if (model.contains("gpt-3.5")) {
                // 간단한 계산이므로 정확한 입력/출력 비율을 알 수 없어 평균 가격 적용
                estimatedCost += (int) (tokens * 0.0018); // 약 $0.0018 / 1K 토큰 (평균)
            } else if (model.contains("gpt-4")) {
                // GPT-4는 더 비쌈, 약 $0.03 / 1K 토큰으로 가정
                estimatedCost += (int) (tokens * 0.03);
            }
        }

        // 결과 반환
        return GPTUsageStatsDto.builder()
                .totalTokensUsed(totalTokens)
                .estimatedCost(estimatedCost)
                .dailyUsage(dailyUsageList)
                .usageByModel(usageByModel)
                .usageByRequestType(usageByType)
                .build();
    }

    // 사용량 로그 페이징 조회 (관리자용)
    @Transactional(readOnly = true)
    public Page<GPTUsageLogDto> getUsageLogs(Pageable pageable) {
        Page<GPTUsageLog> logs = gptUsageLogRepository.findAllByOrderByCreatedDateDesc(pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return logs.map(log -> {
            String userName = log.getUser() != null ? log.getUser().getEmail() : "System";
            String questionContent = log.getChatQuestion() != null ?
                    (log.getChatQuestion().getQuestion().length() > 30 ?
                            log.getChatQuestion().getQuestion().substring(0, 30) + "..." :
                            log.getChatQuestion().getQuestion()) :
                    "-";

            return GPTUsageLogDto.builder()
                    .id(log.getId())
                    .userName(userName)
                    .requestType(log.getRequestType())
                    .promptTokens(log.getPromptTokens())
                    .completionTokens(log.getCompletionTokens())
                    .totalTokens(log.getTotalTokens())
                    .modelName(log.getModelName())
                    .requestDate(log.getCreatedDate().format(formatter))
                    .questionContent(questionContent)
                    .build();
        });
    }

    // 총 누적 토큰 사용량 조회
    @Transactional(readOnly = true)
    public int getTotalTokensUsed() {
        return gptUsageLogRepository.getTotalTokensUsedBetweenDates(
                LocalDateTime.of(2000, 1, 1, 0, 0), // 과거 날짜
                LocalDateTime.now() // 현재까지
        );
    }
}