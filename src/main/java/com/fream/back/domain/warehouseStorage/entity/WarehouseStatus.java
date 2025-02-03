package com.fream.back.domain.warehouseStorage.entity;

public enum WarehouseStatus {
    IN_STORAGE,             // 단순 보관 중
    ASSOCIATED_WITH_ORDER,  // 구매와 연결된 보관
    REMOVED_FROM_STORAGE,
    ON_AUCTION,             // 입찰 중
    SOLD;                   // 판매 완료
}