package com.fream.back.domain.faq.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FAQResponseDto {
    private Long id; // FAQ ID
    private String category; // FAQ 카테고리
    private String question; // 질문
    private String answer; // 답변
    private List<String> imageUrls; // 이미지 URL
}
