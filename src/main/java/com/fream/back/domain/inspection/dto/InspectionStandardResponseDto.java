package com.fream.back.domain.inspection.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InspectionStandardResponseDto {
    private Long id; // 검수 기준 ID
    private String category; // 검수 기준 카테고리
    private String content; // 검수 기준 내용
    private List<String> imageUrls; // 이미지 URL 목록
}
