package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.repository.SortOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProductSearchDto {
    private String keyword;
    private List<Long> categoryIds;
    private List<GenderType> genders;
    private List<Long> brandIds;
    private List<Long> collectionIds;
    private List<String> colors;
    private List<String> sizes;
    private Integer minPrice;
    private Integer maxPrice;
    private SortOption sortOption;

    // 유효성 검증 로직
    public void validate() {
        // 기존 검증 로직 유지
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("최소 가격이 최대 가격보다 클 수 없습니다.");
        }

        // 추가 검증 및 로깅
        log.info("검색 DTO 유효성 검증 - 키워드: {}", keyword);
        log.info("검색 DTO 유효성 검증 - 성별: {}", genders);
        log.info("검색 DTO 유효성 검증 - 가격 범위: {} ~ {}", minPrice, maxPrice);

        // 정렬 옵션 기본값 설정 (null인 경우)
        if (sortOption == null) {
            log.info("정렬 옵션이 null이므로 기본값(interestCount, desc) 설정");
            sortOption = new SortOption();
            sortOption.setField("interestCount");
            sortOption.setOrder("desc");
        }
    }
}