package com.fream.back.domain.notice.dto;

import com.fream.back.domain.notice.entity.NoticeCategory;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class NoticeUpdateRequestDto {
    private String title; // 제목
    private String content; // 내용
    private NoticeCategory category; // 카테고리
    private List<String> existingImageUrls; // 기존 이미지 URL
    private List<MultipartFile> newFiles; // 새로 추가된 파일
}
