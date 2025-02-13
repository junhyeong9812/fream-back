package com.fream.back.domain.warehouseStorage.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WarehouseStatusCountDto {
    private long inStorageCount;        // 단순 보관 중
    private long associatedWithOrderCount;  // 구매와 연결된 보관
    private long removedFromStorageCount;   // 보관 해제
    private long onAuctionCount;        // 입찰 중
    private long soldCount;             // 판매 완료
}