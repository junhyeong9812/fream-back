package com.fream.back.domain.trade.entity;

/**
 * 입찰 방향. 구매(BUY)·판매(SELL)를 하나의 Bid 개념으로 통합한다.
 */
public enum BidDirection {
    BUY,   // 구매 입찰
    SELL   // 판매 입찰
}
