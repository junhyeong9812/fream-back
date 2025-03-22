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
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatQuestionRepository chatQuestionRepository;
    private final FAQQueryService faqQueryService;
    private final GPTService gptService;
    private final UserQueryService userQueryService;
    private final GPTUsageService gptUsageService;

    // 질문 처리 및 응답 반환 (로그인 사용자만 가능)
    @Transactional
    public QuestionResponseDto processQuestion(String email, QuestionRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidQuestionException("질문 데이터가 필요합니다.");
        }

        String question = requestDto.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            throw new InvalidQuestionException("질문 내용이 비어있습니다.");
        }

        try {
            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // FAQ 데이터 가져오기
            List<FAQResponseDto> faqList = faqQueryService.getAllFAQs();

            // GPT API로 응답 받기
            GPTResponseDto gptResponse = gptService.getGPTResponseWithUsage(question, faqList);
            String answer = gptResponse.getAnswer();

            // 질문 저장
            ChatQuestion chatQuestion = ChatQuestion.builder()
                    .question(question)
                    .answer(answer)
                    .isAnswered(true)
                    .user(user)
                    .build();

            try {
                chatQuestionRepository.save(chatQuestion);
            } catch (DataAccessException e) {
                log.error("채팅 질문 저장 중 오류 발생: ", e);
                throw new ChatQueryException(ChatQuestionErrorCode.CHAT_QUESTION_SAVE_ERROR,
                        "채팅 질문을 저장하는 중 데이터베이스 오류가 발생했습니다.", e);
            }

            // GPT 사용량 로그 기록
            gptUsageService.logGPTUsage(gptResponse, user, "FAQ_CHAT", chatQuestion);

            // 응답 반환
            return QuestionResponseDto.builder()
                    .question(question)
                    .answer(answer)
                    .createdAt(chatQuestion.getCreatedAt())
                    .build();
        } catch (AccessDeniedException e) {
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

    // 채팅 기록 조회 (로그인 사용자만 가능)
    @Transactional(readOnly = true)
    public Page<ChatHistoryDto> getChatHistory(String email, Pageable pageable) {
        try {
            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // 사용자의 채팅 기록 조회
            Page<ChatQuestion> chatQuestions = chatQuestionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

            // DTO로 변환하여 반환
            return chatQuestions.map(chat -> ChatHistoryDto.builder()
                    .id(chat.getId())
                    .question(chat.getQuestion())
                    .answer(chat.getAnswer())
                    .createdAt(chat.getCreatedAt())
                    .build());
        } catch (AccessDeniedException e) {
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
     */
    @Transactional(readOnly = true)
    public int getChatHistoryPageCount(String email, int size) {
        try {
            // 사용자 정보 조회
            User user = userQueryService.findByEmail(email);

            // 전체 레코드 수 조회
            long totalRecords = chatQuestionRepository.countByUserId(user.getId());

            // 페이지 수 계산 (나머지가 있으면 1 페이지 추가)
            return (int) Math.ceil((double) totalRecords / size);
        } catch (AccessDeniedException e) {
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
}