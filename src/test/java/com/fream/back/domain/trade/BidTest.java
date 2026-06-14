package com.fream.back.domain.trade;

import com.fream.back.domain.trade.entity.Bid;
import com.fream.back.domain.trade.entity.BidDirection;
import com.fream.back.domain.trade.entity.BidStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bid 도메인 로직 단위 테스트 — 등록/부분·전량 체결/취소/불변식.
 */
class BidTest {

    @Test
    void open_createsPendingBidWithFullRemaining() {
        Bid bid = Bid.open(BidDirection.SELL, 1L, 10L, 50_000, 3, true);

        assertThat(bid.getStatus()).isEqualTo(BidStatus.PENDING);
        assertThat(bid.getRemainingQuantity()).isEqualTo(3);
        assertThat(bid.isInstant()).isTrue();
        assertThat(bid.getMatchId()).isNull();
    }

    @Test
    void open_rejectsNonPositivePriceOrQuantity() {
        assertThatThrownBy(() -> Bid.open(BidDirection.BUY, 1L, 10L, 0, 1, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Bid.open(BidDirection.BUY, 1L, 10L, 1000, 0, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fill_partialThenFull_transitionsStatusAndTracksRemaining() {
        Bid bid = Bid.open(BidDirection.BUY, 1L, 10L, 50_000, 3, false);

        bid.fill(1, "match-1");
        assertThat(bid.getStatus()).isEqualTo(BidStatus.PARTIALLY_MATCHED);
        assertThat(bid.getRemainingQuantity()).isEqualTo(2);
        assertThat(bid.getMatchId()).isEqualTo("match-1");

        bid.fill(2, "match-2");
        assertThat(bid.getStatus()).isEqualTo(BidStatus.MATCHED);
        assertThat(bid.getRemainingQuantity()).isZero();
    }

    @Test
    void fill_rejectsOverRemainingOrMatchedState() {
        Bid bid = Bid.open(BidDirection.BUY, 1L, 10L, 50_000, 1, false);
        assertThatThrownBy(() -> bid.fill(2, "m"))
                .isInstanceOf(IllegalArgumentException.class);

        bid.fill(1, "m");
        assertThatThrownBy(() -> bid.fill(1, "m2"))
                .isInstanceOf(IllegalStateException.class); // 이미 MATCHED
    }

    @Test
    void cancel_fromPending_succeeds_butNotFromMatched() {
        Bid pending = Bid.open(BidDirection.SELL, 1L, 10L, 1000, 1, false);
        pending.cancel();
        assertThat(pending.getStatus()).isEqualTo(BidStatus.CANCELLED);

        Bid matched = Bid.open(BidDirection.SELL, 1L, 10L, 1000, 1, false);
        matched.fill(1, "m");
        assertThatThrownBy(matched::cancel).isInstanceOf(IllegalStateException.class);
    }
}
