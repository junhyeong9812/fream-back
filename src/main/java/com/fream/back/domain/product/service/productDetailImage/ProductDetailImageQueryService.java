package com.fream.back.domain.product.service.productDetailImage;

import com.fream.back.domain.product.entity.ProductDetailImage;
import com.fream.back.domain.product.repository.ProductDetailImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductDetailImageQueryService {
    private final ProductDetailImageRepository productDetailImageRepository;

    public List<ProductDetailImage> findAllByProductColorId(Long productColorId) {
        return productDetailImageRepository.findAllByProductColorId(productColorId);
    }
    public boolean existsByProductColorId(Long productColorId) {
        return productDetailImageRepository.existsByProductColorId(productColorId);
    }
}