package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StyleCommentsResponseDto {
    private List<StyleCommentResponseDto> comments; // 페이징된 댓글 목록
    private Long totalComments; // 총 댓글 수
    private String userProfileImageUrl;
}