package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductSize;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 관리자용 상품 색상 상세 응답 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductColorDetailAdminResponseDto {
    private Long id;                   // 색상 ID
    private String colorName;          // 색상명
    private String content;            // 상세 설명 (HTML)
    private String thumbnailImageUrl;  // 대표 이미지 URL
    private List<ProductImageDto> images;           // 일반 이미지 목록
    private List<ProductDetailImageDto> detailImages; // 상세 이미지 목록
    private List<String> sizes;             // 사이즈 정보 (String만 필요)

    public static ProductColorDetailAdminResponseDto fromEntity(ProductColor productColor) {
        // 썸네일 이미지
        String thumbnailUrl = null;
        if (productColor.getThumbnailImage() != null) {
            thumbnailUrl = productColor.getThumbnailImage().getImageUrl();
        }

        // 일반 이미지
        List<ProductImageDto> imageDtos = productColor.getProductImages().stream()
                .map(image -> new ProductImageDto(image.getId(), image.getImageUrl()))
                .collect(Collectors.toList());

        // 상세 이미지
        List<ProductDetailImageDto> detailImageDtos = productColor.getProductDetailImages().stream()
                .map(image -> new ProductDetailImageDto(image.getId(), image.getImageUrl()))
                .collect(Collectors.toList());

        // 사이즈 정보 (String만 필요)
        List<String> sizes = productColor.getSizes().stream()
                .map(ProductSize::getSize)
                .collect(Collectors.toList());

        return ProductColorDetailAdminResponseDto.builder()
                .id(productColor.getId())
                .colorName(productColor.getColorName())
                .content(productColor.getContent())
                .thumbnailImageUrl(thumbnailUrl)
                .images(imageDtos)
                .detailImages(detailImageDtos)
                .sizes(sizes)
                .build();
    }
}