package com.fream.back.domain.order.controller.command;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.exception.InvalidPaymentShipmentDataException;
import com.fream.back.domain.order.exception.OrderAccessDeniedException;
import com.fream.back.domain.order.service.kafka.OrderEventProducer;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 주문 명령 컨트롤러 (Kafka 비동기 처리)
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderCommandController {

    private final OrderEventProducer orderEventProducer; // 기존 OrderCommandService 대신 Kafka Producer 사용

    /**
     * 주문의 결제 및 배송 정보를 처리합니다.
     * ⭐ 전체 처리를 Kafka로 비동기 처리하고 즉시 응답
     *
     * @param orderId 주문 ID
     * @param requestDto 결제 및 배송 요청 정보
     * @return 처리 진행 상태 응답
     * @throws OrderAccessDeniedException 주문에 대한 접근 권한이 없는 경우
     * @throws InvalidPaymentShipmentDataException 결제 및 배송 정보가 유효하지 않은 경우
     */
    @PostMapping("/{orderId}/process-payment-shipment")
    public ResponseEntity<ResponseDto<Map<String, Object>>> processPaymentAndShipment(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid PayAndShipmentRequestDto requestDto
    ) {
        // 사용자 이메일 추출 및 검증
        String email = SecurityUtils.extractAndValidateEmailForOrder("결제 및 배송 처리");

        log.info("사용자 [{}]가 주문(ID: {})의 결제 및 배송 처리를 요청합니다.", email, orderId);

        try {
            // Kafka로 비동기 주문 처리 이벤트 발행
            orderEventProducer.sendOrderProcessingEvent(orderId, email, requestDto);

            // 즉시 응답 데이터 생성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("orderId", orderId);
            responseData.put("status", "PROCESSING");
            responseData.put("message", "주문이 접수되었습니다. 결제를 처리하고 있습니다.");
            responseData.put("estimatedProcessingTime", "1-3분");
            responseData.put("websocketTopic", "/topic/order/" + orderId);
            responseData.put("statusCheckUrl", "/api/orders/" + orderId + "/status");

            log.info("주문 처리 이벤트 발행 완료: 주문ID={}, 사용자={}", orderId, email);

            // 202 Accepted 응답 (처리 중임을 명시)
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ResponseDto.success(responseData, "주문 처리가 시작되었습니다. 처리 완료 시 알림을 받으실 수 있습니다."));

        } catch (Exception e) {
            log.error("주문 처리 이벤트 발행 실패: 주문ID={}, 사용자={}, 오류={}", orderId, email, e.getMessage(), e);

            // 실패 시 즉시 오류 응답
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("ORDER_PROCESSING_FAILED",
                            "주문 처리 요청 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    //    /**
//     * 주문의 결제 및 배송 정보를 처리합니다.
//     *
//     * @param orderId 주문 ID
//     * @param requestDto 결제 및 배송 요청 정보
//     * @return 응답 엔티티
//     * @throws OrderAccessDeniedException 주문에 대한 접근 권한이 없는 경우
//     * @throws InvalidPaymentShipmentDataException 결제 및 배송 정보가 유효하지 않은 경우
//     */
//    @PostMapping("/{orderId}/process-payment-shipment")
//    public ResponseEntity<ResponseDto<Void>> processPaymentAndShipment(
//            @PathVariable("orderId") Long orderId,
//            @RequestBody @Valid PayAndShipmentRequestDto requestDto
//    ) {
//        // 사용자 이메일 추출 및 검증
//        String email = SecurityUtils.extractAndValidateEmailForOrder("결제 및 배송 처리");
//
//        log.info("사용자 [{}]가 주문(ID: {})의 결제 및 배송 처리를 요청합니다.", email, orderId);
//
//        // 서비스 호출
//        orderCommandService.processPaymentAndShipment(orderId, email, requestDto);
//
//        return ResponseEntity.ok(ResponseDto.success(null, "결제 및 배송 처리가 성공적으로 완료되었습니다."));
//    }

    /**
     * 주문 처리 취소 API (선택적)
     * 처리 중인 주문을 취소할 수 있도록 제공
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ResponseDto<Map<String, Object>>> cancelOrderProcessing(@PathVariable Long orderId) {
        String email = SecurityUtils.extractAndValidateEmailForOrder("주문 처리 취소");

        log.info("주문 처리 취소 요청: 주문ID={}, 사용자={}", orderId, email);

        try {
            // TODO: 취소 로직 구현 (처리 상태에 따라 취소 가능 여부 판단)

            Map<String, Object> cancelData = new HashMap<>();
            cancelData.put("orderId", orderId);
            cancelData.put("status", "CANCELLED");
            cancelData.put("message", "주문 처리가 취소되었습니다.");

            return ResponseEntity.ok(ResponseDto.success(cancelData, "주문 처리 취소 성공"));

        } catch (Exception e) {
            log.error("주문 처리 취소 실패: 주문ID={}, 사용자={}, 오류={}", orderId, email, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("ORDER_CANCEL_FAILED",
                            "주문 처리 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
