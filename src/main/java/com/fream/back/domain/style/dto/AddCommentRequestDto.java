package com.fream.back.domain.style.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCommentRequestDto {

    @NotNull(message = "스타일 ID가 필요합니다.")
    @Positive(message = "스타일 ID는 양수여야 합니다.")
    private Long styleId;

    @NotBlank(message = "댓글 내용이 필요합니다.")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다.")
    private String content;

    @Positive(message = "부모 댓글 ID는 양수여야 합니다.")
    private Long parentCommentId;
}