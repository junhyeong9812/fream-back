package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 검수 기준 리포지토리
 * - N+1 문제 해결을 위한 조인 쿼리 추가
 */
public interface InspectionStandardRepository extends JpaRepository<InspectionStandard, Long>, InspectionStandardRepositoryCustom {

    /**
     * 단일 조회: InspectionStandard와 InspectionStandardImage를 조인하여 조회
     * - N+1 문제 해결
     */
    @Query("""
        SELECT DISTINCT is 
        FROM InspectionStandard is 
        LEFT JOIN FETCH InspectionStandardImage isi ON isi.inspectionStandard.id = is.id
        WHERE is.id = :id
    """)
    Optional<InspectionStandard> findWithImagesById(@Param("id") Long id);

    /**
     * 페이징 처리: InspectionStandard와 InspectionStandardImage를 조인하여 조회
     * - N+1 문제 해결
     */
    @Query(value = """
        SELECT DISTINCT is 
        FROM InspectionStandard is 
        LEFT JOIN FETCH InspectionStandardImage isi ON isi.inspectionStandard.id = is.id
    """,
            countQuery = "SELECT COUNT(DISTINCT is) FROM InspectionStandard is")
    Page<InspectionStandard> findAllWithPaging(Pageable pageable);

    /**
     * 카테고리별 검수 기준 조회 (페이징)
     * - QueryDSL 구현체로 대체 가능
     */
    @Query(value = """
        SELECT DISTINCT is 
        FROM InspectionStandard is 
        LEFT JOIN FETCH InspectionStandardImage isi ON isi.inspectionStandard.id = is.id
        WHERE is.category = :category
    """,
            countQuery = "SELECT COUNT(DISTINCT is) FROM InspectionStandard is WHERE is.category = :category")
    Page<InspectionStandard> findByCategory(@Param("category") InspectionCategory category, Pageable pageable);
}