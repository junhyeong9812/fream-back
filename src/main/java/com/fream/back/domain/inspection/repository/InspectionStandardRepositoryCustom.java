package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 검수 기준 리포지토리 커스텀 인터페이스
 * - QueryDSL을 활용한 복잡한 쿼리 처리
 */
public interface InspectionStandardRepositoryCustom {

    /**
     * 검수 기준 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    Page<InspectionStandard> searchStandards(String keyword, Pageable pageable);

    /**
     * 카테고리 및 키워드로 검수 기준 검색
     *
     * @param category 검색할 카테고리
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    Page<InspectionStandard> searchStandardsByCategoryAndKeyword(
            InspectionCategory category, String keyword, Pageable pageable);
}