package com.fream.back.domain.trade.repository;

import com.fream.back.domain.trade.entity.Bid;
import com.fream.back.domain.trade.entity.BidDirection;
import com.fream.back.domain.trade.entity.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 입찰 리포지토리. 매칭(반대 방향 미체결 입찰 탐색)·조회의 기반.
 */
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByBidderId(Long bidderId);

    List<Bid> findByMatchId(String matchId);

    /**
     * 특정 상품사이즈의 미체결 입찰을 방향별로 가격 우선 정렬해 조회(매칭 후보 탐색).
     * 즉시구매는 SELL을 최저가 우선, 즉시판매는 BUY를 최고가 우선으로 호출 측이 정렬 의도에 맞게 사용.
     */
    @Query("SELECT b FROM Bid b WHERE b.productSizeId = :productSizeId " +
            "AND b.direction = :direction AND b.status IN :openStatuses " +
            "AND b.remainingQuantity >= :minQuantity ORDER BY b.price ASC, b.id ASC")
    List<Bid> findOpenByDirectionPriceAsc(@Param("productSizeId") Long productSizeId,
                                          @Param("direction") BidDirection direction,
                                          @Param("openStatuses") Collection<BidStatus> openStatuses,
                                          @Param("minQuantity") int minQuantity);

    /**
     * 원자적 체결 클레임(CAS). 미체결 + 잔여수량 충분일 때만 잔여를 차감하고 matchId/상태를 갱신한다.
     * 영향 행 0 = 이미 다른 트랜잭션이 체결(경쟁 패배). 같은 입찰의 동시 중복 체결을 DB 레벨에서 방지.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Bid b SET b.remainingQuantity = b.remainingQuantity - :quantity, " +
            "b.matchId = :matchId, " +
            "b.status = CASE WHEN b.remainingQuantity - :quantity = 0 " +
            "THEN com.fream.back.domain.trade.entity.BidStatus.MATCHED " +
            "ELSE com.fream.back.domain.trade.entity.BidStatus.PARTIALLY_MATCHED END " +
            "WHERE b.id = :id AND b.status IN :openStatuses AND b.remainingQuantity >= :quantity")
    int claimFill(@Param("id") Long id,
                  @Param("quantity") int quantity,
                  @Param("matchId") String matchId,
                  @Param("openStatuses") Collection<BidStatus> openStatuses);
}
