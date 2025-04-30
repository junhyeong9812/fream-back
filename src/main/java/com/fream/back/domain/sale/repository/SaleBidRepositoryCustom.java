package com.fream.back.domain.sale.repository;

import com.fream.back.domain.sale.dto.SaleBidResponseDto;
import com.fream.back.domain.sale.dto.SaleBidStatusCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 판매 입찰 레포지토리 커스텀 인터페이스
 * 커스텀 쿼리 메서드를 정의합니다.
 */
public interface SaleBidRepositoryCustom {

    /**
     * 필터 조건에 맞는 판매 입찰 목록 조회
     *
     * @param email 판매자 이메일
     * @param saleBidStatus 판매 입찰 상태 필터 (null인 경우 모든 상태 조회)
     * @param saleStatus 판매 상태 필터 (null인 경우 모든 상태 조회)
     * @param pageable 페이징 정보
     * @return 페이징된 판매 입찰 DTO 목록
     */
    Page<SaleBidResponseDto> findSaleBidsByFilters(
            String email,
            String saleBidStatus,
            String saleStatus,
            Pageable pageable
    );

    /**
     * 사용자별 판매 입찰 상태 카운트 조회
     *
     * @param email 판매자 이메일
     * @return 상태별 판매 입찰 개수 DTO
     */
    SaleBidStatusCountDto countSaleBidsByStatus(String email);

    /**
     * ID로 판매 입찰 상세 정보 조회
     *
     * @param saleBidId 판매 입찰 ID
     * @param email 판매자 이메일 (권한 확인용)
     * @return 판매 입찰 DTO (권한이 없거나 해당 ID의 판매 입찰이 없는 경우 null)
     */
    SaleBidResponseDto findSaleBidById(Long saleBidId, String email);
}