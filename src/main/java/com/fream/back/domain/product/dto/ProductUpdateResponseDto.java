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
public class ProductUpdateResponseDto {
    private Long id;
    private String name;
    private String englishName;
    private int releasePrice;
    private String modelNumber;
    private String releaseDate;
    private String brandName;
    private String categoryName;
    private String collectionName;
    private GenderType gender; // 상품 성별 추가


}
