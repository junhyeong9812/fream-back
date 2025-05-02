package com.fream.back.domain.style.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HashtagUpdateRequestDto {

    @NotBlank(message = "해시태그 이름이 필요합니다.")
    @Size(min = 1, max = 50, message = "해시태그는 1자 이상 50자 이하여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "해시태그는 특수문자를 포함할 수 없습니다.")
    private String name;
}