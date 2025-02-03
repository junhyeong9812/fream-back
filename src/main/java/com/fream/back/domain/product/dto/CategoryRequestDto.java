package com.fream.back.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequestDto {
    private String mainCategoryName; // 메인 카테고리명
    private String subCategoryName; // 서브 카테고리명 (선택)
}
