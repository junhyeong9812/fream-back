package com.fream.back.domain.product.elasticsearch.controller;

import com.fream.back.domain.product.dto.ProductSearchDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.elasticsearch.index.ProductColorIndex;
import com.fream.back.domain.product.elasticsearch.service.ProductColorSearchService;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.product.service.product.ProductQueryService;
import com.fream.back.global.dto.commonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/es/products")
public class ProductColorSearchController {

    private final ProductColorSearchService productColorSearchService;
    private final ProductQueryService productQueryService;
    // 기존 ProductSearchDto와 유사한 DTO를 @ModelAttribute로 받는다

    @GetMapping
    public ResponseEntity<commonDto.PageDto<ProductSearchResponseDto>> esSearchProducts(
            @ModelAttribute ProductSearchDto searchDto,
            @ModelAttribute SortOption sortOption,
            Pageable pageable
    ) {
        Page<ProductSearchResponseDto> resultPage = productColorSearchService.searchToDto(
                searchDto.getKeyword(),
                searchDto.getCategoryIds(),
                convertGenders(searchDto.getGenders()),
                searchDto.getBrandIds(),
                searchDto.getCollectionIds(),
                searchDto.getColors(),
                searchDto.getSizes(),
                searchDto.getMinPrice(),
                searchDto.getMaxPrice(),
                sortOption,
                pageable
        );
        commonDto.PageDto<ProductSearchResponseDto> responseDto = toPageDto(resultPage);

        return ResponseEntity.ok(responseDto);
    }
    private <T> commonDto.PageDto<T> toPageDto(Page<T> pageResult) {
        return new commonDto.PageDto<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),  // 현재 페이지 index(0-based)
                pageResult.getSize()
        );
    }

//    @GetMapping
//    public ResponseEntity<Page<ProductColorIndex>> esSearchProducts(
//            @ModelAttribute ProductSearchDto searchDto,
//            @ModelAttribute SortOption sortOption,  // <-- 쿼리 파라미터 바인딩 예: ?field=price&order=asc
//            Pageable pageable
//    ) {
//        // (1) 필터링 조건을 받아서 ElasticSearch 검색
//        Page<ProductColorIndex> esPage = productColorSearchService.search(
//                searchDto.getKeyword(),
//                searchDto.getCategoryIds(),
//                convertGenders(searchDto.getGenders()),
//                searchDto.getBrandIds(),
//                searchDto.getCollectionIds(),
//                searchDto.getColors(),
//                searchDto.getSizes(),
//                searchDto.getMinPrice(),
//                searchDto.getMaxPrice(),
//                sortOption,
//                pageable
//        );
//
//        // (2) colorId 목록만 추출
//        List<Long> colorIds = esPage.getContent().stream()
//                .map(ProductColorIndex::getColorId)
//                .distinct()
//                .toList();
//
//        // (3) DB에서 해당 colorIds 상세 조회
//        //     만약 DB 페이징/정렬이 필요 없다면, 단순히 colorIds를 IN query로 가져올 수도
//        //     혹은 DB 정렬/페이징을 추가로 하고 싶다면 DB에 별도 pageable을 넘길 수 있음
//        //     예: Page<ProductSearchResponseDto> result = productQueryService.searchProductsByColorIds(
//        //            colorIds, sortOption, pageable
//        //         );
//        // (4) 최종 리턴
//        return ResponseEntity.ok(esPage); // 또는 ResponseEntity.ok(result);
//    }

    private ProductSearchResponseDto toDto(ProductColorIndex idx) {
        return ProductSearchResponseDto.builder()
                .id(idx.getProductId())  // 또는 colorId / productId
                .name(idx.getProductName())
                .englishName(idx.getProductEnglishName())
                .releasePrice(idx.getReleasePrice())
                .thumbnailImageUrl("")  // 썸네일 URL은 DB 추가 조회 or 인덱스에 넣어도 됨
                .price(idx.getMinPrice())  // 최저 구매가
                .colorName(idx.getColorName())
                .colorId(idx.getColorId())
                .interestCount(idx.getInterestCount())
                .build();
    }

    private List<String> convertGenders(List<GenderType> genderTypes) {
        // "MALE", "FEMALE", "KIDS", "UNISEX" 문자열 목록으로 변환
        if (genderTypes == null) return Collections.emptyList();
        return genderTypes.stream().map(GenderType::name).collect(Collectors.toList());
    }
}
