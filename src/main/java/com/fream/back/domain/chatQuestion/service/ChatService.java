package com.fream.back.domain.chatQuestion.service;

import com.fream.back.domain.chatQuestion.dto.chat.ChatHistoryDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.domain.chatQuestion.exception.ChatPermissionException;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.chatQuestion.exception.ChatQueryException;
import com.fream.back.domain.chatQuestion.exception.InvalidQuestionException;
import com.fream.back.domain.chatQuestion.repository.ChatQuestionRepository;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.service.query.FAQQueryService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 채팅 질문 처리 서비스
 * 사용자의 질문을 처리하고 GPT API를 통해 응답을 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatQuestionRepository chatQuestionRepository;
    private final FAQQueryService faqQueryService;
    private final GPTService gptService;
    private final UserQueryService userQueryService;
    private final GPTUsageService gptUsageService;

    /**
     * 질문 처리 및 응답 반환 (로그인 사용자만 가능)
     *
     * @param email 사용자 이메일
     * @param requestDto 질문 요청 DTO
     * @return 질문 응답 DTO
     */
    @Transactional
    public QuestionResponseDto processQuestion(String email, QuestionRequestDto requestDto) {
        validateQuestionRequest(requestDto);

        String question = requestDto.getQuestion();
        log.info("사용자 질문 처리 시작: email={}, 질문 길이={}", email, question.length());

        try {
            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // FAQ 데이터 가져오기
            List<FAQResponseDto> faqList = faqQueryService.getAllFAQs();
            log.debug("FAQ 데이터 조회 완료: {}개 항목", faqList.size());

            // GPT API로 응답 받기
            GPTResponseDto gptResponse = gptService.getGPTResponseWithUsage(question, faqList);
            String answer = gptResponse.getAnswer();

            // 질문 저장
            ChatQuestion chatQuestion = saveChatQuestion(user, question, answer);

            // GPT 사용량 로그 기록 (별도 트랜잭션으로 처리)
            gptUsageService.logGPTUsage(gptResponse, user, "FAQ_CHAT", chatQuestion);

            // 응답 반환
            return QuestionResponseDto.builder()
                    .question(question)
                    .answer(answer)
                    .createdAt(chatQuestion.getCreatedAt())
                    .build();
        } catch (AccessDeniedException e) {
            log.error("질문 권한 오류: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                    "질문 권한이 없습니다. 로그인이 필요합니다.", e);
        } catch (InvalidQuestionException | ChatQueryException e) {
            // 이미 처리된 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("질문 처리 중 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_QUESTION_SAVE_ERROR,
                    "질문 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 질문 요청 유효성 검사
     *
     * @param requestDto 질문 요청 DTO
     */
    private void validateQuestionRequest(QuestionRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidQuestionException("질문 데이터가 필요합니다.");
        }

        String question = requestDto.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            throw new InvalidQuestionException("질문 내용이 비어있습니다.");
        }

        // 추가 유효성 검사 - 컨트롤러에서 이미 체크하지만 서비스 계층에서 한번 더 확인
        if (question.length() > 2000) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.QUESTION_LENGTH_EXCEEDED,
                    "질문 길이가 제한을 초과했습니다. (최대 2000자)");
        }
    }

    /**
     * 채팅 질문을 저장
     *
     * @param user 사용자 정보
     * @param question 질문 내용
     * @param answer 답변 내용
     * @return 저장된 ChatQuestion 엔티티
     */
    private ChatQuestion saveChatQuestion(User user, String question, String answer) {
        try {
            ChatQuestion chatQuestion = ChatQuestion.builder()
                    .question(question)
                    .answer(answer)
                    .isAnswered(true)
                    .user(user)
                    .build();

            return chatQuestionRepository.save(chatQuestion);
        } catch (DataAccessException e) {
            log.error("채팅 질문 저장 중 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_QUESTION_SAVE_ERROR,
                    "채팅 질문을 저장하는 중 데이터베이스 오류가 발생했습니다.", e);
        }
    }

    /**
     * 채팅 기록 조회 (로그인 사용자만 가능)
     *
     * @param email 사용자 이메일
     * @param pageable 페이지 정보
     * @return 채팅 기록 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<ChatHistoryDto> getChatHistory(String email, Pageable pageable) {
        try {
            log.info("사용자 채팅 기록 조회: email={}, page={}, size={}",
                    email, pageable.getPageNumber(), pageable.getPageSize());

            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // 사용자의 채팅 기록 조회
            Page<ChatQuestion> chatQuestions = chatQuestionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
            log.debug("사용자 채팅 기록 조회 완료: 총 {}개 항목, 전체 {}페이지 중 {}페이지",
                    chatQuestions.getNumberOfElements(),
                    chatQuestions.getTotalPages(),
                    chatQuestions.getNumber() + 1);

            // DTO로 변환하여 반환
            return chatQuestions.map(chat -> ChatHistoryDto.builder()
                    .id(chat.getId())
                    .question(chat.getQuestion())
                    .answer(chat.getAnswer())
                    .createdAt(chat.getCreatedAt())
                    .build());
        } catch (AccessDeniedException e) {
            log.error("채팅 기록 조회 권한 오류: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                    "채팅 기록 조회 권한이 없습니다. 로그인이 필요합니다.", e);
        } catch (DataAccessException e) {
            log.error("채팅 기록 조회 중 데이터베이스 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_HISTORY_QUERY_ERROR,
                    "채팅 기록을 조회하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("채팅 기록 조회 중 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_HISTORY_QUERY_ERROR,
                    "채팅 기록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 채팅 기록 총 페이지 수 계산
     *
     * @param email 사용자 이메일
     * @param size 페이지 크기
     * @return 총 페이지 수
     */
    @Transactional(readOnly = true)
    public int getChatHistoryPageCount(String email, int size) {
        try {
            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // 전체 레코드 수 조회
            long totalRecords = chatQuestionRepository.countByUserId(user.getId());
            log.debug("사용자 채팅 기록 총 개수 조회: 총 {}개 항목", totalRecords);

            // 페이지 수 계산 (나머지가 있으면 1 페이지 추가)
            return (int) Math.ceil((double) totalRecords / size);
        } catch (AccessDeniedException e) {
            log.error("채팅 기록 페이지 수 조회 권한 오류: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                    "채팅 기록 조회 권한이 없습니다. 로그인이 필요합니다.", e);
        } catch (DataAccessException e) {
            log.error("채팅 기록 페이지 수 조회 중 데이터베이스 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_HISTORY_QUERY_ERROR,
                    "채팅 기록 페이지 수를 조회하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("채팅 기록 페이지 수 조회 중 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_HISTORY_QUERY_ERROR,
                    "채팅 기록 페이지 수를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자의 최근 질문 목록 조회
     *
     * @param email 사용자 이메일
     * @param limit 최대 항목 수
     * @return 최근 질문 목록 DTO
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryDto> getRecentQuestions(String email, int limit) {
        try {
            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // 최근 질문 조회 (QueryDSL 사용)
            List<ChatQuestion> recentQuestions = chatQuestionRepository.findRecentQuestionsByUserId(user.getId(), limit);

            // DTO로 변환하여 반환
            return recentQuestions.stream()
                    .map(chat -> ChatHistoryDto.builder()
                            .id(chat.getId())
                            .question(chat.getQuestion())
                            .answer(chat.getAnswer())
                            .createdAt(chat.getCreatedAt())
                            .build())
                    .toList();
        } catch (AccessDeniedException e) {
            log.error("최근 질문 조회 권한 오류: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                    "질문 기록 조회 권한이 없습니다. 로그인이 필요합니다.", e);
        } catch (DataAccessException e) {
            log.error("최근 질문 조회 중 데이터베이스 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_HISTORY_QUERY_ERROR,
                    "최근 질문을 조회하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("최근 질문 조회 중 오류 발생: ", e);
            throw new ChatQueryException(ChatQuestionErrorCode.CHAT_HISTORY_QUERY_ERROR,
                    "최근 질문을 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}