package com.fream.back.domain.notice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공지사항 카테고리 Enum
 */
@Getter
@RequiredArgsConstructor
public enum NoticeCategory {
    ANNOUNCEMENT("공지사항"),
    EVENT("이벤트"),
    SERVICE("서비스 안내"),
    OTHERS("기타");

    private final String description;  // 카테고리 설명

    /**
     * 문자열 기반으로 카테고리 조회
     *
     * @param name 카테고리명
     * @return NoticeCategory 객체
     */
    public static NoticeCategory fromString(String name) {
        try {
            return NoticeCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return OTHERS;  // 유효하지 않을 경우 기타로 설정
        }
    }

    /**
     * 카테고리 설명 반환
     */
    @Override
    public String toString() {
        return this.description;
    }
}