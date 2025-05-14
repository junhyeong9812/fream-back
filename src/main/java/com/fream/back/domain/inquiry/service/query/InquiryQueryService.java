package com.fream.back.domain.inquiry.service.query;

import com.fream.back.domain.inquiry.dto.InquiryResponseDto;
import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 1대1 문의 쿼리 서비스 인터페이스
 * 문의 조회 기능 정의
 */
public interface InquiryQueryService {

    /**
     * 문의 단건 조회
     *
     * @param inquiryId 문의 ID
     * @param userId 조회 요청 사용자 ID
     * @param isAdmin 관리자 여부
     * @return 문의 상세 정보
     */
    InquiryResponseDto getInquiry(Long inquiryId, Long userId, boolean isAdmin);

    /**
     * 문의 목록 조회 (조건별)
     *
     * @param condition 검색 조건
     * @param pageable 페이징 정보
     * @return 문의 목록 (페이징)
     */
    Page<InquirySearchResultDto> getInquiries(InquirySearchCondition condition, Pageable pageable);

    /**
     * 사용자의 문의 목록 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 문의 목록 (페이징)
     */
    Page<InquirySearchResultDto> getUserInquiries(Long userId, Pageable pageable);

    /**
     * 특정 상태의 문의 목록 조회
     *
     * @param status 문의 상태
     * @param pageable 페이징 정보
     * @return 특정 상태의 문의 목록 (페이징)
     */
    Page<InquirySearchResultDto> getInquiriesByStatus(InquiryStatus status, Pageable pageable);

    /**
     * 특정 카테고리의 문의 목록 조회
     *
     * @param category 문의 카테고리
     * @param pageable 페이징 정보
     * @return 특정 카테고리의 문의 목록 (페이징)
     */
    Page<InquirySearchResultDto> getInquiriesByCategory(InquiryCategory category, Pageable pageable);

    /**
     * 키워드로 문의 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     */
    Page<InquirySearchResultDto> searchInquiries(String keyword, Pageable pageable);

    /**
     * 답변 대기 중인 문의 목록 조회
     *
     * @param pageable 페이징 정보
     * @return 답변 대기 중인 문의 목록 (페이징)
     */
    Page<InquirySearchResultDto> getPendingInquiries(Pageable pageable);

    /**
     * 문의 통계 조회
     *
     * @return 문의 통계 정보
     */
    Object getInquiryStatistics();
}