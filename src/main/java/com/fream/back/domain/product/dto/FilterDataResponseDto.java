package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.enumType.ColorType;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.entity.enumType.SizeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterDataResponseDto {
    private Map<String, List<String>> sizes; // SizeType별 사이즈 목록
    private List<GenderDto> genders;         // 성별 목록
    private List<ColorDto> colors;           // 색상 목록
    private List<DiscountGroupDto> discounts; // 혜택/할인 목록
    private List<PriceRangeDto> priceRanges; // 가격대 목록
    private List<CategoryDto> categories;    // 카테고리 목록
    private List<BrandDto> brands;           // 브랜드 목록
    private List<CollectionDto> collections; // 컬렉션 목록

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenderDto {
        private String value; // enum 값
        private String label; // 표시 텍스트
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColorDto {
        private String key;   // enum 값
        private String name;  // 표시 텍스트
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiscountGroupDto {
        private String title;          // 그룹 제목 (혜택, 할인율)
        private List<String> options;  // 옵션 목록
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceRangeDto {
        private String label; // 표시 텍스트 (예: "10만원 이하")
        private String value; // 값 (예: "under_100000")
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryDto {
        private Long id;
        private String value;
        private String label;
        private List<CategoryDto> subCategories;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BrandDto {
        private Long id;
        private String value;
        private String label;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CollectionDto {
        private Long id;
        private String value;
        private String label;
    }
}