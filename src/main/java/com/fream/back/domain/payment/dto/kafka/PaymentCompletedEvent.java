package com.fream.back.domain.payment.dto.kafka;

import com.fream.back.domain.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트 DTO
 * 결제 처리 완료 후 다른 서비스들에게 알림을 위해 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    /**
     * 결제 ID
     */
    private Long paymentId;

    /**
     * 주문 ID
     */
    private Long orderId;

    /**
     * 사용자 이메일
     */
    private String userEmail;

    /**
     * 결제 금액
     */
    private Integer paidAmount;

    /**
     * 결제 상태
     */
    private PaymentStatus paymentStatus;

    /**
     * 결제 성공 여부
     */
    private Boolean isSuccess;

    /**
     * 결제 완료 시간
     */
    private LocalDateTime paymentCompletedAt;

    /**
     * 결제 방법 (CARD, ACCOUNT, GENERAL)
     */
    private String paymentMethod;

    /**
     * PG사 거래 고유번호
     */
    private String impUid;

    /**
     * 이벤트 생성 시간
     */
    private LocalDateTime eventCreatedAt;

    /**
     * 이벤트 ID (중복 처리 방지)
     */
    private String eventId;

    public static PaymentCompletedEvent create(Long paymentId, Long orderId, String userEmail,
                                               Integer paidAmount, PaymentStatus status, Boolean isSuccess,
                                               String paymentMethod, String impUid) {
        return PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userEmail(userEmail)
                .paidAmount(paidAmount)
                .paymentStatus(status)
                .isSuccess(isSuccess)
                .paymentCompletedAt(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .impUid(impUid)
                .eventCreatedAt(LocalDateTime.now())
                .eventId(generateEventId(paymentId, orderId))
                .build();
    }

    private static String generateEventId(Long paymentId, Long orderId) {
        return "payment_completed_" + orderId + "_" + paymentId + "_" + System.currentTimeMillis();
    }

    /**
     * 결제 성공 이벤트인지 확인
     */
    public boolean isPaymentSuccess() {
        return Boolean.TRUE.equals(this.isSuccess) &&
                PaymentStatus.PAID.equals(this.paymentStatus);
    }

    /**
     * 결제 실패 이벤트인지 확인
     */
    public boolean isPaymentFailed() {
        return Boolean.FALSE.equals(this.isSuccess) ||
                PaymentStatus.FAILED.equals(this.paymentStatus);
    }
}