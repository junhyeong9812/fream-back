package com.fream.back.domain.product.service.productColor;

import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 색상 조회(Query) 서비스
 * 상품 색상 조회 관련 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductColorQueryService {

    private final ProductColorRepository productColorRepository;

    /**
     * ID로 상품 색상 조회
     *
     * @param productColorId 상품 색상 ID
     * @return 상품 색상 엔티티
     * @throws ProductException 상품 색상이 존재하지 않을 경우
     */
    public ProductColor findById(Long productColorId) {
        log.debug("ID로 상품 색상 조회 - 색상ID: {}", productColorId);

        return productColorRepository.findById(productColorId)
                .orElseThrow(() -> {
                    log.warn("상품 색상 조회 실패 - 존재하지 않는 색상ID: {}", productColorId);
                    return new ProductException(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND,
                            "해당 ProductColor를 찾을 수 없습니다: " + productColorId);
                });
    }

    /**
     * 상품 ID로 모든 색상 조회
     *
     * @param productId 상품 ID
     * @return 상품 색상 엔티티 목록
     */
    public List<ProductColor> findAllByProductId(Long productId) {
        log.debug("상품 ID로 모든 색상 조회 - 상품ID: {}", productId);

        try {
            List<ProductColor> productColors = productColorRepository.findByProductId(productId);
            log.debug("상품 ID로 모든 색상 조회 성공 - 상품ID: {}, 색상 수: {}",
                    productId, productColors.size());
            return productColors;
        } catch (Exception e) {
            log.error("상품 ID로 모든 색상 조회 중 예상치 못한 오류 발생 - 상품ID: {}", productId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND,
                    "상품 색상 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}