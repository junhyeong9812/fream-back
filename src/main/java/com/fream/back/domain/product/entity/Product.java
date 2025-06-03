package com.fream.back.domain.product.entity;

import com.fream.back.domain.product.dto.ProductUpdateRequestDto;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_brand", columnList = "brand_id"),                    // 브랜드별 상품 조회
        @Index(name = "idx_product_category", columnList = "category_id"),              // 카테고리별 상품 조회
        @Index(name = "idx_product_collection", columnList = "collection_id"),          // 컬렉션별 상품 조회
        @Index(name = "idx_product_gender", columnList = "gender"),                     // 성별 필터링
        @Index(name = "idx_product_price", columnList = "release_price"),               // 가격 필터링
        @Index(name = "idx_product_date", columnList = "release_date"),                 // 출시일 정렬
        @Index(name = "idx_product_brand_category", columnList = "brand_id, category_id"), // 복합 인덱스 (브랜드+카테고리)
        @Index(name = "idx_product_name", columnList = "name"),                         // 상품명 검색
        @Index(name = "idx_product_english_name", columnList = "english_name")          // 영어명 검색
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 상품 ID

    @Column(nullable = false)
    private String name; // 상품명

    @Column(nullable = false)
    private String englishName; // 상품 영어명

    @Column(nullable = false)
    private int releasePrice; // 발매가

    @Column(nullable = false)
    private String modelNumber; // 모델 번호

    @Column(nullable = false)
    private String releaseDate; // 출시일 (YYYY-MM-DD)

    @Enumerated(EnumType.STRING) // Enum 값을 String으로 저장
    @Column(nullable = false)
    private GenderType gender; // 성별 필터링 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand; // 브랜드 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // 상위 카테고리 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection; // 컬렉션 참조

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductColor> colors  = new ArrayList<>(); // 색상별 상세 정보

    // 연관관계 편의 메서드
    public void addProductColor(ProductColor color) {
        colors.add(color);
        color.assignProduct(this);
    }

    public void update(String name, String englishName, int releasePrice, String modelNumber, String releaseDate) {
        this.name = name;
        this.englishName = englishName;
        this.releasePrice = releasePrice;
        this.modelNumber = modelNumber;
        this.releaseDate = releaseDate;
    }
    public void updateProduct(ProductUpdateRequestDto request, Brand brand, Category category, Collection collection) {
        if (request.getName() != null) {
            this.name = request.getName();
        }
        if (request.getEnglishName() != null) {
            this.englishName = request.getEnglishName();
        }
        if (request.getReleasePrice() != null) {
            this.releasePrice = request.getReleasePrice();
        }
        if (request.getModelNumber() != null) {
            this.modelNumber = request.getModelNumber();
        }
        if (request.getReleaseDate() != null) {
            this.releaseDate = request.getReleaseDate();
        }
        if (brand != null) {
            this.brand = brand;
        }
        if (category != null) {
            this.category = category;
        }
        if (collection != null) {
            this.collection = collection;
        }
        if (request.getGender() != null) { // 요청에서 Gender 값이 전달된 경우 업데이트
            this.gender = request.getGender();
        }
    }


}
