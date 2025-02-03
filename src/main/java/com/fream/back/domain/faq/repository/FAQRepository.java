package com.fream.back.domain.faq.repository;


import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FAQRepository extends JpaRepository<FAQ, Long>, FAQRepositoryCustom  {
    // FAQ 단일 조회 (이미지 포함)
    @Query("SELECT DISTINCT f FROM FAQ f LEFT JOIN FAQImage i ON i.faq.id = f.id WHERE f.id = :id")
    Optional<FAQ> findWithImagesById(@Param("id") Long id);

    // 페이징 처리된 FAQ 목록 조회
    @Query("SELECT DISTINCT f FROM FAQ f LEFT JOIN FAQImage i ON i.faq.id = f.id")
    Page<FAQ> findAllWithPaging(Pageable pageable);

    // 카테고리별 FAQ 목록 조회
    Page<FAQ> findByCategory(FAQCategory category, Pageable pageable);
}
