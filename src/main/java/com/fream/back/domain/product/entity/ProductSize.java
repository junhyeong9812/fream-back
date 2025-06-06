package com.fream.back.domain.product.entity;

import com.fream.back.domain.product.entity.enumType.SizeType;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_size", indexes = {
        @Index(name = "idx_product_size_color", columnList = "product_color_id"),       // 색상별 사이즈 조회 (FK, 필수)
        @Index(name = "idx_product_size_purchase_price", columnList = "purchase_price") // 최저가 조회용 (자주 사용)
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSize extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SizeType sizeType; // SizeType 추가 (CLOTHING, SHOES, ACCESSORIES)


    @Column(nullable = false)
    private String size; // 사이즈 (예: 250, M, L)

    @Column(name = "purchase_price",nullable = false)
    private int purchasePrice; // 구매가

    @Column(nullable = false)
    private int salePrice; // 판매가

    @Column(nullable = false)
    private int quantity; // 재고 수량

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_color_id")
    private ProductColor productColor; // ProductColor 참조

    public void assignProductColor(ProductColor productColor) {
        this.productColor = productColor;
    }

    public void update(int purchasePrice, int salePrice, int quantity) {
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.quantity = quantity;
    }
}

