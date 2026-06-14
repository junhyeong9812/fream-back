/**
 * trade 모듈의 공개 이벤트(타 모듈 제공). order/sale이 BidMatchedEvent를 수신해 거래를 생성한다.
 */
@NamedInterface("event")
package com.fream.back.domain.trade.event;

import org.springframework.modulith.NamedInterface;
