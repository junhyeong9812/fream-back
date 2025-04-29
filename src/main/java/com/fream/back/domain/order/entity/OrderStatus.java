package com.fream.back.domain.order.entity;

import java.util.*;

/**
 * 주문 상태를 정의하는 Enum
 */
public enum OrderStatus {
    PENDING_PAYMENT, // 결제 대기
    PAYMENT_COMPLETED, // 결제 완료
    PREPARING, // 상품 준비 중
    IN_WAREHOUSE, // 창고 보관 중
    SHIPMENT_STARTED, // 배송 시작
    IN_TRANSIT, // 배송 중
    COMPLETED, // 배송 완료
    REFUND_REQUESTED, // 환불 대기
    REFUNDED; // 환불 완료

    // 각 상태에서 전이 가능한 다음 상태들을 정의
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        // 상태 전이 규칙 초기화
        ALLOWED_TRANSITIONS.put(PENDING_PAYMENT, Set.of(PAYMENT_COMPLETED, COMPLETED, IN_WAREHOUSE));
        ALLOWED_TRANSITIONS.put(PAYMENT_COMPLETED, Set.of(PREPARING, REFUND_REQUESTED, IN_WAREHOUSE));
        ALLOWED_TRANSITIONS.put(PREPARING, Set.of(IN_WAREHOUSE, SHIPMENT_STARTED));
        ALLOWED_TRANSITIONS.put(IN_WAREHOUSE, Set.of(SHIPMENT_STARTED, COMPLETED));
        ALLOWED_TRANSITIONS.put(SHIPMENT_STARTED, Set.of(IN_TRANSIT));
        ALLOWED_TRANSITIONS.put(IN_TRANSIT, Set.of(COMPLETED));
        ALLOWED_TRANSITIONS.put(REFUND_REQUESTED, Set.of(REFUNDED));
        ALLOWED_TRANSITIONS.put(COMPLETED, Collections.emptySet());
        ALLOWED_TRANSITIONS.put(REFUNDED, Collections.emptySet());
    }

    /**
     * 현재 상태에서 새 상태로 전이가 가능한지 확인합니다.
     *
     * @param newStatus 전이하려는 새 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        Set<OrderStatus> allowedNextStates = ALLOWED_TRANSITIONS.get(this);
        return allowedNextStates != null && allowedNextStates.contains(newStatus);
    }

    /**
     * 현재 상태에서 전이 가능한 모든 상태를 반환합니다.
     *
     * @return 전이 가능한 상태 집합
     */
    public Set<OrderStatus> getAllowedNextStates() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(this, Collections.emptySet()));
    }
}