package com.fream.back.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderBidStatusCountDto {
    private long pendingCount;              // 대기 중
    private long matchedCount;              // 매칭 완료
    private long cancelledOrCompletedCount; // 취소 및 완료
}
