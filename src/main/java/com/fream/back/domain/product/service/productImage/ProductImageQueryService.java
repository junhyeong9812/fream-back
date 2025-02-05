package com.fream.back.domain.product.service.productImage;

import com.fream.back.domain.product.entity.ProductImage;
import com.fream.back.domain.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductImageQueryService {
    private final ProductImageRepository productImageRepository;

    public List<ProductImage> findAllByProductColorId(Long productColorId) {
        return productImageRepository.findAllByProductColorId(productColorId);
    }
    public boolean existsByProductColorId(Long productColorId) {
        return productImageRepository.existsByProductColorId(productColorId);
    }
}