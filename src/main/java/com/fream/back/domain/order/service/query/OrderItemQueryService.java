package com.fream.back.domain.order.service.query;

import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.exception.InvalidOrderDataException;
import com.fream.back.domain.order.exception.OrderItemNotFoundException;
import com.fream.back.domain.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderItemQueryService {

    private final OrderItemRepository orderItemRepository;

    /**
     * ID로 주문 항목을 조회합니다.
     *
     * @param id 주문 항목 ID
     * @return 주문 항목
     * @throws OrderItemNotFoundException 주문 항목을 찾을 수 없는 경우
     * @throws InvalidOrderDataException 주문 항목 ID가 유효하지 않은 경우
     */
    public OrderItem findById(Long id) {
        if (id == null) {
            throw new InvalidOrderDataException("주문 항목 ID가 없습니다.");
        }

        try {
            return orderItemRepository.findById(id)
                    .orElseThrow(() -> new OrderItemNotFoundException("해당 주문 항목을 찾을 수 없습니다(ID: " + id + ")"));
        } catch (Exception e) {
            if (e instanceof OrderItemNotFoundException) {
                throw e;
            }
            log.error("주문 항목 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new InvalidOrderDataException("주문 항목 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}