package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponseDto {
    private Long id;
    private String name;
    private Long count;
}