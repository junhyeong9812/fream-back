package com.fream.back.domain.user.controller.admin;

import com.fream.back.domain.user.dto.SanctionDto.*;
import com.fream.back.domain.user.entity.SanctionStatus;
import com.fream.back.domain.user.entity.SanctionType;
import com.fream.back.domain.user.service.admin.AdminSanctionService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/users/sanctions")
@RequiredArgsConstructor
public class AdminSanctionController {

    private final AdminSanctionService adminSanctionService;
    private final UserQueryService userQueryService; // 권한 확인 서비스

    // SecurityContext에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }
    /**
     * 제재 검색 (페이징)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SanctionResponseDto>> searchSanctions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdDateEnd,
            @RequestParam(required = false, defaultValue = "createdDate") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String adminEmail = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(adminEmail); // 관리자 권한 확인
        SanctionSearchRequestDto searchDto = SanctionSearchRequestDto.builder()
                .userId(userId)
                .email(email)
                .status(status != null ? SanctionStatus.valueOf(status) : null)
                .type(type != null ? SanctionType.valueOf(type) : null)
                .startDateStart(startDateStart)
                .startDateEnd(startDateEnd)
                .endDateStart(endDateStart)
                .endDateEnd(endDateEnd)
                .createdDateStart(createdDateStart)
                .createdDateEnd(createdDateEnd)
                .sortField(sort)
                .sortDirection(direction)
                .build();

        Page<SanctionResponseDto> result = adminSanctionService.searchSanctions(searchDto, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 제재 상세 조회
     */
    @GetMapping("/{sanctionId}")
    public ResponseEntity<SanctionResponseDto> getSanctionById(@PathVariable Long sanctionId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        SanctionResponseDto sanction = adminSanctionService.getSanctionById(sanctionId);
        return ResponseEntity.ok(sanction);
    }

    /**
     * 제재 생성
     */
    @PostMapping
    public ResponseEntity<SanctionResponseDto> createSanction(
            @RequestBody SanctionCreateRequestDto requestDto,
            @RequestHeader("X-Admin-Email") String adminEmail) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        SanctionResponseDto createdSanction = adminSanctionService.createSanction(requestDto, adminEmail);
        return ResponseEntity.ok(createdSanction);
    }

    /**
     * 제재 수정
     */
    @PutMapping("/{sanctionId}")
    public ResponseEntity<SanctionResponseDto> updateSanction(
            @PathVariable Long sanctionId,
            @RequestBody SanctionUpdateRequestDto requestDto) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        SanctionResponseDto updatedSanction = adminSanctionService.updateSanction(sanctionId, requestDto);
        return ResponseEntity.ok(updatedSanction);
    }

    /**
     * 제재 승인/거부
     */
    @PatchMapping("/{sanctionId}/review")
    public ResponseEntity<SanctionResponseDto> reviewSanction(
            @PathVariable Long sanctionId,
            @RequestBody SanctionReviewRequestDto requestDto,
            @RequestHeader("X-Admin-Email") String adminEmail) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        SanctionResponseDto reviewedSanction = adminSanctionService.reviewSanction(sanctionId, requestDto, adminEmail);
        return ResponseEntity.ok(reviewedSanction);
    }

    /**
     * 제재 취소
     */
    @PatchMapping("/{sanctionId}/cancel")
    public ResponseEntity<SanctionResponseDto> cancelSanction(@PathVariable Long sanctionId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        SanctionResponseDto canceledSanction = adminSanctionService.cancelSanction(sanctionId);
        return ResponseEntity.ok(canceledSanction);
    }

    /**
     * 특정 사용자의 제재 내역 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SanctionResponseDto>> getUserSanctions(@PathVariable Long userId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        List<SanctionResponseDto> sanctions = adminSanctionService.getUserSanctions(userId);
        return ResponseEntity.ok(sanctions);
    }

    /**
     * 제재 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<SanctionStatisticsDto> getSanctionStatistics() {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        SanctionStatisticsDto statistics = adminSanctionService.getSanctionStatistics();
        return ResponseEntity.ok(statistics);
    }
}