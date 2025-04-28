package com.fream.back.domain.notification.entity;

public enum NotificationType {

    // SHOPPING 카테고리 알림 유형
    TRADE(NotificationCategory.SHOPPING, "거래 관련 알림"),
    BID(NotificationCategory.SHOPPING, "입찰 관련 알림"),
    STORAGE(NotificationCategory.SHOPPING, "보관 관련 알림"),
    FAVORITE(NotificationCategory.SHOPPING, "관심 상품 관련 알림"),
    BENEFIT(NotificationCategory.SHOPPING, "혜택 관련 알림"),
    ANNOUNCEMENT(NotificationCategory.SHOPPING, "공지사항 알림"),

    // STYLE 카테고리 알림 유형
    LIKE(NotificationCategory.STYLE, "좋아요 관련 알림"),
    COMMENT(NotificationCategory.STYLE, "댓글 관련 알림"),
    FOLLOW(NotificationCategory.STYLE, "팔로우 관련 알림");

    private final NotificationCategory category;
    private final String description;

    NotificationType(NotificationCategory category, String description) {
        this.category = category;
        this.description = description;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 특정 카테고리에 속하는 모든 알림 유형을 반환
     *
     * @param category 검색할 카테고리
     * @return 카테고리에 속하는 알림 유형 배열
     */
    public static NotificationType[] getTypesByCategory(NotificationCategory category) {
        if (category == null) {
            return values();
        }

        return java.util.Arrays.stream(values())
                .filter(type -> type.getCategory() == category)
                .toArray(NotificationType[]::new);
    }
}