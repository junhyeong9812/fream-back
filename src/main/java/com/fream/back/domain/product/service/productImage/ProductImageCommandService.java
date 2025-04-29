package com.fream.back.domain.product.service.productImage;

import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductImage;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 이미지 명령(Command) 서비스
 * 상품 이미지의 생성, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductImageCommandService {

    private final ProductImageRepository productImageRepository;

    /**
     * 상품 이미지 생성
     *
     * @param imageUrl 이미지 URL
     * @param productColor 상품 색상 엔티티
     * @return 생성된 상품 이미지 엔티티
     * @throws ProductException 상품 이미지 생성 실패 시
     */
    public ProductImage createProductImage(String imageUrl, ProductColor productColor) {
        log.info("상품 이미지 생성 요청 - 색상ID: {}, 이미지URL: {}",
                productColor.getId(), imageUrl);

        try {
            ProductImage productImage = ProductImage.builder()
                    .imageUrl(imageUrl)
                    .productColor(productColor)
                    .build();

            ProductImage savedImage = productImageRepository.save(productImage);
            log.info("상품 이미지 생성 성공 - 이미지ID: {}", savedImage.getId());
            return savedImage;
        } catch (Exception e) {
            log.error("상품 이미지 생성 중 예상치 못한 오류 발생 - 색상ID: {}, 이미지URL: {}",
                    productColor.getId(), imageUrl, e);
            throw new ProductException(ProductErrorCode.IMAGE_UPLOAD_FAILED,
                    "상품 이미지 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 이미지 삭제
     *
     * @param imageId 이미지 ID
     * @throws ProductException 상품 이미지 삭제 실패 시
     */
    public void deleteProductImage(Long imageId) {
        log.info("상품 이미지 삭제 요청 - 이미지ID: {}", imageId);

        try {
            ProductImage productImage = productImageRepository.findById(imageId)
                    .orElseThrow(() -> {
                        log.warn("상품 이미지 삭제 실패 - 존재하지 않는 이미지ID: {}", imageId);
                        return new ProductException(ProductErrorCode.IMAGE_NOT_FOUND,
                                "해당 이미지를 찾을 수 없습니다: " + imageId);
                    });

            productImageRepository.delete(productImage);
            log.info("상품 이미지 삭제 성공 - 이미지ID: {}", imageId);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 이미지 삭제 중 예상치 못한 오류 발생 - 이미지ID: {}", imageId, e);
            throw new ProductException(ProductErrorCode.IMAGE_DELETION_FAILED,
                    "상품 이미지 삭제 중 오류가 발생했습니다.", e);
        }
    }
}