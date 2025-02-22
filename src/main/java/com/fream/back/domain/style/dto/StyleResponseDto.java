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
}
