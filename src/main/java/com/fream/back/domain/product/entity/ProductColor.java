package com.fream.back.domain.product.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_color", indexes = {
        @Index(name = "idx_product_color_name", columnList = "color_name"),         // 색상명 필터링 (핵심)
        @Index(name = "idx_product_color_product", columnList = "product_id")       // 상품별 색상 조회 (FK)
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColor extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 색상 ID

    @Column(nullable = false)
    private String colorName; // 색상명 (예: Midnight Navy)

    @Lob
    private String content; // 상세페이지 (HTML)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // 상위 Product 참조

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "thumbnail_image_id", nullable = true)
    private ProductImage thumbnailImage; // 대표 이미지

    @Builder.Default
    @OneToMany(mappedBy = "productColor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSize> sizes = new ArrayList<>(); // 사이즈 정보

    @Builder.Default
    @OneToMany(mappedBy = "productColor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> productImages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "productColor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDetailImage> productDetailImages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "productColor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interest> interests = new ArrayList<>();

    // 연관관계 편의 메서드
    public void assignProduct(Product product) {
        this.product = product;
    }

    public void addThumbnailImage(ProductImage thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
        if (thumbnailImage != null) {
            thumbnailImage.assignProductColor(this);
        }
    }

    public void addProductImage(ProductImage image) {
        productImages.add(image);
        image.assignProductColor(this);
    }

    public void addProductDetailImage(ProductDetailImage detailImage) {
        productDetailImages.add(detailImage);
        detailImage.assignProductColor(this);
    }

    public void addProductSize(ProductSize size) {
        sizes.add(size);
        size.assignProductColor(this);
    }

    public void update(String colorName, String content) {
        this.colorName = colorName;
        this.content = content;
    }


}

