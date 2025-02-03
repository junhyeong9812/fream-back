package com.fream.back.domain.payment.entity;

public enum PaymentStatus {
    PENDING, // 결제 대기
    PAID, // 결제 완료
    REFUND_REQUESTED, // 환불 대기
    REFUNDED; // 환불 완료

    public boolean canTransitionTo(PaymentStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == PAID;
            case PAID:
                return newStatus == REFUND_REQUESTED || newStatus == REFUNDED;
            case REFUND_REQUESTED:
                return newStatus == REFUNDED;
            default:
                return false;
        }
    }
}
