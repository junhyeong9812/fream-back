package com.fream.back.domain.product.dto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageDto {
    private Long id;
    private String imageUrl;
}
