package com.fream.back.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductColorCreateRequestDto {
    private String colorName; // 색상명
    private String content; // 상세 설명 (HTML)
    private List<String> sizes; // 사이즈 배열
}
