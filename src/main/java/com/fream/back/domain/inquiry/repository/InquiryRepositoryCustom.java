package com.fream.back.domain.inquiry.repository;

import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 1대1 문의 커스텀 리포지토리 인터페이스
 * QueryDSL을 활용한 복잡한 쿼리를 위한 인터페이스
 */
public interface InquiryRepositoryCustom {

    /**
     * 다양한 조건으로 문의 검색 (QueryDSL 사용)
     *
     * @param condition 검색 조건 DTO
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    Page<InquirySearchResultDto> searchInquiries(InquirySearchCondition condition, Pageable pageable);

    /**
     * 유저와 문의를 조인하여 유저 정보와 함께 문의 조회
     *
     * @param inquiryId 문의 ID
     * @return 사용자 정보를 포함한 문의 DTO
     */
    InquirySearchResultDto findInquiryWithUserDetails(Long inquiryId);

    /**
     * 답변되지 않은 문의 중 가장 오래된 순으로 조회
     *
     * @param pageable 페이징 정보
     * @return 미답변 문의 페이지
     */
    Page<InquirySearchResultDto> findUnansweredInquiriesOrderByOldest(Pageable pageable);

    /**
     * 키워드로 제목과 내용에서 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    Page<InquirySearchResultDto> findByTitleOrContentContaining(String keyword, Pageable pageable);

    /**
     * 대시보드용 통계 데이터 조회
     * - 최근 30일간 일별 문의 수
     * - 카테고리별 문의 수
     * - 상태별 문의 수
     */
    Object getInquiryStatistics();
}