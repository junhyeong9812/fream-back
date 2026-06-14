/**
 * trade(매칭) 컨텍스트 — 입찰(Bid, BUY/SELL 통합)과 매칭을 소유한다.
 *
 * <p>catalog·identity를 ID로만 참조하므로 모듈 의존이 없다(allowedDependencies = {}).
 * 매칭 성사는 향후 BidMatchedEvent로 발행하여 order/sale이 거래를 생성한다(단방향).
 */
@ApplicationModule(displayName = "Trade", allowedDependencies = {})
package com.fream.back.domain.trade;

import org.springframework.modulith.ApplicationModule;
