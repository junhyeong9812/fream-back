package com.fream.back.domain.order.service.query;

import com.fream.back.domain.order.dto.OrderBidResponseDto;
import com.fream.back.domain.order.dto.OrderBidStatusCountDto;
import com.fream.back.domain.order.entity.OrderBid;
import com.fream.back.domain.order.exception.InvalidOrderBidDataException;
import com.fream.back.domain.order.exception.OrderBidAccessDeniedException;
import com.fream.back.domain.order.exception.OrderBidNotFoundException;
import com.fream.back.domain.order.repository.OrderBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 주문 입찰 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderBidQueryService {

    private final OrderBidRepository orderBidRepository;

    /**
     * ID로 주문 입찰을 조회합니다.
     *
     * @param id 주문 입찰 ID
     * @return 주문 입찰 (Optional)
     * @throws InvalidOrderBidDataException 주문 입찰 ID가 유효하지 않은 경우
     */
    public Optional<OrderBid> findById(Long id) {
        if (id == null) {
            throw new InvalidOrderBidDataException("주문 입찰 ID가 없습니다.");
        }
        return orderBidRepository.findById(id);
    }

    /**
     * 주문 ID로 주문 입찰을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 입찰 (Optional)
     * @throws InvalidOrderBidDataException 주문 ID가 유효하지 않은 경우
     */
    public Optional<OrderBid> findByOrderId(Long orderId) {
        if (orderId == null) {
            throw new InvalidOrderBidDataException("주문 ID가 없습니다.");
        }
        return orderBidRepository.findByOrderId(orderId);
    }

    /**
     * 필터링된 주문 입찰 목록을 조회합니다.
     *
     * @param email 사용자 이메일
     * @param bidStatus 입찰 상태
     * @param orderStatus 주문 상태
     * @param pageable 페이징 정보
     * @return 주문 입찰 목록 (페이징)
     * @throws OrderBidAccessDeniedException 사용자 이메일이 유효하지 않을 경우
     * @throws InvalidOrderBidDataException 조회 중 오류가 발생한 경우
     */
    public Page<OrderBidResponseDto> getOrderBids(String email, String bidStatus, String orderStatus, Pageable pageable) {
        validateEmail(email);

        try {
            return orderBidRepository.findOrderBidsByFilters(email, bidStatus, orderStatus, pageable);
        } catch (Exception e) {
            log.error("주문 입찰 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new InvalidOrderBidDataException("주문 입찰 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자의 주문 입찰 상태별 개수를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 주문 입찰 상태별 개수
     * @throws OrderBidAccessDeniedException 사용자 이메일이 유효하지 않을 경우
     * @throws InvalidOrderBidDataException 조회 중 오류가 발생한 경우
     */
    public OrderBidStatusCountDto getOrderBidStatusCounts(String email) {
        validateEmail(email);

        try {
            return orderBidRepository.countOrderBidsByStatus(email);
        } catch (Exception e) {
            log.error("주문 입찰 상태별 개수 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new InvalidOrderBidDataException("주문 입찰 상태별 개수 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 입찰 상세 정보를 조회합니다.
     *
     * @param orderBidId 주문 입찰 ID
     * @param email 사용자 이메일
     * @return 주문 입찰 상세 정보 (Optional)
     * @throws OrderBidAccessDeniedException 사용자 이메일이 유효하지 않을 경우
     * @throws InvalidOrderBidDataException 주문 입찰 ID가 유효하지 않을 경우
     */
    public Optional<OrderBidResponseDto> getOrderBidDetail(Long orderBidId, String email) {
        if (orderBidId == null) {
            throw new InvalidOrderBidDataException("주문 입찰 ID가 없습니다.");
        }

        validateEmail(email);

        try {
            OrderBidResponseDto responseDto = orderBidRepository.findOrderBidById(orderBidId, email);
            return Optional.ofNullable(responseDto);
        } catch (Exception e) {
            log.error("주문 입찰 상세 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new InvalidOrderBidDataException("주문 입찰 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 이메일 유효성을 검사합니다.
     *
     * @param email 사용자 이메일
     * @throws OrderBidAccessDeniedException 이메일이 유효하지 않은 경우
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new OrderBidAccessDeniedException("사용자 이메일이 없습니다.");
        }
    }
}