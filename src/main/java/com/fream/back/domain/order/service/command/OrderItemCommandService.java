package com.fream.back.domain.order.service.command;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.exception.InvalidOrderDataException;
import com.fream.back.domain.order.exception.OrderItemCreationFailedException;
import com.fream.back.domain.order.exception.ProductSizeNotFoundException;
import com.fream.back.domain.order.repository.OrderItemRepository;
import com.fream.back.domain.product.entity.ProductSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemCommandService {

    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 항목을 생성합니다.
     *
     * @param order 주문 정보 (null 가능)
     * @param productSize 상품 사이즈 정보
     * @param price 가격
     * @return 생성된 주문 항목
     * @throws OrderItemCreationFailedException 주문 항목 생성 실패 시
     * @throws ProductSizeNotFoundException 상품 사이즈가 null인 경우
     * @throws InvalidOrderDataException 가격이 유효하지 않은 경우
     */
    @Transactional
    public OrderItem createOrderItem(Order order, ProductSize productSize, int price) {
        try {
            // 입력값 검증
            if (productSize == null) {
                throw new ProductSizeNotFoundException("상품 사이즈 정보가 없습니다.");
            }

            if (price <= 0) {
                throw new InvalidOrderDataException("주문 항목 가격은 0보다 커야 합니다. 현재 가격: " + price);
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productSize(productSize)
                    .quantity(1) // 주문 수량은 기본 1
                    .price(price)
                    .build();

            return orderItemRepository.save(orderItem);
        } catch (Exception e) {
            if (e instanceof ProductSizeNotFoundException || e instanceof InvalidOrderDataException) {
                throw e;
            }
            log.error("주문 항목 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderItemCreationFailedException("주문 항목 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}