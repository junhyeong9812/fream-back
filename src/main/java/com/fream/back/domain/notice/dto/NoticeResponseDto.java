package com.fream.back.domain.notice.dto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NoticeResponseDto {
    private Long id; // 공지사항 ID
    private String title; // 제목
    private String content; // 내용
    private String category; // 카테고리
    private LocalDateTime createdDate; // 생성일
    private LocalDateTime updatedDate; // 수정일
    private List<String> imageUrls; // 이미지 및 비디오 URL
}
