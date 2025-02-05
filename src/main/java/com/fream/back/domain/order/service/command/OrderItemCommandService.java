package com.fream.back.domain.order.service.command;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.repository.OrderItemRepository;
import com.fream.back.domain.product.entity.ProductSize;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderItemCommandService {

    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderItem createOrderItem(Order order, ProductSize productSize, int price) {
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productSize(productSize)
                .quantity(1) // 주문 수량은 기본 1
                .price(price)
                .build();

        return orderItemRepository.save(orderItem);
    }
}
