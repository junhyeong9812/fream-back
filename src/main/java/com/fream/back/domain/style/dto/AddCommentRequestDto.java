package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCommentRequestDto {
    private Long styleId; // 댓글을 달 스타일 ID
    private String content; // 댓글 내용
    private Long parentCommentId; // 부모 댓글 ID (대댓글일 경우)
}
