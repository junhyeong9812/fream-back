package com.fream.back.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResponseDto {
    private Long id;
    private String name;
    private String englishName;
    private int releasePrice;
    private String thumbnailImageUrl; // 대표 이미지 URL
    private Integer price; // 가장 낮은 구매가 추가
    private String colorName; // 해당 이미지의 색상명 추가
    private Long colorId;        // 컬러 ID 추가
    private Long interestCount; // 관심 수 추가

    private Long styleCount;              // 스타일 수
    private Long tradeCount;              // 거래 수 (OrderBid 중 COMPLETED)
}

