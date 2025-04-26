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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅 질문 관리 컨트롤러
 * 사용자의 질문 처리 및 채팅 기록 조회 API를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int DEFAULT_PAGE_SIZE = 2;

    /**
     * 질문 전송 및 응답 받기 (로그인 사용자만 가능)
     *
     * @param requestDto 질문 요청 DTO
     * @return 질문 응답 DTO
     */
    @PostMapping("/question")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDto<QuestionResponseDto>> askQuestion(
            @RequestBody @Validated QuestionRequestDto requestDto
    ) {
        // 요청 데이터 검증
        validateQuestionRequest(requestDto);

        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.info("사용자 질문 요청: email={}, 질문 길이={}", email, requestDto.getQuestion().length());

        QuestionResponseDto responseDto = chatService.processQuestion(email, requestDto);
        return ResponseEntity.ok(ResponseDto.success(responseDto));
    }

    /**
     * 채팅 기록 조회 (로그인 사용자만 가능)
     * 기본적으로 최신순(DESC) 정렬
     *
     * @param pageable 페이징 정보 (기본값: 크기 2)
     * @return 채팅 기록 DTO 페이지
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDto<Page<ChatHistoryDto>>> getChatHistory(
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.info("채팅 기록 조회 요청: email={}, 페이지={}, 크기={}",
                email, pageable.getPageNumber(), pageable.getPageSize());

        Page<ChatHistoryDto> history = chatService.getChatHistory(email, pageable);
        return ResponseEntity.ok(ResponseDto.success(history));
    }

    /**
     * 채팅 기록 페이지 수 조회 (로그인 사용자만 가능)
     *
     * @param size 페이지 크기 (기본값: 2)
     * @return 총 페이지 수
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
        log.info("채팅 기록 페이지 수 조회 요청: email={}, 페이지 크기={}", email, size);

        int totalPages = chatService.getChatHistoryPageCount(email, size);
        return ResponseEntity.ok(ResponseDto.success(totalPages));
    }

    /**
     * 최근 질문 목록 조회 (로그인 사용자만 가능)
     *
     * @param limit 최대 항목 수 (기본값: 5)
     * @return 최근 질문 목록 DTO
     */
    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDto<List<ChatHistoryDto>>> getRecentQuestions(
            @RequestParam(defaultValue = "5") int limit
    ) {
        // 유효한 항목 수 검증
        if (limit <= 0 || limit > 20) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.INVALID_QUESTION_DATA,
                    "항목 수는 1에서 20 사이여야 합니다.");
        }

        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.info("최근 질문 목록 조회 요청: email={}, 항목 수={}", email, limit);

        List<ChatHistoryDto> recentQuestions = chatService.getRecentQuestions(email, limit);
        return ResponseEntity.ok(ResponseDto.success(recentQuestions));
    }

    /**
     * 질문 요청 유효성 검사
     *
     * @param requestDto 질문 요청 DTO
     */
    private void validateQuestionRequest(QuestionRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.INVALID_QUESTION_DATA,
                    "질문 데이터가 필요합니다.");
        }

        String question = requestDto.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.INVALID_QUESTION_DATA,
                    "질문 내용이 비어있습니다.");
        }

        // 질문 길이 제한
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new InvalidQuestionException(ChatQuestionErrorCode.QUESTION_LENGTH_EXCEEDED,
                    "질문 길이가 제한을 초과했습니다. (최대 " + MAX_QUESTION_LENGTH + "자)");
        }
    }
}