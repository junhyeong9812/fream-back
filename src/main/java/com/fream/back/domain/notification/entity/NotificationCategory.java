package com.fream.back.domain.notification.entity;

public enum NotificationCategory {

    SHOPPING("쇼핑 관련 알림"),
    STYLE("스타일 관련 알림");

    private final String description;

    NotificationCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}