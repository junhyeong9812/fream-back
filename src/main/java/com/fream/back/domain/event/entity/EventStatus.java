package com.fream.back.domain.event.entity;

/**
 * 이벤트 상태를 정의하는 열거형
 */
public enum EventStatus {

    /**
     * 예정된 이벤트 (아직 시작되지 않음)
     */
    UPCOMING("예정"),

    /**
     * 진행 중인 이벤트 (현재 활성화됨)
     */
    ACTIVE("진행 중"),

    /**
     * 종료된 이벤트 (이미 종료됨)
     */
    ENDED("종료");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}