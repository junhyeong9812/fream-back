package com.fream.back.domain.faq.dto;

import com.fream.back.domain.faq.entity.FAQCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
public class FAQUpdateRequestDto {
    @NotNull(message = "카테고리는 필수입니다.")
    private FAQCategory category; // FAQ 카테고리

    @NotBlank(message = "질문은 필수입니다.")
    @Size(max = 100, message = "질문은 100자 이내로 작성해주세요.")
    private String question; // 질문

    @NotBlank(message = "답변은 필수입니다.")
    private String answer; // 답변

    private List<String> retainedImageUrls = new ArrayList<>(); // 유지할 이미지 URL
    private List<MultipartFile> newFiles = new ArrayList<>(); // 새로 추가된 파일
}