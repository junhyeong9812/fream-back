package com.fream.back.domain.chatQuestion.controller;

import com.fream.back.domain.chatQuestion.dto.chat.ChatHistoryDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.chatQuestion.exception.InvalidQuestionException;
import com.fream.back.domain.chatQuestion.service.ChatService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * 질문 전송 및 응답 받기 (로그인 사용자만 가능)
     */
    @PostMapping("/question")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDto<QuestionResponseDto>> askQuestion(
            @RequestBody QuestionRequestDto requestDto
    ) {
        // 요청 데이터 검증
        if (requestDto == null) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.INVALID_QUESTION_DATA,
                    "질문 데이터가 필요합니다.");
        }

        String question = requestDto.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.INVALID_QUESTION_DATA,
                    "질문 내용이 비어있습니다.");
        }

        // 질문 길이 제한 (예: 2000자)
        if (question.length() > 2000) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.QUESTION_LENGTH_EXCEEDED,
                    "질문 길이가 제한을 초과했습니다. (최대 2000자)");
        }

        String email = SecurityUtils.extractEmailFromSecurityContext();
        QuestionResponseDto responseDto = chatService.processQuestion(email, requestDto);
        return ResponseEntity.ok(ResponseDto.success(responseDto));
    }

    /**
     * 채팅 기록 조회 (로그인 사용자만 가능)
     * 기본적으로 최신순(DESC) 정렬, 명시적인 정렬 방향 지정 가능
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDto<Page<ChatHistoryDto>>> getChatHistory(
            @PageableDefault(size = 2) Pageable pageable
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        Page<ChatHistoryDto> history = chatService.getChatHistory(email, pageable);
        return ResponseEntity.ok(ResponseDto.success(history));
    }

    /**
     * 채팅 기록 페이지 수 조회 (로그인 사용자만 가능)
     */
    @GetMapping("/history/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDto<Integer>> getChatHistoryPageCount(
            @RequestParam(defaultValue = "2") int size
    ) {
        // 유효한 페이지 크기 검증
        if (size <= 0) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.INVALID_QUESTION_DATA,
                    "페이지 크기는 1 이상이어야 합니다.");
        }

        String email = SecurityUtils.extractEmailFromSecurityContext();
        int totalPages = chatService.getChatHistoryPageCount(email, size);
        return ResponseEntity.ok(ResponseDto.success(totalPages));
    }
}