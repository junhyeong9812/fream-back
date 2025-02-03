package com.fream.back.domain.order.repository;

import com.fream.back.domain.order.dto.OrderBidResponseDto;
import com.fream.back.domain.order.dto.OrderBidStatusCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface OrderBidRepositoryCustom {
    Page<OrderBidResponseDto> findOrderBidsByFilters(String email, String bidStatus, String orderStatus, Pageable pageable);
    OrderBidStatusCountDto countOrderBidsByStatus(String email);
    OrderBidResponseDto findOrderBidById(Long orderBidId,String email); // 단일 조회 메서드 추가
    Map<Long, Long> tradeCountByColorIds(List<Long> colorIds);
}
