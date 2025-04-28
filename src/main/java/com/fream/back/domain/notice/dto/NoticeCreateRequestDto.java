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
 * 공지사항 생성 요청 DTO
 */
@Data
@Builder
public class NoticeCreateRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "카테고리는 필수입니다.")
    private NoticeCategory category;

    private List<MultipartFile> files;

    /**
     * 파일 목록 반환 (null 안전)
     */
    public List<MultipartFile> getFiles() {
        return files != null ? files : new ArrayList<>();
    }
}