package com.fream.back.domain.inspection.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 검수 기준 응답 DTO
 * - 캐싱을 위해 Serializable 구현 추가
 * - BaseTimeEntity와 필드명 일치시킴 (createdDate, modifiedDate)
 */
@Getter
@Builder
public class InspectionStandardResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // 검수 기준 ID
    private String category; // 검수 기준 카테고리
    private String content; // 검수 기준 내용
    private List<String> imageUrls; // 이미지 URL 목록
    private LocalDateTime createdDate; // 생성일
    private LocalDateTime modifiedDate; // 수정일
}