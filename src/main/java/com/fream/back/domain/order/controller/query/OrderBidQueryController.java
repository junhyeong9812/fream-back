package com.fream.back.domain.order.controller.query;

import com.fream.back.domain.order.dto.OrderBidResponseDto;
import com.fream.back.domain.order.dto.OrderBidStatusCountDto;
import com.fream.back.domain.order.service.query.OrderBidQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order-bids")
@RequiredArgsConstructor
public class OrderBidQueryController {

    private final OrderBidQueryService orderBidQueryService;

    // OrderBid 목록 조회
    @GetMapping
    public Page<OrderBidResponseDto> getOrderBids(
            @RequestParam(value = "bidStatus", required = false) String bidStatus,
            @RequestParam(value = "orderStatus", required = false) String orderStatus,
            Pageable pageable
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        return orderBidQueryService.getOrderBids(email, bidStatus, orderStatus, pageable);
    }

    // OrderBid 상태 카운트 조회
    @GetMapping("/count")
    public OrderBidStatusCountDto getOrderBidStatusCounts() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        return orderBidQueryService.getOrderBidStatusCounts(email);
    }

    // 단일 OrderBid 조회
    @GetMapping("/{orderBidId}")
    public OrderBidResponseDto getOrderBidDetail(@PathVariable("orderBidId") Long orderBidId) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 현재 사용자 이메일 가져오기
        return orderBidQueryService.getOrderBidDetail(orderBidId, email)
                .orElseThrow(() -> new IllegalArgumentException("해당 OrderBid가 존재하지 않습니다. ID: " + orderBidId));
    }
}
