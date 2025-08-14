package com.fream.back.domain.payment.dto.kafka;

import com.fream.back.domain.payment.dto.PaymentRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 처리를 위한 Kafka 이벤트 DTO
 * 주문 생성 후 비동기 결제 처리를 위해 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    /**
     * 주문 ID (멱등성 보장을 위한 고유 키)
     */
    private Long orderId;

    /**
     * 사용자 이메일
     */
    private String userEmail;

    /**
     * 결제 요청 정보
     */
    private PaymentRequestDto paymentRequest;

    /**
     * 이벤트 생성 시간
     */
    private LocalDateTime eventCreatedAt;

    /**
     * 재시도 횟수 (결제 실패 시 재시도를 위함)
     */
    private Integer retryCount;

    /**
     * 이벤트 ID (중복 처리 방지)
     */
    private String eventId;

    public static PaymentEvent create(Long orderId, String userEmail,
                                      PaymentRequestDto paymentRequest) {
        return PaymentEvent.builder()
                .orderId(orderId)
                .userEmail(userEmail)
                .paymentRequest(paymentRequest)
                .eventCreatedAt(LocalDateTime.now())
                .retryCount(0)
                .eventId(generateEventId(orderId))
                .build();
    }

    /**
     * 재시도를 위한 이벤트 생성
     */
    public PaymentEvent withRetry() {
        return PaymentEvent.builder()
                .orderId(this.orderId)
                .userEmail(this.userEmail)
                .paymentRequest(this.paymentRequest)
                .eventCreatedAt(LocalDateTime.now())
                .retryCount(this.retryCount + 1)
                .eventId(this.eventId)
                .build();
    }

    private static String generateEventId(Long orderId) {
        return "payment_" + orderId + "_" + System.currentTimeMillis();
    }

    /**
     * 최대 재시도 횟수 확인
     */
    public boolean isMaxRetryExceeded() {
        return this.retryCount >= 3; // 최대 3회 재시도
    }
}