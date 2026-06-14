package com.fream.back.domain.notification;

import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.event.NotificationBroadcastRequestedEvent;
import com.fream.back.domain.notification.service.command.NotificationEventListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 요청 이벤트 → NotificationRequestDTO 매핑 검증(String category/type → enum).
 */
class NotificationEventListenerTest {

    @Test
    void toRequestDTO_mapsStringContractToNotificationEnums() {
        NotificationEventListener listener = new NotificationEventListener(null);

        NotificationRequestDTO dto = listener.toRequestDTO(
                new NotificationBroadcastRequestedEvent("SHOPPING", "ANNOUNCEMENT", "새로운 공지사항: 제목"));

        assertThat(dto.getCategory()).isEqualTo(NotificationCategory.SHOPPING);
        assertThat(dto.getType()).isEqualTo(NotificationType.ANNOUNCEMENT);
        assertThat(dto.getMessage()).isEqualTo("새로운 공지사항: 제목");
    }
}
