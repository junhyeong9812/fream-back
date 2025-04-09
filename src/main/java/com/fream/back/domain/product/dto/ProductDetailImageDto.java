package com.fream.back.domain.product.dto;

import lombok.*;

/**
 * 상품 상세 이미지 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailImageDto {
    private Long id;
    private String imageUrl;
}