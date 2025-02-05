package com.fream.back.domain.product.service.productImage;

import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductImage;
import com.fream.back.domain.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductImageCommandService {
    private final ProductImageRepository productImageRepository;

    public ProductImage createProductImage(String imageUrl, ProductColor productColor) {
        ProductImage productImage = ProductImage.builder()
                .imageUrl(imageUrl)
                .productColor(productColor)
                .build();
        return productImageRepository.save(productImage);
    }

    public void deleteProductImage(Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이미지를 찾을 수 없습니다."));
        productImageRepository.delete(productImage);
    }
}