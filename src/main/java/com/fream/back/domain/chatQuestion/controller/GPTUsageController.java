package com.fream.back.domain.chatQuestion.controller;

import com.fream.back.domain.chatQuestion.dto.log.GPTUsageLogDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTUsageStatsDto;
import com.fream.back.domain.chatQuestion.exception.ChatPermissionException;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.chatQuestion.exception.GPTUsageException;
import com.fream.back.domain.chatQuestion.service.GPTUsageService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/gpt")
@Slf4j
public class GPTUsageController {

    private final GPTUsageService gptUsageService;
    private final UserQueryService userQueryService;

    /**
     * GPT 사용량 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<ResponseDto<GPTUsageStatsDto>> getUsageStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            // 이메일 추출 후 Admin 권한 체크
            String email = SecurityUtils.extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email);

            if (startDate == null || endDate == null) {
                throw new GPTUsageException(ChatQuestionErrorCode.INVALID_DATE_RANGE,
                        "시작 날짜와 종료 날짜는 필수입니다.");
            }

            if (startDate.isAfter(endDate)) {
                throw new GPTUsageException(ChatQuestionErrorCode.INVALID_DATE_RANGE,
                        "시작 날짜는 종료 날짜보다 이전이어야 합니다.");
            }

            GPTUsageStatsDto stats = gptUsageService.getUsageStatistics(startDate, endDate);
            return ResponseEntity.ok(ResponseDto.success(stats));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 GPT 사용량 통계 조회 시도");
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "GPT 사용량 통계 조회는 관리자만 가능합니다.", e);
        }
    }

    /**
     * GPT 사용량 로그 페이징 조회
     */
    @GetMapping("/logs")
    public ResponseEntity<ResponseDto<Page<GPTUsageLogDto>>> getUsageLogs(
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable
    ) {
        try {
            // 이메일 추출 후 Admin 권한 체크
            String email = SecurityUtils.extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email);

            Page<GPTUsageLogDto> logs = gptUsageService.getUsageLogs(pageable);
            return ResponseEntity.ok(ResponseDto.success(logs));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 GPT 사용량 로그 조회 시도");
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "GPT 사용량 로그 조회는 관리자만 가능합니다.", e);
        }
    }

    /**
     * 총 누적 토큰 사용량 조회
     */
    @GetMapping("/total-tokens")
    public ResponseEntity<ResponseDto<Integer>> getTotalTokensUsed() {
        try {
            // 이메일 추출 후 Admin 권한 체크
            String email = SecurityUtils.extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email);

            int totalTokens = gptUsageService.getTotalTokensUsed();
            return ResponseEntity.ok(ResponseDto.success(totalTokens));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 총 토큰 사용량 조회 시도");
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "GPT 총 토큰 사용량 조회는 관리자만 가능합니다.", e);
        }
    }
}