package com.fream.back.domain.order.service.query;

import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemQueryService {

    private final OrderItemRepository orderItemRepository;

    public OrderItem findById(Long id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 OrderItem을 찾을 수 없습니다: " + id));
    }
}

