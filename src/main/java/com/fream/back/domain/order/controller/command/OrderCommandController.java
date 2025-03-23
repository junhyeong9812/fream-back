package com.fream.back.domain.order.controller.command;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.exception.InvalidPaymentShipmentDataException;
import com.fream.back.domain.order.exception.OrderAccessDeniedException;
import com.fream.back.domain.order.service.command.OrderCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<Void> processPaymentAndShipment(
            @PathVariable("orderId") Long orderId,
            @RequestBody PayAndShipmentRequestDto requestDto
    ) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractEmailFromSecurityContext();
        if (email == null || email.trim().isEmpty()) {
            log.warn("결제 및 배송 처리 시 사용자 이메일을 가져올 수 없습니다.");
            throw new OrderAccessDeniedException("사용자 정보를 가져올 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        // 요청 DTO 검증
        if (requestDto == null) {
            throw new InvalidPaymentShipmentDataException("결제 및 배송 정보가 없습니다.");
        }

        log.info("사용자 [{}]가 주문(ID: {})의 결제 및 배송 처리를 요청합니다.", email, orderId);

        // 서비스 호출
        orderCommandService.processPaymentAndShipment(orderId, email, requestDto);

        return ResponseEntity.ok().build();
    }
}