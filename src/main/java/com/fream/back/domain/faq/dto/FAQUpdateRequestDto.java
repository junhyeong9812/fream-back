package com.fream.back.domain.faq.dto;

import com.fream.back.domain.faq.entity.FAQCategory;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FAQUpdateRequestDto {
    private FAQCategory category; // FAQ 카테고리
    private String question; // 질문
    private String answer; // 답변
    private List<String> existingImageUrls; // 기존 이미지 URL
    private List<MultipartFile> newFiles; // 새로 추가된 파일
}
