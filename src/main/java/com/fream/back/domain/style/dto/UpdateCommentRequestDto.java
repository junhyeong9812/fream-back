package com.fream.back.domain.style.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCommentRequestDto {

    @NotBlank(message = "댓글 내용이 필요합니다.")
    private String updatedContent;
}