package com.fream.back.domain.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT, // 결제 대기
    PAYMENT_COMPLETED, // 결제 완료
    PREPARING, // 상품 준비 중
    IN_WAREHOUSE, // 창고 보관 중
    SHIPMENT_STARTED, // 배송 시작
    IN_TRANSIT, // 배송 중
    COMPLETED, // 배송 완료
    REFUND_REQUESTED, // 환불 대기
    REFUNDED; // 환불 완료

    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING_PAYMENT:
                return newStatus == PAYMENT_COMPLETED || newStatus == COMPLETED|| newStatus == IN_WAREHOUSE ;
            case PAYMENT_COMPLETED:
                return newStatus == PREPARING || newStatus == REFUND_REQUESTED || newStatus == IN_WAREHOUSE ;
            case PREPARING:
                return newStatus == IN_WAREHOUSE || newStatus == SHIPMENT_STARTED;
            case IN_WAREHOUSE:
                return newStatus == SHIPMENT_STARTED || newStatus == COMPLETED; // 창고 보관에서 완료도 가능하도록 수정
            case SHIPMENT_STARTED:
                return newStatus == IN_TRANSIT;
            case IN_TRANSIT:
                return newStatus == COMPLETED;
            case REFUND_REQUESTED:
                return newStatus == REFUNDED;
            default:
                return false;
        }
    }
}
