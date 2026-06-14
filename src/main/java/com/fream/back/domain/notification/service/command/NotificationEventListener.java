package com.fream.back.domain.notification.service.command;

import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.event.NotificationBroadcastRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 알림 요청 이벤트 수신기. 발신 도메인의 직접 서비스 주입을 이벤트 수신으로 대체한다.
 *
 * <p>현재는 동기 {@code @EventListener}(기존 동기 호출 동작 보존). 트랜잭션 커밋 후 비동기/내구성이
 * 필요하면 {@code @ApplicationModuleListener}(+ spring-modulith-starter-jpa 이벤트 레지스트리)로 승격한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCommandService notificationCommandService;

    @EventListener
    public void onBroadcastRequested(NotificationBroadcastRequestedEvent event) {
        NotificationRequestDTO requestDTO = toRequestDTO(event);
        notificationCommandService.createNotificationForAll(requestDTO);
        log.debug("전체 알림 이벤트 처리: category={}, type={}", event.category(), event.type());
    }

    /**
     * 이벤트의 String category/type을 notification enum으로 매핑한다(이벤트 계약을 enum 결합 없이 유지).
     */
    public NotificationRequestDTO toRequestDTO(NotificationBroadcastRequestedEvent event) {
        return NotificationRequestDTO.builder()
                .category(NotificationCategory.valueOf(event.category()))
                .type(NotificationType.valueOf(event.type()))
                .message(event.message())
                .build();
    }
}
