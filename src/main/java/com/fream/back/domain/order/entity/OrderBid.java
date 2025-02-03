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

    public void assignOrder(Order order) {
        this.order = order;
        this.status = BidStatus.MATCHED;

        // Order 엔티티에도 OrderBid 설정
        if (order != null && order.getOrderBid() != this) {
            order.assignOrderBid(this);
        }
    }

    public void assignSale(Sale sale) {
        this.sale = sale;
        this.status = BidStatus.MATCHED;
    }
    public void markAsInstantPurchase() {
        this.isInstantPurchase = true;
    }

    public void updateStatus(BidStatus newStatus) {
        this.status = newStatus;
    }
}

