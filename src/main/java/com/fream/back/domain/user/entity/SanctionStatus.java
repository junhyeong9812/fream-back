package com.fream.back.domain.user.entity;

// 제재 상태
public enum SanctionStatus {
    PENDING,  // 승인 대기중
    ACTIVE,   // 활성
    EXPIRED,  // 만료됨
    REJECTED, // 거부됨
    CANCELED  // 취소됨
}