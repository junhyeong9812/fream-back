package com.fream.back.domain.notice.dto;

import com.fream.back.domain.notice.entity.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 공지사항 수정 요청 DTO
 */
@Data
@Builder
public class NoticeUpdateRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "카테고리는 필수입니다.")
    private NoticeCategory category;

    private List<String> existingImageUrls;  // 기존 이미지 URL

    private List<MultipartFile> newFiles;  // 새로 추가된 파일

    /**
     * 기존 이미지 URL 목록 반환 (null 안전)
     */
    public List<String> getExistingImageUrls() {
        return existingImageUrls != null ? existingImageUrls : new ArrayList<>();
    }

    /**
     * 새 파일 목록 반환 (null 안전)
     */
    public List<MultipartFile> getNewFiles() {
        return newFiles != null ? newFiles : new ArrayList<>();
    }
}