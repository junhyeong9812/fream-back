package com.fream.back.domain.user.controller.point;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.service.query.PointQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/points/queries")
public class PointQueryController {

    private final PointQueryService pointQueryService;

    /**
     * 포인트 내역 전체 조회
     */
    @GetMapping
    public ResponseEntity<List<PointDto.PointResponse>> getAllPointHistory() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        List<PointDto.PointResponse> response = pointQueryService.getAllPointHistory(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용 가능한 포인트만 조회
     */
    @GetMapping("/available")
    public ResponseEntity<List<PointDto.PointResponse>> getAvailablePoints() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        List<PointDto.PointResponse> response = pointQueryService.getAvailablePoints(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 종합 정보 조회
     */
    @GetMapping("/summary")
    public ResponseEntity<PointDto.PointSummaryResponse> getPointSummary() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        PointDto.PointSummaryResponse response = pointQueryService.getPointSummary(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 상세 조회
     */
    @GetMapping("/{pointId}")
    public ResponseEntity<PointDto.PointResponse> getPointDetail(@PathVariable Long pointId) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        PointDto.PointResponse response = pointQueryService.getPointDetail(email, pointId);
        return ResponseEntity.ok(response);
    }
}