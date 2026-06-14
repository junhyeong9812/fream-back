package com.fream.back.domain.trade.entity;

/**
 * 입찰 상태.
 */
public enum BidStatus {
    PENDING,            // 대기(미체결)
    PARTIALLY_MATCHED,  // 부분 체결(잔여 수량 존재)
    MATCHED,            // 전량 체결
    CANCELLED,          // 취소
    EXPIRED             // 만료
}
