package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleResponseDto {
    private Long id;
    private Long profileId;
    private String profileName;
    private String profileImageUrl;
    private String content;
    private String mediaUrl; // 첫 번째 이미지 URL (썸네일)
    private Long viewCount;
    private Integer likeCount;
    private Boolean liked;
    private Boolean interested;
    private LocalDateTime createdDate; // BaseTimeEntity의 필드명에 맞춤
    private LocalDateTime modifiedDate; // BaseTimeEntity의 필드명에 맞춤

    @Builder.Default
    private List<HashtagResponseDto> hashtags = new ArrayList<>(); // 해시태그 목록 추가

    // 기존 생성자 유지 (호환성을 위해)
    public StyleResponseDto(Long id, Long profileId, String profileName,
                            String profileImageUrl, String content,
                            String mediaUrl, Long viewCount, Integer likeCount) {
        this.id = id;
        this.profileId = profileId;
        this.profileName = profileName;
        this.profileImageUrl = profileImageUrl;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.liked = false; // 기본값 설정
        this.interested = false; // 기본값 설정
        this.hashtags = new ArrayList<>(); // 빈 리스트로 초기화
    }
}