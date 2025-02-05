package com.fream.back.domain.order.controller.command;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.service.command.OrderCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final OrderCommandService orderCommandService;

    // 결제 및 배송 처리
    @PostMapping("/{orderId}/process-payment-shipment")
    public ResponseEntity<Void> processPaymentAndShipment(
            @PathVariable("orderId") Long orderId,
            @RequestBody PayAndShipmentRequestDto requestDto
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 이메일 추출
        orderCommandService.processPaymentAndShipment(orderId, email, requestDto);
        return ResponseEntity.ok().build();
    }
}
