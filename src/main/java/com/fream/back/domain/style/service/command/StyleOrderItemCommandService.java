package com.fream.back.domain.style.service.command;

import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.service.query.OrderItemQueryService;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleOrderItem;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleOrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StyleOrderItemCommandService {

    private final StyleOrderItemRepository styleOrderItemRepository;
    private final OrderItemQueryService orderItemQueryService;

    /**
     * 스타일과 주문 아이템을 연결하는 StyleOrderItem 생성
     *
     * @param orderItemId 주문 아이템 ID
     * @param style 스타일 엔티티
     * @return 생성된 StyleOrderItem 엔티티
     * @throws StyleException 스타일-주문 아이템 연결 실패 시
     */
    public StyleOrderItem createStyleOrderItem(Long orderItemId, Style style) {
        log.debug("스타일-주문 아이템 연결 시작: styleId={}, orderItemId={}",
                style.getId(), orderItemId);

        // 입력값 검증
        if (orderItemId == null) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "주문 아이템 ID가 필요합니다.");
        }

        if (style == null) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 정보가 필요합니다.");
        }

        try {
            // 1. OrderItem 조회
            OrderItem orderItem = orderItemQueryService.findById(orderItemId);
            log.debug("주문 아이템 조회 성공: orderItemId={}", orderItemId);

            // 유효성 검사: 해당 주문 아이템이 이미 다른 스타일에 연결되어 있는지 확인 가능하면 좋을 것 같음
            // 현재 구현에선 하나의 주문 아이템이 여러 스타일에 연결될 수 있음

            // 2. StyleOrderItem 생성
            StyleOrderItem styleOrderItem = StyleOrderItem.builder()
                    .style(style)
                    .orderItem(orderItem)
                    .build();

            // 3. 양방향 연관관계 설정
            styleOrderItem.assignStyle(style);
            styleOrderItem.assignOrderItem(orderItem);

            // 4. 저장
            StyleOrderItem savedLink = styleOrderItemRepository.save(styleOrderItem);
            log.info("스타일-주문 아이템 연결 완료: linkId={}, styleId={}, orderItemId={}",
                    savedLink.getId(), style.getId(), orderItemId);

            return savedLink;

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일-주문 아이템 연결 중 오류 발생: styleId={}, orderItemId={}",
                    style.getId(), orderItemId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일-주문 아이템 연결 중 오류가 발생했습니다.", e);
        }
    }
}