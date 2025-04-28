package com.fream.back.domain.notification.dto;

import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class NotificationRequestDTO {

    @NotNull(message = "알림 카테고리는 필수입니다.")
    private NotificationCategory category;

    @NotNull(message = "알림 유형은 필수입니다.")
    private NotificationType type;

    @NotBlank(message = "알림 메시지는 필수입니다.")
    @Size(min = 1, max = 500, message = "알림 메시지는 1자 이상 500자 이하여야 합니다.")
    private String message;

    @Builder
    public NotificationRequestDTO(
            NotificationCategory category,
            NotificationType type,
            String message
    ) {
        this.category = category;
        this.type = type;
        this.message = message;
    }
}