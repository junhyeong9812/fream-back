package com.fream.back.domain.notification.event;

/**
 * 전체 사용자 알림 발송 요청 이벤트(notification 모듈 소유 공개 계약).
 *
 * <p>발신 도메인은 notification 내부(서비스/엔티티)에 의존하지 않고 이 이벤트를 발행한다.
 * category/type은 notification 내부 enum 결합을 피하기 위해 String으로 전달하며, 수신 리스너가 매핑한다.
 */
public record NotificationBroadcastRequestedEvent(
        String category,
        String type,
        String message
) {
}
