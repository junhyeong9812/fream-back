package com.fream.back.domain.faq.dto;

import com.fream.back.domain.faq.entity.FAQCategory;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FAQCreateRequestDto {
    private FAQCategory category; // FAQ 카테고리
    private String question; // 질문
    private String answer; // 답변
    private List<MultipartFile> files; // 첨부 파일
}
