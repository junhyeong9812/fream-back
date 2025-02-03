package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.enumType.GenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreateRequestDto {
    private String name; // 상품명
    private String englishName; // 상품 영어명
    private int releasePrice; // 발매가
    private String modelNumber; // 모델 번호
    private String releaseDate; // 출시일
    private String brandName; // 브랜드명
    private String mainCategoryName; // 메인 카테고리명
    private String categoryName; // 서브 카테고리명
    private String collectionName; // 컬렉션명 (옵션)
    private GenderType gender; // 상품 성별 추가
}
