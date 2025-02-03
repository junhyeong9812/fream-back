package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InspectionStandardRepository extends JpaRepository<InspectionStandard, Long>, InspectionStandardRepositoryCustom {
    // 단일 조회: InspectionStandard와 InspectionStandardImage를 조인하여 조회
    @Query("""
        SELECT DISTINCT is 
        FROM InspectionStandard is 
        LEFT JOIN InspectionStandardImage isi ON isi.inspectionStandard.id = is.id
        WHERE is.id = :id
    """)
    Optional<InspectionStandard> findWithImagesById(@Param("id") Long id);

    // 페이징 처리: InspectionStandard와 InspectionStandardImage를 조인하여 조회
    @Query("""
        SELECT DISTINCT is 
        FROM InspectionStandard is 
        LEFT JOIN InspectionStandardImage isi ON isi.inspectionStandard.id = is.id
    """)
    Page<InspectionStandard> findAllWithPaging(Pageable pageable);

    // JPQL로 카테고리별 검수 기준 조회
    Page<InspectionStandard> findByCategory(InspectionCategory category, Pageable pageable);


}
