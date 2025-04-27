package com.fream.back.domain.faq.dto;

import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class FAQResponseDto {
    private Long id; // FAQ ID
    private String category; // FAQ 카테고리
    private String question; // 질문
    private String answer; // 답변
    private List<String> imageUrls; // 이미지 URL
    private LocalDateTime createdDate; // 생성일
    private LocalDateTime modifiedDate; // 수정일

    // FAQ 엔티티와 이미지 목록에서 ResponseDto 생성
    public static FAQResponseDto from(FAQ faq, List<FAQImage> images) {
        List<String> urls = images.stream()
                .map(FAQImage::getFullImageUrl)
                .collect(Collectors.toList());

        return FAQResponseDto.builder()
                .id(faq.getId())
                .category(faq.getCategory().name())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .imageUrls(urls)
                .createdDate(faq.getCreatedDate())
                .modifiedDate(faq.getModifiedDate())
                .build();
    }
}