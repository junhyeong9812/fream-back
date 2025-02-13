package com.fream.back.domain.sale.controller.query;

import com.fream.back.domain.sale.dto.SaleBidResponseDto;
import com.fream.back.domain.sale.dto.SaleBidStatusCountDto;
import com.fream.back.domain.sale.service.query.SaleBidQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sale-bids") // 공통 경로를 클래스 레벨로 이동
@RequiredArgsConstructor
public class SaleBidQueryController {

    private final SaleBidQueryService saleBidQueryService;

    // SaleBid 목록 조회
    @GetMapping
    public Page<SaleBidResponseDto> getSaleBids(
            @RequestParam(value = "saleBidStatus", required = false) String saleBidStatus,
            @RequestParam(value = "saleStatus", required = false) String saleStatus,
            Pageable pageable
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        return saleBidQueryService.getSaleBids(email, saleBidStatus, saleStatus, pageable);
    }

    // SaleBid 상태 카운트 조회
    @GetMapping("/count")
    public SaleBidStatusCountDto getSaleBidStatusCounts() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        return saleBidQueryService.getSaleBidStatusCounts(email);
    }
    // 단일 SaleBid 조회
    @GetMapping("/{saleBidId}")
    public SaleBidResponseDto getSaleBidDetail(@PathVariable("saleBidId") Long saleBidId) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 현재 사용자 이메일 가져오기
        SaleBidResponseDto saleBidResponse = saleBidQueryService.getSaleBidDetail(saleBidId, email);

        // null 값 예외 처리
        if (saleBidResponse == null) {
            throw new IllegalArgumentException("해당 SaleBid 데이터를 찾을 수 없습니다. ID: " + saleBidId);
        }

        return saleBidResponse;    }
}
