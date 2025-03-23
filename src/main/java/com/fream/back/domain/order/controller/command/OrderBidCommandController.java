package com.fream.back.domain.order.controller.command;

import com.fream.back.domain.order.dto.InstantOrderRequestDto;
import com.fream.back.domain.order.dto.OrderBidRequestDto;
import com.fream.back.domain.order.exception.*;
import com.fream.back.domain.order.service.command.OrderBidCommandService;
import com.fream.back.domain.payment.dto.PaymentRequestDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order-bids")
@RequiredArgsConstructor
@Slf4j
public class OrderBidCommandController {

    private final OrderBidCommandService orderBidCommandService;

    /**
     * 주문 입찰을 생성합니다.
     *
     * @param requestDto 주문 입찰 요청 정보
     * @return 생성된 주문 입찰 ID
     * @throws OrderBidAccessDeniedException 주문 입찰에 대한 접근 권한이 없는 경우
     * @throws InvalidOrderBidDataException 주문 입찰 정보가 유효하지 않은 경우
     * @throws OrderBidCreationFailedException 주문 입찰 생성 실패 시
     */
    @PostMapping
    public ResponseEntity<Long> createOrderBid(@RequestBody OrderBidRequestDto requestDto) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractEmailFromSecurityContext();
        if (email == null || email.trim().isEmpty()) {
            log.warn("주문 입찰 생성 시 사용자 이메일을 가져올 수 없습니다.");
            throw new OrderBidAccessDeniedException("사용자 정보를 가져올 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        // 요청 DTO 검증
        if (requestDto == null) {
            throw new InvalidOrderBidDataException("주문 입찰 정보가 없습니다.");
        }
        if (requestDto.getProductSizeId() == null) {
            throw new InvalidOrderBidDataException("상품 사이즈 정보가 없습니다.");
        }
        if (requestDto.getBidPrice() <= 0) {
            throw new InvalidBidPriceException("입찰 가격은 0보다 커야 합니다. 현재 가격: " + requestDto.getBidPrice());
        }

        log.info("사용자 [{}]가 상품 사이즈(ID: {})에 대한 주문 입찰을 생성합니다. 입찰 가격: {}",
                email, requestDto.getProductSizeId(), requestDto.getBidPrice());

        // 서비스 호출
        Long orderBidId = orderBidCommandService.createOrderBid(
                email,
                requestDto.getProductSizeId(),
                requestDto.getBidPrice()
        ).getId();

        return ResponseEntity.ok(orderBidId);
    }

    /**
     * 주문 입찰을 삭제합니다.
     *
     * @param orderBidId 주문 입찰 ID
     * @return 응답 엔티티
     * @throws OrderBidNotFoundException 주문 입찰을 찾을 수 없는 경우
     * @throws OrderBidAlreadyMatchedException 이미 매칭된 주문 입찰인 경우
     * @throws OrderBidDeletionFailedException 주문 입찰 삭제 실패 시
     */
    @DeleteMapping("/{orderBidId}")
    public ResponseEntity<Void> deleteOrderBid(@PathVariable("orderBidId") Long orderBidId) {
        log.info("주문 입찰(ID: {}) 삭제 요청이 들어왔습니다.", orderBidId);

        orderBidCommandService.deleteOrderBid(orderBidId);

        return ResponseEntity.ok().build();
    }

    /**
     * 즉시 구매 입찰을 생성합니다.
     *
     * @param requestDto 즉시 구매 요청 정보
     * @return 생성된 주문 ID
     * @throws OrderBidAccessDeniedException 주문 입찰에 대한 접근 권한이 없는 경우
     * @throws InvalidOrderBidDataException 주문 입찰 정보가 유효하지 않은 경우
     * @throws SaleBidNotFoundException 판매 입찰을 찾을 수 없는 경우
     * @throws OrderBidCreationFailedException 주문 입찰 생성 실패 시
     */
    @PostMapping("/instant")
    public ResponseEntity<Long> createInstantOrderBid(@RequestBody InstantOrderRequestDto requestDto) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractEmailFromSecurityContext();
        if (email == null || email.trim().isEmpty()) {
            log.warn("즉시 구매 입찰 생성 시 사용자 이메일을 가져올 수 없습니다.");
            throw new OrderBidAccessDeniedException("사용자 정보를 가져올 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        // 요청 DTO 검증
        if (requestDto == null) {
            throw new InvalidOrderBidDataException("즉시 구매 입찰 정보가 없습니다.");
        }
        if (requestDto.getSaleBidId() == null) {
            throw new InvalidOrderBidDataException("판매 입찰 정보가 없습니다.");
        }
        if (requestDto.getAddressId() == null) {
            throw new InvalidOrderBidDataException("배송지 정보가 없습니다.");
        }
        if (requestDto.getPaymentRequest() == null) {
            throw new InvalidPaymentShipmentDataException("결제 정보가 없습니다.");
        }

        log.info("사용자 [{}]가 판매 입찰(ID: {})에 대한 즉시 구매를 요청합니다. 창고 보관 여부: {}",
                email, requestDto.getSaleBidId(), requestDto.isWarehouseStorage());

        // 결제 정보 보강
        PaymentRequestDto paymentRequest = enrichPaymentRequest(requestDto.getPaymentRequest(), email);

        // 서비스 호출
        Long orderId = orderBidCommandService.createInstantOrderBid(
                email,
                requestDto.getSaleBidId(),
                requestDto.getAddressId(),
                requestDto.isWarehouseStorage(),
                paymentRequest
        ).getId();

        return ResponseEntity.ok(orderId);
    }

    /**
     * 결제 요청 정보를 보강합니다.
     *
     * @param paymentRequest 결제 요청 정보
     * @param email 사용자 이메일
     * @return 보강된 결제 요청 정보
     */
    private PaymentRequestDto enrichPaymentRequest(PaymentRequestDto paymentRequest, String email) {
        paymentRequest.setUserEmail(email);
        paymentRequest.setOrderId(null); // 초기에는 null로 설정, 이후 Order 생성 후 업데이트 가능
        return paymentRequest;
    }
}