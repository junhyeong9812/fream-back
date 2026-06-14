package com.fream.back.domain.trade.service;

import com.fream.back.domain.trade.entity.Bid;
import com.fream.back.domain.trade.entity.BidDirection;
import com.fream.back.domain.trade.entity.BidStatus;
import com.fream.back.domain.trade.event.BidMatchedEvent;
import com.fream.back.domain.trade.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 입찰 등록·매칭 서비스.
 *
 * <p>동시성: 같은 입찰이 동시에 두 번 체결되지 않도록 {@code BidRepository.claimFill}(상태/잔여 조건부 갱신, CAS)로
 * 원자적으로 체결을 클레임한다. 영향 행 0이면 경쟁에 패배한 것이므로 다음 후보로 넘어간다.
 * 매칭 성사 시 {@link BidMatchedEvent}를 발행하여 order/sale 모듈이 거래를 생성하도록 한다(단방향).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    private static final List<BidStatus> OPEN_STATUSES =
            List.of(BidStatus.PENDING, BidStatus.PARTIALLY_MATCHED);

    private final BidRepository bidRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 입찰 등록(미체결 resting bid). 판매자의 즉시구매가 SELL 등록, 구매자의 BUY 입찰 등록 등.
     */
    public Bid placeBid(BidDirection direction, Long productSizeId, Long bidderId,
                        int price, int quantity, boolean instant) {
        return bidRepository.save(Bid.open(direction, productSizeId, bidderId, price, quantity, instant));
    }

    /**
     * 즉시구매 — 해당 상품사이즈의 최저가 판매 입찰을 원자적으로 체결한다.
     *
     * @return 매칭 성사 이벤트(이미 발행됨)
     * @throws IllegalStateException 체결 가능한 판매 입찰이 없을 때
     */
    public BidMatchedEvent instantBuy(Long productSizeId, Long buyerId, int quantity) {
        List<Bid> candidates = bidRepository.findOpenByDirectionPriceAsc(
                productSizeId, BidDirection.SELL, OPEN_STATUSES, quantity);

        for (Bid maker : candidates) {
            String matchId = UUID.randomUUID().toString();
            int claimed = bidRepository.claimFill(maker.getId(), quantity, matchId, OPEN_STATUSES);
            if (claimed == 1) {
                BidMatchedEvent event = new BidMatchedEvent(
                        matchId, productSizeId, maker.getPrice(), quantity,
                        maker.getId(), buyerId, maker.getBidderId(), BidDirection.BUY);
                eventPublisher.publishEvent(event);
                log.info("즉시구매 매칭 성사: matchId={}, sellBidId={}, buyerId={}, qty={}",
                        matchId, maker.getId(), buyerId, quantity);
                return event;
            }
            log.debug("체결 클레임 경쟁 패배, 다음 후보로: sellBidId={}", maker.getId());
        }
        throw new IllegalStateException("체결 가능한 판매 입찰이 없습니다: productSizeId=" + productSizeId);
    }
}
