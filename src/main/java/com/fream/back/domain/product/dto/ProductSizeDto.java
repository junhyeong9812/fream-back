package com.fream.back.domain.product.dto;

import lombok.*;

/**
 * 상품 사이즈 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSizeDto {
    private Long id;
    private String size;
    private int price;
    private int stock;
}
