package com.fream.back.domain.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 1대1 문의 답변 작성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryAnswerRequestDto {

    @NotBlank(message = "답변 내용은 필수 입력 항목입니다.")
    @Size(min = 10, message = "답변 내용은 10자 이상 입력해주세요.")
    private String answer;

    @NotBlank(message = "답변자 정보는 필수 입력 항목입니다.")
    private String answeredBy;

    // 답변에 첨부할 이미지 파일
    @Builder.Default
    private List<MultipartFile> files = new ArrayList<>();
}