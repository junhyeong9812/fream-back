package com.fream.back.domain.inquiry.repository;

import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 1대1 문의 리포지토리
 */
public interface InquiryRepository extends JpaRepository<Inquiry, Long>, InquiryRepositoryCustom {

    // 특정 사용자의 모든 문의 조회
    Page<Inquiry> findByUserIdOrderByCreatedDateDesc(Long userId, Pageable pageable);

    // 특정 상태의 문의 조회
    Page<Inquiry> findByStatus(InquiryStatus status, Pageable pageable);

    // 특정 카테고리의 문의 조회
    Page<Inquiry> findByCategory(InquiryCategory category, Pageable pageable);

    // 특정 기간 내의 문의 조회
    Page<Inquiry> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 키워드가 제목에 포함된 문의 조회
    Page<Inquiry> findByTitleContaining(String keyword, Pageable pageable);

    // 특정 사용자의 특정 상태 문의 조회
    Page<Inquiry> findByUserIdAndStatus(Long userId, InquiryStatus status, Pageable pageable);

    // 답변이 완료되지 않은 문의 조회
    @Query("SELECT i FROM Inquiry i WHERE i.status <> :answeredStatus ORDER BY i.createdDate ASC")
    Page<Inquiry> findAllUnansweredInquiries(@Param("answeredStatus") InquiryStatus answeredStatus, Pageable pageable);

    // 특정 사용자의 총 문의 수 조회
    long countByUserId(Long userId);

    // 특정 상태의 문의 수 조회
    long countByStatus(InquiryStatus status);

    // 특정 기간 내의 문의 수 조회
    long countByCreatedDateBetween(LocalDateTime start, LocalDateTime end);
}