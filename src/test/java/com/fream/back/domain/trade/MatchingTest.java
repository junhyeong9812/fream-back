package com.fream.back.domain.trade;

import com.fream.back.domain.trade.entity.Bid;
import com.fream.back.domain.trade.entity.BidDirection;
import com.fream.back.domain.trade.entity.BidStatus;
import com.fream.back.domain.trade.event.BidMatchedEvent;
import com.fream.back.domain.trade.repository.BidRepository;
import com.fream.back.domain.trade.service.MatchingService;
import com.fream.back.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * trade 매칭 엔진 검증 — CAS 원자적 체결(중복 방지) + 즉시구매 매칭/이벤트.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class MatchingTest {

    @Autowired
    private BidRepository bidRepository;

    private static final List<BidStatus> OPEN = List.of(BidStatus.PENDING, BidStatus.PARTIALLY_MATCHED);

    @Test
    void claimFill_isAtomic_secondClaimOnSameBidFails() {
        Bid sell = bidRepository.save(Bid.open(BidDirection.SELL, 1L, 100L, 50_000, 1, true));

        int first = bidRepository.claimFill(sell.getId(), 1, "match-1", OPEN);
        int second = bidRepository.claimFill(sell.getId(), 1, "match-2", OPEN);

        assertThat(first).isEqualTo(1);   // 첫 체결 성공
        assertThat(second).isZero();      // 같은 입찰 재체결 차단(동시 중복 방지)

        Bid reloaded = bidRepository.findById(sell.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BidStatus.MATCHED);
        assertThat(reloaded.getRemainingQuantity()).isZero();
        assertThat(reloaded.getMatchId()).isEqualTo("match-1");
    }

    @Test
    void instantBuy_matchesLowestSellBid_andPublishesEvent() {
        List<Object> published = new ArrayList<>();
        ApplicationEventPublisher publisher = published::add;
        MatchingService matchingService = new MatchingService(bidRepository, publisher);

        // 같은 상품사이즈에 가격이 다른 두 판매 입찰
        Bid cheaper = bidRepository.save(Bid.open(BidDirection.SELL, 1L, 100L, 50_000, 1, true));
        bidRepository.save(Bid.open(BidDirection.SELL, 1L, 101L, 60_000, 1, true));

        BidMatchedEvent event = matchingService.instantBuy(1L, 999L, 1);

        // 최저가(50,000, sellerId=100) 입찰이 체결되고 이벤트 발행
        assertThat(event.price()).isEqualTo(50_000);
        assertThat(event.sellerId()).isEqualTo(100L);
        assertThat(event.buyerId()).isEqualTo(999L);
        assertThat(event.sellBidId()).isEqualTo(cheaper.getId());
        assertThat(event.matchId()).isNotBlank();
        assertThat(published).containsExactly(event);

        Bid reloaded = bidRepository.findById(cheaper.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BidStatus.MATCHED);
    }

    @Test
    void instantBuy_noOpenSellBid_throws() {
        MatchingService matchingService = new MatchingService(bidRepository, e -> {});
        assertThatThrownBy(() -> matchingService.instantBuy(999L, 1L, 1))
                .isInstanceOf(IllegalStateException.class);
    }
}
