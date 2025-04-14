package com.fream.back.domain.user.controller.admin;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.service.admin.AdminPointService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users/points")
@RequiredArgsConstructor
public class AdminPointController {

    private final AdminPointService adminPointService;
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
     * 특정 사용자의 모든 포인트 내역 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PointDto.PointResponse>> getUserPointHistory(@PathVariable Long userId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        List<PointDto.PointResponse> response = adminPointService.getUserPointHistory(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 사용 가능한 포인트만 조회
     */
    @GetMapping("/user/{userId}/available")
    public ResponseEntity<List<PointDto.PointResponse>> getUserAvailablePoints(@PathVariable Long userId) {

        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        List<PointDto.PointResponse> response = adminPointService.getUserAvailablePoints(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 포인트 종합 정보 조회
     */
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<PointDto.PointSummaryResponse> getUserPointSummary(@PathVariable Long userId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인


        PointDto.PointSummaryResponse response = adminPointService.getUserPointSummary(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 포인트 상세 조회
     */
    @GetMapping("/{pointId}")
    public ResponseEntity<PointDto.PointResponse> getPointDetail(@PathVariable Long pointId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);  // 관리자 권한 확인

        PointDto.PointResponse response = adminPointService.getPointDetail(pointId);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 지급 (어드민)
     */
    @PostMapping("/user/{userId}/add")
    public ResponseEntity<PointDto.PointResponse> addPointByAdmin(
            @PathVariable Long userId,
            @RequestBody PointDto.AdminPointRequest request) {

        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        PointDto.PointResponse response = adminPointService.addPointByAdmin(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 차감 (어드민)
     */
    @PostMapping("/user/{userId}/deduct")
    public ResponseEntity<PointDto.UsePointResponse> deductPointByAdmin(
            @PathVariable Long userId,
            @RequestBody PointDto.AdminPointRequest request) {

        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        PointDto.UsePointResponse response = adminPointService.deductPointByAdmin(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<PointDto.PointStatisticsResponse> getPointStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        PointDto.PointStatisticsResponse response = adminPointService.getPointStatistics(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}