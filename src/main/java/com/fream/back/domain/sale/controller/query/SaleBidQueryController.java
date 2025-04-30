package com.fream.back.domain.sale.controller.query;

import com.fream.back.domain.sale.dto.SaleBidResponseDto;
import com.fream.back.domain.sale.dto.SaleBidStatusCountDto;
import com.fream.back.domain.sale.service.query.SaleBidQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sale-bids")
@RequiredArgsConstructor
@Slf4j
public class SaleBidQueryController {

    private final SaleBidQueryService saleBidQueryService;

    // SaleBid 목록 조회
    @GetMapping
    public ResponseEntity<ResponseDto<Page<SaleBidResponseDto>>> getSaleBids(
            @RequestParam(value = "saleBidStatus", required = false) String saleBidStatus,
            @RequestParam(value = "saleStatus", required = false) String saleStatus,
            Pageable pageable
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.info("사용자 [{}]의 판매 입찰 목록을 조회합니다. 필터 - 입찰 상태: {}, 판매 상태: {}",
                email, saleBidStatus, saleStatus);

        Page<SaleBidResponseDto> result = saleBidQueryService.getSaleBids(email, saleBidStatus, saleStatus, pageable);
        return ResponseEntity.ok(ResponseDto.success(result, "판매 입찰 목록 조회 성공"));
    }

    // SaleBid 상태 카운트 조회
    @GetMapping("/count")
    public ResponseEntity<ResponseDto<SaleBidStatusCountDto>> getSaleBidStatusCounts() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.info("사용자 [{}]의 판매 입찰 상태별 개수를 조회합니다.", email);

        SaleBidStatusCountDto result = saleBidQueryService.getSaleBidStatusCounts(email);
        return ResponseEntity.ok(ResponseDto.success(result, "판매 입찰 상태별 개수 조회 성공"));
    }

    // 단일 SaleBid 조회
    @GetMapping("/{saleBidId}")
    public ResponseEntity<ResponseDto<SaleBidResponseDto>> getSaleBidDetail(@PathVariable("saleBidId") Long saleBidId) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.info("사용자 [{}]가 판매 입찰(ID: {})의 상세 정보를 조회합니다.", email, saleBidId);

        SaleBidResponseDto saleBidResponse = saleBidQueryService.getSaleBidDetail(saleBidId, email);

        // null 값 예외 처리
        if (saleBidResponse == null) {
            throw new IllegalArgumentException("해당 SaleBid 데이터를 찾을 수 없습니다. ID: " + saleBidId);
        }

        return ResponseEntity.ok(ResponseDto.success(saleBidResponse, "판매 입찰 상세 정보 조회 성공"));
    }
}