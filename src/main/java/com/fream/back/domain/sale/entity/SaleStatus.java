package com.fream.back.domain.sale.entity;

public enum SaleStatus {
    PENDING_SHIPMENT,    // 판매자 발송 대기
    IN_TRANSIT,          // 배송 중
    IN_INSPECTION,       // 검수 중
    FAILED_INSPECTION,   // 검수 불합격
    IN_STORAGE,          // 창고 보관 중
    ON_AUCTION,          // 판매 입찰 중
    SOLD,                // 판매 완료
    AUCTION_EXPIRED;     // 입찰 기한 만료

    // 상태 전환 규칙
    public boolean canTransitionTo(SaleStatus newStatus) {
        return switch (this) {
            case PENDING_SHIPMENT -> newStatus == IN_TRANSIT;
            case IN_TRANSIT -> newStatus == IN_INSPECTION;
            case IN_INSPECTION -> newStatus == FAILED_INSPECTION || newStatus == IN_STORAGE;
            case IN_STORAGE -> newStatus == ON_AUCTION || newStatus == AUCTION_EXPIRED;
            case ON_AUCTION -> newStatus == SOLD || newStatus == AUCTION_EXPIRED;
            case SOLD, AUCTION_EXPIRED, FAILED_INSPECTION -> false;
        };
    }
}
