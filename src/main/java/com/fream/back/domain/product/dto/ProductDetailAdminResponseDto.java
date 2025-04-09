package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.product.entity.enumType.GenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 상품 상세 응답 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailAdminResponseDto {
    private Long id;                   // 상품 ID
    private String name;               // 상품명
    private String englishName;        // 영문명
    private int releasePrice;          // 발매가
    private String modelNumber;        // 모델 번호
    private String releaseDate;        // 출시일
    private GenderType gender;         // 성별
    private String brandName;          // 브랜드명
    private String categoryName;       // 카테고리명
    private String mainCategoryName;   // 메인 카테고리명
    private String collectionName;     // 컬렉션명
    private List<ProductColorDetailAdminResponseDto> colors; // 색상 정보

    public static ProductDetailAdminResponseDto fromEntity(Product product) {
        List<ProductColorDetailAdminResponseDto> colorDtos = product.getColors().stream()
                .map(ProductColorDetailAdminResponseDto::fromEntity)
                .collect(Collectors.toList());

        return ProductDetailAdminResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .englishName(product.getEnglishName())
                .releasePrice(product.getReleasePrice())
                .modelNumber(product.getModelNumber())
                .releaseDate(product.getReleaseDate())
                .gender(product.getGender())
                .brandName(product.getBrand().getName())
                .categoryName(product.getCategory().getName())
                .mainCategoryName(product.getCategory().getParentCategory() != null
                        ? product.getCategory().getParentCategory().getName()
                        : null)
                .collectionName(product.getCollection() != null ? product.getCollection().getName() : null)
                .colors(colorDtos)
                .build();
    }
}
