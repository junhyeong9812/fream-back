package com.fream.back.domain.trade.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입찰(Bid) — BUY/SELL 통합 개념. trade(매칭) 컨텍스트의 핵심 애그리거트.
 *
 * <p>다른 모듈(catalog·identity)을 <b>ID로만</b> 참조한다(productSizeId·bidderId) — 엔티티 직접 결합 없음.
 * 매칭 성사 시 {@code matchId}(UUID)를 부여하며, Order/Sale은 이 matchId로 한 매칭의 양면을 연결한다.
 * 동시성(같은 입찰 중복 체결)은 {@code @Version} 낙관락 + 상태 전이 가드 + (서비스 계층의 상태 조건부 갱신)으로 방어한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "bid")
public class Bid extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidDirection direction; // BUY/SELL

    @Column(nullable = false)
    private Long productSizeId; // catalog의 상품 사이즈 ID(참조만)

    @Column(nullable = false)
    private Long bidderId; // identity의 사용자 ID(참조만)

    @Column(nullable = false)
    private int price; // 입찰 가격

    @Column(nullable = false)
    private int quantity; // 입찰 수량

    @Column(nullable = false)
    private int remainingQuantity; // 잔여 수량(부분 체결 추적)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(length = 36)
    private String matchId; // 매칭 성사 시 부여되는 UUID (Order/Sale 연결 키)

    @Column(nullable = false)
    private boolean instant; // 즉시 체결 입찰 여부(즉시구매/즉시판매)

    @Version
    private Long version; // 낙관적 락(동시 체결 방어)

    /**
     * 신규 입찰 등록(PENDING, 잔여 수량 = 전체 수량).
     */
    public static Bid open(BidDirection direction, Long productSizeId, Long bidderId,
                           int price, int quantity, boolean instant) {
        if (price <= 0) {
            throw new IllegalArgumentException("입찰 가격은 0보다 커야 합니다: " + price);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("입찰 수량은 0보다 커야 합니다: " + quantity);
        }
        return Bid.builder()
                .direction(direction)
                .productSizeId(productSizeId)
                .bidderId(bidderId)
                .price(price)
                .quantity(quantity)
                .remainingQuantity(quantity)
                .status(BidStatus.PENDING)
                .instant(instant)
                .build();
    }

    /**
     * 일부/전량 체결. 잔여 수량을 줄이고 matchId를 기록하며 상태를 전이한다.
     * 체결 가능 상태(PENDING/PARTIALLY_MATCHED)가 아니거나 수량이 잔여를 초과하면 거부한다.
     *
     * @param fillQuantity 이번에 체결되는 수량
     * @param matchId      매칭 식별자(UUID)
     */
    public void fill(int fillQuantity, String matchId) {
        if (status != BidStatus.PENDING && status != BidStatus.PARTIALLY_MATCHED) {
            throw new IllegalStateException("체결할 수 없는 상태입니다: " + status);
        }
        if (fillQuantity <= 0 || fillQuantity > remainingQuantity) {
            throw new IllegalArgumentException(
                    "체결 수량이 잘못되었습니다: fill=" + fillQuantity + ", remaining=" + remainingQuantity);
        }
        this.remainingQuantity -= fillQuantity;
        this.matchId = matchId;
        this.status = (this.remainingQuantity == 0) ? BidStatus.MATCHED : BidStatus.PARTIALLY_MATCHED;
    }

    /**
     * 취소(미체결 잔여가 있을 때).
     */
    public void cancel() {
        if (status == BidStatus.MATCHED || status == BidStatus.CANCELLED) {
            throw new IllegalStateException("취소할 수 없는 상태입니다: " + status);
        }
        this.status = BidStatus.CANCELLED;
    }
}
