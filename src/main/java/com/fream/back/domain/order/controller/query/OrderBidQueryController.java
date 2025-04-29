package com.fream.back.domain.order.controller.query;

import com.fream.back.domain.order.dto.OrderBidResponseDto;
import com.fream.back.domain.order.dto.OrderBidStatusCountDto;
import com.fream.back.domain.order.exception.InvalidOrderBidDataException;
import com.fream.back.domain.order.exception.OrderBidAccessDeniedException;
import com.fream.back.domain.order.exception.OrderBidNotFoundException;
import com.fream.back.domain.order.service.query.OrderBidQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 입찰 조회 컨트롤러
 */
@RestController
@RequestMapping("/order-bids")
@RequiredArgsConstructor
@Slf4j
public class OrderBidQueryController {

    private final OrderBidQueryService orderBidQueryService;

    /**
     * 주문 입찰 목록을 조회합니다.
     *
     * @param bidStatus 입찰 상태 필터
     * @param orderStatus 주문 상태 필터
     * @param pageable 페이징 정보
     * @return 주문 입찰 목록 (페이징)
     * @throws OrderBidAccessDeniedException 주문 입찰에 대한 접근 권한이 없는 경우
     * @throws InvalidOrderBidDataException 주문 입찰 정보가 유효하지 않은 경우
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<OrderBidResponseDto>>> getOrderBids(
            @RequestParam(value = "bidStatus", required = false) String bidStatus,
            @RequestParam(value = "orderStatus", required = false) String orderStatus,
            Pageable pageable
    ) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrderBid("주문 입찰 목록 조회");

        log.info("사용자 [{}]의 주문 입찰 목록을 조회합니다. 필터 - 입찰 상태: {}, 주문 상태: {}",
                email, bidStatus, orderStatus);

        Page<OrderBidResponseDto> result = orderBidQueryService.getOrderBids(email, bidStatus, orderStatus, pageable);

        return ResponseEntity.ok(ResponseDto.success(result, "주문 입찰 목록 조회 성공"));
    }

    /**
     * 주문 입찰 상태별 개수를 조회합니다.
     *
     * @return 주문 입찰 상태별 개수
     * @throws OrderBidAccessDeniedException 주문 입찰에 대한 접근 권한이 없는 경우
     */
    @GetMapping("/count")
    public ResponseEntity<ResponseDto<OrderBidStatusCountDto>> getOrderBidStatusCounts() {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrderBid("주문 입찰 상태별 개수 조회");

        log.info("사용자 [{}]의 주문 입찰 상태별 개수를 조회합니다.", email);

        OrderBidStatusCountDto result = orderBidQueryService.getOrderBidStatusCounts(email);

        return ResponseEntity.ok(ResponseDto.success(result, "주문 입찰 상태별 개수 조회 성공"));
    }

    /**
     * 주문 입찰 상세 정보를 조회합니다.
     *
     * @param orderBidId 주문 입찰 ID
     * @return 주문 입찰 상세 정보
     * @throws OrderBidAccessDeniedException 주문 입찰에 대한 접근 권한이 없는 경우
     * @throws OrderBidNotFoundException 주문 입찰을 찾을 수 없는 경우
     */
    @GetMapping("/{orderBidId}")
    public ResponseEntity<ResponseDto<OrderBidResponseDto>> getOrderBidDetail(@PathVariable("orderBidId") Long orderBidId) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrderBid("주문 입찰 상세 정보 조회");

        log.info("사용자 [{}]가 주문 입찰(ID: {})의 상세 정보를 조회합니다.", email, orderBidId);

        OrderBidResponseDto result = orderBidQueryService.getOrderBidDetail(orderBidId, email)
                .orElseThrow(() -> new OrderBidNotFoundException("해당 주문 입찰이 존재하지 않습니다(ID: " + orderBidId + ")"));

        return ResponseEntity.ok(ResponseDto.success(result, "주문 입찰 상세 정보 조회 성공"));
    }
}