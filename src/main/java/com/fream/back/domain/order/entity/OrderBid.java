package com.fream.back.domain.order.entity;

import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.user.entity.User;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 입찰 엔티티
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderBid extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 구매자 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize; // 구매 대상 상품 사이즈

    private int bidPrice; // 입찰 가격

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private BidStatus status = BidStatus.PENDING; // 입찰 상태 (대기 중, 매칭 완료 등)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order; // 매칭된 구매 엔티티

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale; // 매칭된 판매 엔티티

    // 즉시 구매 플래그
    @Builder.Default
    @Column(nullable = false)
    private boolean isInstantPurchase = false;

    /**
     * Order와의 양방향 관계를 설정합니다.
     * 기존 관계가 있다면 해제하고 새 관계를 설정합니다.
     *
     * @param order 연결할 Order 객체
     */
    public void assignOrder(Order order) {
        // 기존 관계가 있으면 해제
        if (this.order != null && this.order != order) {
            Order oldOrder = this.order;
            this.order = null;
            if (oldOrder.getOrderBid() == this) {
                oldOrder.assignOrderBid(null);
            }
        }

        // 새 관계 설정
        this.order = order;

        // 매칭 상태로 변경
        if (order != null) {
            this.status = BidStatus.MATCHED;

            // Order 쪽에도 관계 설정
            if (order.getOrderBid() != this) {
                order.assignOrderBid(this);
            }
        }
    }

    /**
     * Sale과의 양방향 관계를 설정합니다.
     *
     * @param sale 연결할 Sale 객체
     */
    public void assignSale(Sale sale) {
        this.sale = sale;
        this.status = BidStatus.MATCHED;
    }

    /**
     * 즉시 구매로 표시합니다.
     */
    public void markAsInstantPurchase() {
        this.isInstantPurchase = true;
    }

    /**
     * 입찰 상태를 업데이트합니다.
     *
     * @param newStatus 새 입찰 상태
     */
    public void updateStatus(BidStatus newStatus) {
        this.status = newStatus;
    }
}