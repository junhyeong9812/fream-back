package com.fream.back.domain.order.controller.command;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.exception.InvalidPaymentShipmentDataException;
import com.fream.back.domain.order.exception.OrderAccessDeniedException;
import com.fream.back.domain.order.service.command.OrderCommandService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 명령 컨트롤러
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderCommandController {

    private final OrderCommandService orderCommandService;

    /**
     * 주문의 결제 및 배송 정보를 처리합니다.
     *
     * @param orderId 주문 ID
     * @param requestDto 결제 및 배송 요청 정보
     * @return 응답 엔티티
     * @throws OrderAccessDeniedException 주문에 대한 접근 권한이 없는 경우
     * @throws InvalidPaymentShipmentDataException 결제 및 배송 정보가 유효하지 않은 경우
     */
    @PostMapping("/{orderId}/process-payment-shipment")
    public ResponseEntity<ResponseDto<Void>> processPaymentAndShipment(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid PayAndShipmentRequestDto requestDto
    ) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrder("결제 및 배송 처리");

        log.info("사용자 [{}]가 주문(ID: {})의 결제 및 배송 처리를 요청합니다.", email, orderId);

        // 서비스 호출
        orderCommandService.processPaymentAndShipment(orderId, email, requestDto);

        return ResponseEntity.ok(ResponseDto.success(null, "결제 및 배송 처리가 성공적으로 완료되었습니다."));
    }
}