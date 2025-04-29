package com.fream.back.domain.order.repository;

import com.fream.back.domain.order.dto.OrderBidResponseDto;
import com.fream.back.domain.order.dto.OrderBidStatusCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 주문 입찰 저장소 커스텀 인터페이스
 */
public interface OrderBidRepositoryCustom {
    /**
     * 필터링된 주문 입찰 목록을 조회합니다.
     *
     * @param email 사용자 이메일
     * @param bidStatus 입찰 상태 필터
     * @param orderStatus 주문 상태 필터
     * @param pageable 페이징 정보
     * @return 페이징된 주문 입찰 목록
     */
    Page<OrderBidResponseDto> findOrderBidsByFilters(String email, String bidStatus, String orderStatus, Pageable pageable);

    /**
     * 사용자별 주문 입찰 상태 개수를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 상태별 주문 입찰 개수
     */
    OrderBidStatusCountDto countOrderBidsByStatus(String email);

    /**
     * ID와 이메일로 단일 주문 입찰을 조회합니다.
     *
     * @param orderBidId 주문 입찰 ID
     * @param email 사용자 이메일
     * @return 주문 입찰 응답 DTO
     */
    OrderBidResponseDto findOrderBidById(Long orderBidId, String email);

    /**
     * 색상 ID 목록으로 거래 완료된 입찰 수를 조회합니다.
     *
     * @param colorIds 색상 ID 목록
     * @return 색상 ID별 거래 완료 수 맵
     */
    Map<Long, Long> tradeCountByColorIds(List<Long> colorIds);
}