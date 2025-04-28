package com.fream.back.domain.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fream.back.domain.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 공지사항 응답 DTO
 */
@Getter
@Builder
public class NoticeResponseDto {

    private Long id;  // 공지사항 ID

    private String title;  // 제목

    private String content;  // 내용

    private String category;  // 카테고리

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;  // 생성일

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedDate;  // 수정일

    private List<String> imageUrls;  // 이미지 및 비디오 URL 목록

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     *
     * @param notice 공지사항 엔티티
     * @param imageUrls 이미지 URL 목록
     * @return NoticeResponseDto 객체
     */
    public static NoticeResponseDto of(Notice notice, List<String> imageUrls) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory().name())
                .createdDate(notice.getCreatedDate())
                .modifiedDate(notice.getModifiedDate())
                .imageUrls(imageUrls != null ? imageUrls : new ArrayList<>())
                .build();
    }
}