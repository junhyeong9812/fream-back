package com.fream.back.domain.chatQuestion.service;

import com.fream.back.domain.chatQuestion.dto.chat.ChatHistoryDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.domain.chatQuestion.repository.ChatQuestionRepository;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.service.query.FAQQueryService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        String question = requestDto.getQuestion();

        // 사용자 정보 조회
        User user = userQueryService.findByEmail(email);

        // FAQ 데이터 가져오기
        List<FAQResponseDto> faqList = faqQueryService.getAllFAQs();

        // GPT API로 응답 받기 - GPTResponseDto 전체를 반환하도록 GPTService 수정 필요
        GPTResponseDto gptResponse = gptService.getGPTResponseWithUsage(question, faqList);
        String answer = gptResponse.getAnswer();

        // 질문 저장
        ChatQuestion chatQuestion = ChatQuestion.builder()
                .question(question)
                .answer(answer)
                .isAnswered(true)
                .user(user)
                .build();

        chatQuestionRepository.save(chatQuestion);

        // GPT 사용량 로그 기록
        gptUsageService.logGPTUsage(gptResponse, user, "FAQ_CHAT", chatQuestion);

        // 응답 반환
        return QuestionResponseDto.builder()
                .question(question)
                .answer(answer)
                .createdAt(chatQuestion.getCreatedDate())
                .build();
    }

    // 채팅 기록 조회 (로그인 사용자만 가능)
    @Transactional(readOnly = true)
    public Page<ChatHistoryDto> getChatHistory(String email, Pageable pageable) {
        // 사용자 정보 조회
        User user = userQueryService.findByEmail(email);

        // 사용자의 채팅 기록 조회
        Page<ChatQuestion> chatQuestions = chatQuestionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        // DTO로 변환하여 반환
        return chatQuestions.map(chat -> ChatHistoryDto.builder()
                .id(chat.getId())
                .question(chat.getQuestion())
                .answer(chat.getAnswer())
                .createdAt(chat.getCreatedDate())
                .build());
    }
}