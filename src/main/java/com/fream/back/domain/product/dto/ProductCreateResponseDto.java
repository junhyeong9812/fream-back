package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.Product;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCreateResponseDto {
    private Long id;
    private String name;
    private String englishName;
    private int releasePrice;
    private String modelNumber;
    private String releaseDate;
    private String brandName;
    private String categoryName;
    private String collectionName;


    public static ProductCreateResponseDto fromEntity(Product product) {
        return ProductCreateResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .englishName(product.getEnglishName())
                .releasePrice(product.getReleasePrice())
                .modelNumber(product.getModelNumber())
                .releaseDate(product.getReleaseDate())
                .brandName(product.getBrand().getName())
                .categoryName(product.getCategory().getName())
                .collectionName(product.getCollection() != null ? product.getCollection().getName() : null)
                .build();
    }
}
