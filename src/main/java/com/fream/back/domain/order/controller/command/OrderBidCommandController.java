package com.fream.back.domain.order.controller.command;

import com.fream.back.domain.order.dto.InstantOrderRequestDto;
import com.fream.back.domain.order.dto.OrderBidRequestDto;
import com.fream.back.domain.order.exception.*;
import com.fream.back.domain.order.service.command.OrderBidCommandService;
import com.fream.back.domain.payment.dto.PaymentRequestDto;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 주문 입찰 명령 컨트롤러
 */
@RestController
@RequestMapping("/order-bids")
@RequiredArgsConstructor
@Slf4j
@Validated
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
    public ResponseEntity<ResponseDto<Long>> createOrderBid(@RequestBody @Valid OrderBidRequestDto requestDto) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrderBid("주문 입찰 생성");

        log.info("사용자 [{}]가 상품 사이즈(ID: {})에 대한 주문 입찰을 생성합니다. 입찰 가격: {}",
                email, requestDto.getProductSizeId(), requestDto.getBidPrice());

        // 서비스 호출
        Long orderBidId = orderBidCommandService.createOrderBid(
                email,
                requestDto.getProductSizeId(),
                requestDto.getBidPrice()
        ).getId();

        return ResponseEntity.ok(ResponseDto.success(orderBidId, "주문 입찰이 성공적으로 생성되었습니다."));
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
    public ResponseEntity<ResponseDto<Void>> deleteOrderBid(@PathVariable("orderBidId") Long orderBidId) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrderBid("주문 입찰 삭제");

        log.info("사용자 [{}]가 주문 입찰(ID: {})의 삭제를 요청합니다.", email, orderBidId);

        // 서비스 호출
        orderBidCommandService.deleteOrderBid(orderBidId);

        return ResponseEntity.ok(ResponseDto.success(null, "주문 입찰이 성공적으로 삭제되었습니다."));
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
    public ResponseEntity<ResponseDto<Long>> createInstantOrderBid(@RequestBody @Valid InstantOrderRequestDto requestDto) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrderBid("즉시 구매 입찰 생성");

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

        return ResponseEntity.ok(ResponseDto.success(orderId, "즉시 구매가 성공적으로 처리되었습니다."));
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