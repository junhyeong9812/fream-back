package com.fream.back.domain.style.dto;

import com.fream.back.domain.style.entity.StyleComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StyleCommentResponseDto {
    private Long id;
    private Long profileId;
    private String profileName;
    private String profileImageUrl;
    private String content;
    private Long likeCount;
    private Boolean liked;
    private LocalDateTime createdDate;

    @Builder.Default
    private List<StyleCommentResponseDto> replies = new ArrayList<>();
}