package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleFilterRequestDto {
    private String brandName;
    private String collectionName;
    private Long categoryId;
    private Boolean isMainCategory;
    private String profileName;
    private String sortBy; // "popular" or "latest"
}
