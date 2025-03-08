package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileStyleResponseDto {
    private Long id;
    private String mediaUrl;
    private Long likeCount;
    private Boolean liked = false; // 좋아요 상태 필드 추가

    public ProfileStyleResponseDto(Long id, String mediaUrl, Long likeCount, Long commentCount) {
        this.id = id;
        this.mediaUrl = mediaUrl;
        this.likeCount = likeCount;
        this.liked = false; // 기본값 설정
    }
}