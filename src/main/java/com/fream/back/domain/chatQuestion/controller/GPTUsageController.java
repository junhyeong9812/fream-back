package com.fream.back.domain.chatQuestion.controller;

import com.fream.back.domain.chatQuestion.dto.log.GPTUsageLogDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTUsageStatsDto;
import com.fream.back.domain.chatQuestion.service.GPTUsageService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/gpt")
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
        // 이메일 추출 후 Admin 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        GPTUsageStatsDto stats = gptUsageService.getUsageStatistics(startDate, endDate);
        return ResponseEntity.ok(ResponseDto.success(stats));
    }

    /**
     * GPT 사용량 로그 페이징 조회
     */
    @GetMapping("/logs")
    public ResponseEntity<ResponseDto<Page<GPTUsageLogDto>>> getUsageLogs(
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable
    ) {
        // 이메일 추출 후 Admin 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        Page<GPTUsageLogDto> logs = gptUsageService.getUsageLogs(pageable);
        return ResponseEntity.ok(ResponseDto.success(logs));
    }

    /**
     * 총 누적 토큰 사용량 조회
     */
    @GetMapping("/total-tokens")
    public ResponseEntity<ResponseDto<Integer>> getTotalTokensUsed() {
        // 이메일 추출 후 Admin 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        int totalTokens = gptUsageService.getTotalTokensUsed();
        return ResponseEntity.ok(ResponseDto.success(totalTokens));
    }
}