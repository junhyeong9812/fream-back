package com.fream.back.domain.product.service.filter;

import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.dto.CategoryResponseDto;
import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.dto.FilterDataResponseDto;
import com.fream.back.domain.product.entity.enumType.ColorType;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.entity.enumType.SizeType;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.domain.product.service.category.CategoryQueryService;
import com.fream.back.domain.product.service.collection.CollectionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FilterService {

    private final BrandQueryService brandQueryService;
    private final CategoryQueryService categoryQueryService;
    private final CollectionQueryService collectionQueryService;

    public FilterDataResponseDto getAllFilterData() {
        // 사이즈 데이터 가공
        Map<String, List<String>> sizes = new HashMap<>();
        for (SizeType sizeType : SizeType.values()) {
            sizes.put(sizeType.name(), Arrays.asList(sizeType.getSizes()));
        }

        // 성별 데이터 가공
        List<FilterDataResponseDto.GenderDto> genders = Arrays.stream(GenderType.values())
                .map(gender -> FilterDataResponseDto.GenderDto.builder()
                        .value(gender.name())
                        .label(getGenderLabel(gender))
                        .build())
                .collect(Collectors.toList());

        // 색상 데이터 가공
        List<FilterDataResponseDto.ColorDto> colors = Arrays.stream(ColorType.values())
                .map(color -> FilterDataResponseDto.ColorDto.builder()
                        .key(color.name())
                        .name(color.getDisplayName())
                        .build())
                .collect(Collectors.toList());

        // 혜택/할인 데이터
        List<FilterDataResponseDto.DiscountGroupDto> discounts = getDiscountData();

        // 가격대 데이터
        List<FilterDataResponseDto.PriceRangeDto> priceRanges = getPriceRangeData();

        // 카테고리 데이터 가공
        // 실질적인 메인 카테고리 구성
        List<CategoryResponseDto> effectiveMainCategories = new ArrayList<>();

        // 모든 메인 카테고리 가져오기
        List<CategoryResponseDto> mainCategories = categoryQueryService.findAllMainCategories();

        // Clothing의 서브카테고리(Tops 등) 찾기
        mainCategories.stream()
                .filter(c -> "Clothing".equals(c.getName()))
                .findFirst()
                .ifPresent(clothing -> {
                    // 서비스 레이어를 통해 Clothing의 하위 카테고리 조회
                    List<CategoryResponseDto> clothingSubCategories =
                            categoryQueryService.findSubCategoriesByMainCategory(clothing.getName());
                    effectiveMainCategories.addAll(clothingSubCategories);
                });

        // Shoes는 그대로 메인 카테고리로 추가
        mainCategories.stream()
                .filter(c -> "Shoes".equals(c.getName()))
                .forEach(effectiveMainCategories::add);

        // 실질적인 메인 카테고리 처리
        List<FilterDataResponseDto.CategoryDto> categories = processCategoryData(effectiveMainCategories);

        // 브랜드 데이터 가공
        List<BrandResponseDto> brandList = brandQueryService.findAllBrands();
        List<FilterDataResponseDto.BrandDto> brands = brandList.stream()
                .map(brand -> FilterDataResponseDto.BrandDto.builder()
                        .id(brand.getId())
                        .value(brand.getName())
                        .label(brand.getName())
                        .build())
                .collect(Collectors.toList());

        // 컬렉션 데이터 가공
        List<CollectionResponseDto> collectionList = collectionQueryService.findAllCollections();
        List<FilterDataResponseDto.CollectionDto> collections = collectionList.stream()
                .map(collection -> FilterDataResponseDto.CollectionDto.builder()
                        .id(collection.getId())
                        .value(collection.getName())
                        .label(collection.getName())
                        .build())
                .collect(Collectors.toList());

        return FilterDataResponseDto.builder()
                .sizes(sizes)
                .genders(genders)
                .colors(colors)
                .discounts(discounts)
                .priceRanges(priceRanges)
                .categories(categories)
                .brands(brands)
                .collections(collections)
                .build();
    }

    private String getGenderLabel(GenderType gender) {
        switch (gender) {
            case MALE: return "남성";
            case FEMALE: return "여성";
            case KIDS: return "키즈";
            case UNISEX: return "공용";
            default: return gender.name();
        }
    }

    private List<FilterDataResponseDto.DiscountGroupDto> getDiscountData() {
        List<FilterDataResponseDto.DiscountGroupDto> discounts = new ArrayList<>();

        // 혜택 그룹
        discounts.add(FilterDataResponseDto.DiscountGroupDto.builder()
                .title("혜택")
                .options(Arrays.asList("무료배송", "할인", "정가이하"))
                .build());

        // 할인율 그룹
        discounts.add(FilterDataResponseDto.DiscountGroupDto.builder()
                .title("할인율")
                .options(Arrays.asList("30% 이하", "30%~50%", "50% 이상"))
                .build());

        return discounts;
    }

    private List<FilterDataResponseDto.PriceRangeDto> getPriceRangeData() {
        return Arrays.asList(
                createPriceRange("10만원 이하", "under_100000"),
                createPriceRange("10만원대", "100000_200000"),
                createPriceRange("20만원대", "200000_300000"),
                createPriceRange("30만원대", "300000_400000"),
                createPriceRange("30~50만원", "300000_500000"),
                createPriceRange("50~100만원", "500000_1000000"),
                createPriceRange("100~500만원", "1000000_5000000"),
                createPriceRange("500만원 이상", "over_5000000")
        );
    }

    private FilterDataResponseDto.PriceRangeDto createPriceRange(String label, String value) {
        return FilterDataResponseDto.PriceRangeDto.builder()
                .label(label)
                .value(value)
                .build();
    }

    private List<FilterDataResponseDto.CategoryDto> processCategoryData(List<CategoryResponseDto> categories) {
        return categories.stream()
                .map(category -> {
                    List<CategoryResponseDto> subCategories;

                    // 이미 effectiveMainCategories에는 실질적인 메인 카테고리만 포함됨
                    // Tops(Clothing의 하위) 또는 Shoes(실제 메인)
                    subCategories = categoryQueryService.findSubCategoriesByParentId(category.getId());

                    return FilterDataResponseDto.CategoryDto.builder()
                            .id(category.getId())
                            .value(category.getName())
                            .label(category.getName())
                            .subCategories(subCategories.isEmpty() ? null :
                                    // 서브 카테고리는 더 이상 재귀 처리하지 않음
                                    subCategories.stream()
                                            .map(sub -> FilterDataResponseDto.CategoryDto.builder()
                                                    .id(sub.getId())
                                                    .value(sub.getName())
                                                    .label(sub.getName())
                                                    .subCategories(null) // 최대 2단계만 표시
                                                    .build())
                                            .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }
}