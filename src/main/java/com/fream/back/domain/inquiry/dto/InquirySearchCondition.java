package com.fream.back.domain.inquiry.dto;

import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1대1 문의 검색 조건 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquirySearchCondition {

    private Long userId; // 사용자 ID (특정 사용자의 문의만 조회)
    private InquiryStatus status; // 문의 상태
    private InquiryCategory category; // 문의 카테고리
    private String keyword; // 검색 키워드 (제목/내용)
    private LocalDateTime startDate; // 시작 날짜
    private LocalDateTime endDate; // 종료 날짜
    private boolean isAdmin; // 관리자 여부 (관리자라면 비공개 문의도 조회 가능)

    // 기본 검색 조건 생성 (모든 사용자의 공개 문의)
    public static InquirySearchCondition defaultCondition() {
        return InquirySearchCondition.builder()
                .isAdmin(false)
                .build();
    }

    // 특정 사용자의 문의만 조회하는 조건 생성
    public static InquirySearchCondition forUser(Long userId) {
        return InquirySearchCondition.builder()
                .userId(userId)
                .isAdmin(false)
                .build();
    }

    // 관리자용 검색 조건 생성 (모든 문의 조회 가능)
    public static InquirySearchCondition forAdmin() {
        return InquirySearchCondition.builder()
                .isAdmin(true)
                .build();
    }
}