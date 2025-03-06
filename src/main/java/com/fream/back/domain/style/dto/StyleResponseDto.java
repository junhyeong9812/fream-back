package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StyleResponseDto {
    private Long id;
    private Long profileId;
    private String profileName;
    private String profileImageUrl;
    private String content;
    private String mediaUrl;
    private Long viewCount;
    private Integer likeCount;
    private Boolean liked;

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
    }
}
