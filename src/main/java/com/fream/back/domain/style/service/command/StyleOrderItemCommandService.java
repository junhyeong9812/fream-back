package com.fream.back.domain.style.service.command;

import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.service.query.OrderItemQueryService;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleOrderItem;
import com.fream.back.domain.style.repository.StyleOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StyleOrderItemCommandService {

    private final StyleOrderItemRepository styleOrderItemRepository;
    private final OrderItemQueryService orderItemQueryService;

    public StyleOrderItem createStyleOrderItem(Long orderItemId, Style style) {
        // 1. OrderItem 조회
        OrderItem orderItem = orderItemQueryService.findById(orderItemId);

        // 2. StyleOrderItem 생성
        StyleOrderItem styleOrderItem = StyleOrderItem.builder()
                .style(style)
                .orderItem(orderItem)
                .build();

        // 3. 저장
        return styleOrderItemRepository.save(styleOrderItem);
    }
}
