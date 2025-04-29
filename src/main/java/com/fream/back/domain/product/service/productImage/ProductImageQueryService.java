package com.fream.back.domain.product.service.productImage;

import com.fream.back.domain.product.entity.ProductImage;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 이미지 조회(Query) 서비스
 * 상품 이미지 조회 관련 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductImageQueryService {

    private final ProductImageRepository productImageRepository;

    /**
     * 상품 색상 ID로 모든 이미지 조회
     *
     * @param productColorId 상품 색상 ID
     * @return 상품 이미지 엔티티 목록
     * @throws ProductException 상품 이미지 조회 실패 시
     */
    public List<ProductImage> findAllByProductColorId(Long productColorId) {
        log.debug("상품 색상 ID로 모든 이미지 조회 - 색상ID: {}", productColorId);

        try {
            List<ProductImage> images = productImageRepository.findAllByProductColorId(productColorId);
            log.debug("상품 색상 ID로 모든 이미지 조회 성공 - 색상ID: {}, 이미지 수: {}",
                    productColorId, images.size());
            return images;
        } catch (Exception e) {
            log.error("상품 색상 ID로 모든 이미지 조회 중 예상치 못한 오류 발생 - 색상ID: {}",
                    productColorId, e);
            throw new ProductException(ProductErrorCode.IMAGE_NOT_FOUND,
                    "상품 이미지 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 색상 ID로 이미지 존재 여부 확인
     *
     * @param productColorId 상품 색상 ID
     * @return 이미지 존재 여부
     * @throws ProductException 상품 이미지 조회 실패 시
     */
    public boolean existsByProductColorId(Long productColorId) {
        log.debug("상품 색상 ID로 이미지 존재 여부 확인 - 색상ID: {}", productColorId);

        try {
            boolean exists = productImageRepository.existsByProductColorId(productColorId);
            log.debug("상품 색상 ID로 이미지 존재 여부 확인 완료 - 색상ID: {}, 존재 여부: {}",
                    productColorId, exists);
            return exists;
        } catch (Exception e) {
            log.error("상품 색상 ID로 이미지 존재 여부 확인 중 예상치 못한 오류 발생 - 색상ID: {}",
                    productColorId, e);
            throw new ProductException(ProductErrorCode.IMAGE_NOT_FOUND,
                    "상품 이미지 존재 여부 확인 중 오류가 발생했습니다.", e);
        }
    }
}