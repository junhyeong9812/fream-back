package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.repository.SortOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 이름 기반 상품 검색 DTO
 * 인덱스 최적화를 위해 이름으로 검색하는 DTO
 *
 * 기존 ID 기반 검색과 달리 브랜드명, 카테고리명, 컬렉션명으로 검색하여
 * DB 인덱스를 최대한 활용합니다.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProductSearchByNameDto {
    private String keyword;
    private List<String> categoryNames;    // ID 대신 이름으로 검색 (idx_category_name 활용)
    private List<GenderType> genders;
    private List<String> brandNames;       // ID 대신 이름으로 검색 (idx_brand_name 활용)
    private List<String> collectionNames;  // ID 대신 이름으로 검색 (idx_collection_name 활용)
    private List<String> colors;
    private List<String> sizes;
    private Integer minPrice;
    private Integer maxPrice;
    private SortOption sortOption;

    /**
     * 유효성 검증 로직
     */
    public void validate() {
        // 가격 범위 검증
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("최소 가격이 최대 가격보다 클 수 없습니다.");
        }

        // 가격 음수 검증
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("최소 가격은 0 이상이어야 합니다.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("최대 가격은 0 이상이어야 합니다.");
        }

        // 로깅
        log.info("이름 기반 검색 DTO 유효성 검증 - 키워드: {}", keyword);
        log.info("브랜드명: {}, 카테고리명: {}, 컬렉션명: {}", brandNames, categoryNames, collectionNames);
        log.info("가격 범위: {} ~ {}", minPrice, maxPrice);

        // 정렬 옵션 기본값 설정
        if (sortOption == null) {
            log.info("정렬 옵션이 null이므로 기본값(interestCount, desc) 설정");
            sortOption = new SortOption();
            sortOption.setField("interestCount");
            sortOption.setOrder("desc");
        }
    }
}