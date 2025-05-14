package com.fream.back.domain.inquiry.dto;

import com.fream.back.domain.inquiry.entity.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 1대1 문의 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryUpdateRequestDto {

    @NotBlank(message = "문의 제목은 필수 입력 항목입니다.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "문의 내용은 필수 입력 항목입니다.")
    @Size(min = 10, message = "내용은 10자 이상 입력해주세요.")
    private String content;

    @NotNull(message = "문의 카테고리는 필수 선택 항목입니다.")
    private InquiryCategory category;

    @Builder.Default
    private boolean isPrivate = false;

    @Builder.Default
    private boolean pushNotification = true;

    // 새로 추가할 이미지 파일
    @Builder.Default
    private List<MultipartFile> newFiles = new ArrayList<>();

    // 유지할 기존 이미지 URL (삭제할 이미지는 이 목록에서 제외)
    @Builder.Default
    private List<String> retainedImageUrls = new ArrayList<>();
}