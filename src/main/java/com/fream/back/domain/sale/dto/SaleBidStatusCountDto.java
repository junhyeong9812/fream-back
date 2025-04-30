package com.fream.back.domain.sale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 판매 입찰 상태별 카운트 DTO
 * 판매 입찰의 상태별 개수를 클라이언트에게 전달하기 위한 DTO입니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaleBidStatusCountDto {

    /**
     * 대기 중인 판매 입찰 개수
     */
    private long pendingCount;

    /**
     * 매칭 완료된 판매 입찰 개수
     */
    private long matchedCount;

    /**
     * 취소 또는 완료된 판매 입찰 개수
     */
    private long cancelledOrCompletedCount;

    /**
     * 전체 판매 입찰 개수
     *
     * @return 모든 상태의 판매 입찰 개수 합계
     */
    public long getTotalCount() {
        return pendingCount + matchedCount + cancelledOrCompletedCount;
    }

    /**
     * 대기 중인 판매 입찰 비율 (%)
     *
     * @return 전체 대비 대기 중 판매 입찰 비율 (0~100)
     */
    public double getPendingRatio() {
        long total = getTotalCount();
        if (total == 0) return 0;
        return Math.round((double) pendingCount / total * 100 * 10) / 10.0;
    }

    /**
     * 매칭 완료된 판매 입찰 비율 (%)
     *
     * @return 전체 대비 매칭 완료 판매 입찰 비율 (0~100)
     */
    public double getMatchedRatio() {
        long total = getTotalCount();
        if (total == 0) return 0;
        return Math.round((double) matchedCount / total * 100 * 10) / 10.0;
    }

    /**
     * 취소 또는 완료된 판매 입찰 비율 (%)
     *
     * @return 전체 대비 취소 또는 완료 판매 입찰 비율 (0~100)
     */
    public double getCancelledOrCompletedRatio() {
        long total = getTotalCount();
        if (total == 0) return 0;
        return Math.round((double) cancelledOrCompletedCount / total * 100 * 10) / 10.0;
    }
}