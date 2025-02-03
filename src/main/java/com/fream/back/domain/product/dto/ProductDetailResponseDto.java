package com.fream.back.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailResponseDto {
    private Long id; // 상품 ID
    private String name; // 상품명
    private String englishName; // 상품 영어명
    private int releasePrice; // 발매가
    private String thumbnailImageUrl; // 대표 이미지 URL
    private Long colorId; // 색상 ID 추가
    private String colorName; // 색상명
    private String content; // 색상 상세 설명
    private Long interestCount; // 관심 수 추가
    private List<SizeDetailDto> sizes; // 사이즈 정보 리스트
    private List<ColorDetailDto> otherColors; // 다른 색상 정보 리스트 추가

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SizeDetailDto {
        private String size; // 사이즈 이름
        private int purchasePrice; // 구매가
        private int salePrice; // 판매가
        private int quantity; // 재고 수량
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ColorDetailDto {
        private Long colorId; // 색상 ID
        private String colorName; // 색상명
        private String thumbnailImageUrl; // 색상 대표 이미지 URL
        private String content; // 색상 상세 설명
    }
}

