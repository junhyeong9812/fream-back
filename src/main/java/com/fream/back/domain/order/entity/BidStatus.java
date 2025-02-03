package com.fream.back.domain.order.entity;

public enum BidStatus {
    PENDING, // 입찰 대기 중
    MATCHED, // 매칭 완료
    CANCELLED, // 입찰 취소
    COMPLETED; // 거래 완료
}