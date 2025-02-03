package com.fream.back.domain.notification.entity;

public enum NotificationType {
    TRADE(NotificationCategory.SHOPPING),
    BID(NotificationCategory.SHOPPING),
    STORAGE(NotificationCategory.SHOPPING),
    FAVORITE(NotificationCategory.SHOPPING),
    BENEFIT(NotificationCategory.SHOPPING),
    ANNOUNCEMENT(NotificationCategory.SHOPPING),
    LIKE(NotificationCategory.STYLE),
    COMMENT(NotificationCategory.STYLE),
    FOLLOW(NotificationCategory.STYLE);

    private final NotificationCategory category;

    NotificationType(NotificationCategory category) {
        this.category = category;
    }

    public NotificationCategory getCategory() {
        return category;
    }
}