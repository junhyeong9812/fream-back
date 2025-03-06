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
    private Long commentCount;
    private Boolean liked; // 좋아요 상태 필드 추가
}