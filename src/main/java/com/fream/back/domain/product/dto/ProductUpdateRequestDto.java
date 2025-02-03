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
public class ProductUpdateRequestDto {
    private String name;          // 상품명
    private String englishName;   // 상품 영어명
    private Integer releasePrice; // 발매가
    private String modelNumber;   // 모델 번호
    private String releaseDate;   // 출시일 (YYYY-MM-DD)
    private String brandName;     // 변경할 브랜드명
    private String categoryName;  // 변경할 카테고리명
    private String mainCategoryName; // 변경할 메인 카테고리명
    private String collectionName;   // 변경할 컬렉션명
    private GenderType gender; // 상품 성별 추가
}
