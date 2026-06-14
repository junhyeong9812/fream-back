package com.fream.back.domain.trade.repository;

import com.fream.back.domain.trade.entity.Bid;
import com.fream.back.domain.trade.entity.BidDirection;
import com.fream.back.domain.trade.entity.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 입찰 리포지토리. 매칭(반대 방향 미체결 입찰 탐색)·조회의 기반.
 */
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByBidderId(Long bidderId);

    List<Bid> findByProductSizeIdAndDirectionAndStatus(Long productSizeId, BidDirection direction, BidStatus status);

    List<Bid> findByMatchId(String matchId);
}
