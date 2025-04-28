package com.fream.back.domain.product.service.product;

import com.fream.back.domain.product.entity.Product;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 엔티티 서비스
 * 상품 엔티티를 조회하는 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductEntityService {

    private final ProductRepository productRepository;

    /**
     * ID로 상품 엔티티 조회
     *
     * @param id 상품 ID
     * @return 상품 엔티티
     * @throws ProductException 상품이 존재하지 않을 경우
     */
    public Product findById(Long id) {
        log.debug("ID로 상품 엔티티 조회 - 상품ID: {}", id);

        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("상품 엔티티 조회 실패 - 존재하지 않는 상품ID: {}", id);
                    return new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                            "해당 Product가 존재하지 않습니다. ID: " + id);
                });
    }
}