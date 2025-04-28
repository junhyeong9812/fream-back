package com.fream.back.domain.inspection.dto;

import com.fream.back.domain.inspection.entity.InspectionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 검수 기준 생성 요청 DTO
 * - 유효성 검증 어노테이션 추가
 */
@Data
public class InspectionStandardCreateRequestDto {
    @NotNull(message = "검수 기준 카테고리는 필수입니다.")
    private InspectionCategory category; // 검수 기준 카테고리

    @NotBlank(message = "검수 기준 내용은 필수입니다.")
    private String content; // 검수 기준 내용

    private List<MultipartFile> files; // 업로드된 파일
}