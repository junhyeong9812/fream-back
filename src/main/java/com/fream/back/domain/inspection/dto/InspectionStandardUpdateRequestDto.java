package com.fream.back.domain.inspection.dto;

import com.fream.back.domain.inspection.entity.InspectionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 검수 기준 수정 요청 DTO
 * - 유효성 검증 어노테이션 추가
 * - existingImageUrls에 기본값 설정으로 null 방지
 */
@Data
public class InspectionStandardUpdateRequestDto {
    @NotNull(message = "검수 기준 카테고리는 필수입니다.")
    private InspectionCategory category; // 검수 기준 카테고리

    @NotBlank(message = "검수 기준 내용은 필수입니다.")
    private String content; // 검수 기준 내용

    private List<String> existingImageUrls = new ArrayList<>(); // 기존 이미지 URL (null 방지)
    private List<MultipartFile> newFiles; // 새로 추가된 파일
}