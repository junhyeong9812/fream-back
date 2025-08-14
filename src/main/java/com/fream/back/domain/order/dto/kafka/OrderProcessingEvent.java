package com.fream.back.domain.order.dto.kafka;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 처리 이벤트 DTO
 * 결제 + 배송 + 창고보관을 포함한 전체 주문 처리를 비동기로 처리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderProcessingEvent {

    /**
     * 주문 ID (멱등성 보장을 위한 고유 키)
     */
    private Long orderId;

    /**
     * 사용자 이메일
     */
    private String userEmail;

    /**
     * 결제 및 배송 요청 정보 (전체 데이터)
     */
    private PayAndShipmentRequestDto requestDto;

    /**
     * 이벤트 생성 시간
     */
    private LocalDateTime eventCreatedAt;

    /**
     * 재시도 횟수
     */
    private Integer retryCount;

    /**
     * 이벤트 ID (중복 처리 방지)
     */
    private String eventId;

    /**
     * 처리 단계 (PAYMENT, SHIPMENT, WAREHOUSE, COMPLETE)
     */
    private String processingStep;

    public static OrderProcessingEvent create(Long orderId, String userEmail,
                                              PayAndShipmentRequestDto requestDto) {
        return OrderProcessingEvent.builder()
                .orderId(orderId)
                .userEmail(userEmail)
                .requestDto(requestDto)
                .eventCreatedAt(LocalDateTime.now())
                .retryCount(0)
                .eventId(generateEventId(orderId))
                .processingStep("STARTED")
                .build();
    }

    /**
     * 재시도를 위한 이벤트 생성
     */
    public OrderProcessingEvent withRetry() {
        return OrderProcessingEvent.builder()
                .orderId(this.orderId)
                .userEmail(this.userEmail)
                .requestDto(this.requestDto)
                .eventCreatedAt(LocalDateTime.now())
                .retryCount(this.retryCount + 1)
                .eventId(this.eventId)
                .processingStep(this.processingStep)
                .build();
    }

    /**
     * 처리 단계 업데이트
     */
    public OrderProcessingEvent withStep(String step) {
        this.processingStep = step;
        return this;
    }

    private static String generateEventId(Long orderId) {
        return "order_processing_" + orderId + "_" + System.currentTimeMillis();
    }

    /**
     * 최대 재시도 횟수 확인
     */
    public boolean isMaxRetryExceeded() {
        return this.retryCount >= 3;
    }
}