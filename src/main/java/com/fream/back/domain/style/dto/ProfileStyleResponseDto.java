package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStyleResponseDto {
    private Long id;
    private String mediaUrl; // 첫 번째 미디어 URL (썸네일)
    private Long likeCount;
    private Boolean liked;

    @Builder.Default
    private List<HashtagResponseDto> hashtags = new ArrayList<>(); // 해시태그 목록 추가

    // 기존 생성자 (호환성을 위해)
    public ProfileStyleResponseDto(Long id, String mediaUrl, Long likeCount) {
        this.id = id;
        this.mediaUrl = mediaUrl;
        this.likeCount = likeCount;
        this.liked = false; // 기본값 설정
        this.hashtags = new ArrayList<>(); // 빈 리스트로 초기화
    }
}