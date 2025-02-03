package com.fream.back.domain.notification.dto;

import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@Builder
public class NotificationRequestDTO {
    private NotificationCategory category;
    private NotificationType type;
    private String message;
}
