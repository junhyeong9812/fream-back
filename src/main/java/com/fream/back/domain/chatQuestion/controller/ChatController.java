package com.fream.back.domain.chat.controller;

import com.fream.back.domain.chatQuestion.dto.chat.ChatHistoryDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.service.ChatService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * 질문 전송 및 응답 받기 (로그인 사용자만 가능)
     */
    @PostMapping("/question")
    @PreAuthorize("isAuthenticated()")  // 인증된 사용자만 접근 가능
    public ResponseEntity<ResponseDto<QuestionResponseDto>> askQuestion(
            @RequestBody QuestionRequestDto requestDto
    ) {
        // SecurityUtils로 이메일 추출
        String email = SecurityUtils.extractEmailFromSecurityContext();
        QuestionResponseDto responseDto = chatService.processQuestion(email, requestDto);
        return ResponseEntity.ok(ResponseDto.success(responseDto));
    }

    /**
     * 채팅 기록 조회 (로그인 사용자만 가능)
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")  // 인증된 사용자만 접근 가능
    public ResponseEntity<ResponseDto<Page<ChatHistoryDto>>> getChatHistory(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        // SecurityUtils로 이메일 추출
        String email = SecurityUtils.extractEmailFromSecurityContext();
        Page<ChatHistoryDto> history = chatService.getChatHistory(email, pageable);
        return ResponseEntity.ok(ResponseDto.success(history));
    }
}