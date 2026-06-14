package com.fream.back.domain.characterization;

import com.fream.back.domain.order.entity.BidStatus;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderBid;
import com.fream.back.domain.sale.entity.Sale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 특성화 테스트(순수 단위) — OrderBid ↔ Order/Sale 매칭 연관 로직.
 *
 * <p>trade(매칭) 재설계(#12)는 OrderBid·SaleBid가 Order·Sale을 @OneToOne 교차참조하는 구조를
 * 통합 Bid + matchId(ID 참조)로 바꾼다. 그 전에 현재 매칭 연관/상태 동작을 고정해
 * 재설계가 동일한 매칭 의미(체결 시 MATCHED, 양방향 연결)를 보존하는지 비교 기준을 만든다.
 *
 * <p>엔티티 연관 로직만 검증하므로 DB/ProductSize 그래프 없이 순수 단위로 수행한다.
 */
class OrderBidLinkageCharacterizationTest {

    @Test
    void assignOrder_setsMatchedStatusAndBidirectionalLink() {
        OrderBid bid = OrderBid.builder().bidPrice(10_000).status(BidStatus.PENDING).build();
        Order order = Order.builder().totalAmount(10_000).build();

        bid.assignOrder(order);

        assertThat(bid.getStatus()).isEqualTo(BidStatus.MATCHED);
        assertThat(bid.getOrder()).isSameAs(order);
        // Order 쪽 back-reference 동기화
        assertThat(order.getOrderBid()).isSameAs(bid);
    }

    @Test
    void assignOrder_reassign_detachesPreviousOrder() {
        OrderBid bid = OrderBid.builder().bidPrice(10_000).build();
        Order first = Order.builder().build();
        Order second = Order.builder().build();

        bid.assignOrder(first);
        bid.assignOrder(second);

        assertThat(bid.getOrder()).isSameAs(second);
        assertThat(second.getOrderBid()).isSameAs(bid);
        // 이전 Order는 연결 해제
        assertThat(first.getOrderBid()).isNull();
    }

    @Test
    void assignSale_setsMatchedStatusAndSaleLink() {
        OrderBid bid = OrderBid.builder().bidPrice(10_000).status(BidStatus.PENDING).build();
        Sale sale = Sale.builder().build();

        bid.assignSale(sale);

        assertThat(bid.getStatus()).isEqualTo(BidStatus.MATCHED);
        assertThat(bid.getSale()).isSameAs(sale);
    }

    @Test
    void markAsInstantPurchase_setsFlag() {
        OrderBid bid = OrderBid.builder().bidPrice(10_000).build();

        bid.markAsInstantPurchase();

        assertThat(bid.isInstantPurchase()).isTrue();
    }
}
