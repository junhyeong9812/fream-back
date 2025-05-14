package com.fream.back.domain.inquiry.entity;

/**
 * 1대1 문의 상태를 나타내는 Enum
 */
public enum InquiryStatus {
    REQUESTED("질문요청"), // 사용자가 문의 등록 시 초기 상태
    CONFIRMED("질문확인"), // 관리자가 문의를 확인한 상태
    ANSWERED("답변완료"); // 관리자가 답변을 완료한 상태

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}