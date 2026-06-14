package com.fream.back.domain.trade.event;

import com.fream.back.domain.trade.entity.BidDirection;

/**
 * 입찰 매칭 성사 이벤트. trade 모듈이 발행하고, order/sale 모듈이 수신해 각 거래를 생성한다(단방향).
 *
 * @param matchId         매칭 식별자(UUID) — Order/Sale이 한 매칭의 양면을 연결하는 키
 * @param productSizeId   상품 사이즈 ID
 * @param price           체결 가격
 * @param quantity        체결 수량
 * @param sellBidId       체결된 판매 입찰 ID
 * @param buyerId         구매자 ID
 * @param sellerId        판매자 ID
 * @param takerDirection  체결을 일으킨 측(BUY=즉시구매, SELL=즉시판매)
 */
public record BidMatchedEvent(
        String matchId,
        Long productSizeId,
        int price,
        int quantity,
        Long sellBidId,
        Long buyerId,
        Long sellerId,
        BidDirection takerDirection
) {
}
