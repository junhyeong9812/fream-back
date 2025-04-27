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

/**
 * GPT 사용량 관리 컨트롤러
 * 관리자만 접근 가능한 GPT 사용량 통계 및 로그 조회 API를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/gpt")
@Slf4j
public class GPTUsageController {

    private final GPTUsageService gptUsageService;
    private final UserQueryService userQueryService;

    /**
     * GPT 사용량 통계 조회
     * 특정 기간 동안의 GPT 사용량 통계를 조회합니다.
     *
     * @param startDate 시작 날짜 (ISO 형식: yyyy-MM-dd)
     * @param endDate 종료 날짜 (ISO 형식: yyyy-MM-dd)
     * @return GPT 사용량 통계 DTO
     */
    @GetMapping("/stats")
    public ResponseEntity<ResponseDto<GPTUsageStatsDto>> getUsageStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            // 이메일 추출 후 Admin 권한 체크 (추가 보안)
            String email = SecurityUtils.extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email);

            validateDateRange(startDate, endDate);

            GPTUsageStatsDto stats = gptUsageService.getUsageStatistics(startDate, endDate);
            log.info("GPT 사용량 통계 조회 성공: 기간={} ~ {}, 총 토큰={}",
                    startDate, endDate, stats.getTotalTokensUsed());

            return ResponseEntity.ok(ResponseDto.success(stats));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 GPT 사용량 통계 조회 시도: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "GPT 사용량 통계 조회는 관리자만 가능합니다.", e);
        }
    }

    /**
     * GPT 사용량 로그 페이징 조회
     *
     * @param pageable 페이징 정보 (기본값: 크기 20, 생성일 기준 정렬)
     * @return GPT 사용량 로그 DTO 페이지
     */
    @GetMapping("/logs")
    public ResponseEntity<ResponseDto<Page<GPTUsageLogDto>>> getUsageLogs(
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable
    ) {
        try {
            // 이메일 추출 후 Admin 권한 체크 (추가 보안)
            String email = SecurityUtils.extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email);

            Page<GPTUsageLogDto> logs = gptUsageService.getUsageLogs(pageable);
            log.info("GPT 사용량 로그 조회 성공: 페이지={}, 크기={}",
                    pageable.getPageNumber(), pageable.getPageSize());

            return ResponseEntity.ok(ResponseDto.success(logs));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 GPT 사용량 로그 조회 시도: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "GPT 사용량 로그 조회는 관리자만 가능합니다.", e);
        }
    }

    /**
     * 총 누적 토큰 사용량 조회
     * 서비스 시작부터 현재까지의 총 토큰 사용량을 조회합니다.
     *
     * @return 총 토큰 사용량
     */
    @GetMapping("/total-tokens")
    public ResponseEntity<ResponseDto<Integer>> getTotalTokensUsed() {
        try {
            // 이메일 추출 후 Admin 권한 체크 (추가 보안)
            String email = SecurityUtils.extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email);

            int totalTokens = gptUsageService.getTotalTokensUsed();
            log.info("총 누적 토큰 사용량 조회 성공: 총 토큰={}", totalTokens);

            return ResponseEntity.ok(ResponseDto.success(totalTokens));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 총 토큰 사용량 조회 시도: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "GPT 총 토큰 사용량 조회는 관리자만 가능합니다.", e);
        }
    }

    /**
     * 날짜 범위 유효성 검사
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
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
}