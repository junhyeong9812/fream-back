package com.fream.back.domain.order.entity;

import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize; // 선택된 사이즈 정보

    private int quantity; // 주문 수량
    private int price; // 단가
    // Order를 설정하는 메서드 추가
    public void addOrder(Order order) {
        this.order = order;
    }
}
