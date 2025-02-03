package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileStyleResponseDto {
    private Long id; // Style ID
    private String mediaUrl; // 미디어 URL
    private Long likeCount; // 좋아요 수
}

