package com.fream.back.domain.product.service.productSize;

import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품 사이즈 조회(Query) 서비스
 * 상품 사이즈 조회 관련 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductSizeQueryService {

    private final ProductSizeRepository productSizeRepository;

    /**
     * 색상 ID와 사이즈로 상품 사이즈 조회
     *
     * @param colorId 색상 ID
     * @param size 사이즈
     * @return 상품 사이즈 엔티티 Optional
     * @throws ProductException 상품 사이즈 조회 실패 시
     */
    public Optional<ProductSize> findByColorIdAndSize(Long colorId, String size) {
        log.debug("색상 ID와 사이즈로 상품 사이즈 조회 - 색상ID: {}, 사이즈: {}", colorId, size);

        try {
            Optional<ProductSize> productSize = productSizeRepository.findByProductColorIdAndSize(colorId, size);
            if (productSize.isPresent()) {
                log.debug("색상 ID와 사이즈로 상품 사이즈 조회 성공 - 색상ID: {}, 사이즈: {}, 사이즈ID: {}",
                        colorId, size, productSize.get().getId());
            } else {
                log.debug("색상 ID와 사이즈로 상품 사이즈 조회 결과 없음 - 색상ID: {}, 사이즈: {}",
                        colorId, size);
            }
            return productSize;
        } catch (Exception e) {
            log.error("색상 ID와 사이즈로 상품 사이즈 조회 중 예상치 못한 오류 발생 - 색상ID: {}, 사이즈: {}",
                    colorId, size, e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                    "상품 사이즈 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 색상 ID로 모든 사이즈 문자열 조회
     *
     * @param productColorId 상품 색상 ID
     * @return 사이즈 문자열 목록
     * @throws ProductException 상품 사이즈 조회 실패 시
     */
    public List<String> findSizesByColorId(Long productColorId) {
        log.debug("색상 ID로 모든 사이즈 문자열 조회 - 색상ID: {}", productColorId);

        try {
            List<String> sizes = productSizeRepository.findAllByProductColorId(productColorId)
                    .stream()
                    .map(ProductSize::getSize) // 사이즈 값만 추출
                    .collect(Collectors.toList());

            log.debug("색상 ID로 모든 사이즈 문자열 조회 성공 - 색상ID: {}, 사이즈 수: {}",
                    productColorId, sizes.size());
            return sizes;
        } catch (Exception e) {
            log.error("색상 ID로 모든 사이즈 문자열 조회 중 예상치 못한 오류 발생 - 색상ID: {}",
                    productColorId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                    "상품 사이즈 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ID로 상품 사이즈 조회
     *
     * @param id 사이즈 ID
     * @return 상품 사이즈 엔티티 Optional
     * @throws ProductException 상품 사이즈 조회 실패 시
     */
    public Optional<ProductSize> findById(Long id) {
        log.debug("ID로 상품 사이즈 조회 - 사이즈ID: {}", id);

        try {
            Optional<ProductSize> productSize = productSizeRepository.findById(id);
            if (productSize.isPresent()) {
                log.debug("ID로 상품 사이즈 조회 성공 - 사이즈ID: {}, 사이즈: {}",
                        id, productSize.get().getSize());
            } else {
                log.debug("ID로 상품 사이즈 조회 결과 없음 - 사이즈ID: {}", id);
            }
            return productSize;
        } catch (Exception e) {
            log.error("ID로 상품 사이즈 조회 중 예상치 못한 오류 발생 - 사이즈ID: {}", id, e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                    "상품 사이즈 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 색상 ID로 모든 상품 사이즈 엔티티 조회
     *
     * @param productColorId 상품 색상 ID
     * @return 상품 사이즈 엔티티 목록
     * @throws ProductException 상품 사이즈 조회 실패 시
     */
    public List<ProductSize> findAllByProductColorId(Long productColorId) {
        log.debug("색상 ID로 모든 상품 사이즈 엔티티 조회 - 색상ID: {}", productColorId);

        try {
            List<ProductSize> sizes = productSizeRepository.findAllByProductColorId(productColorId);
            log.debug("색상 ID로 모든 상품 사이즈 엔티티 조회 성공 - 색상ID: {}, 사이즈 수: {}",
                    productColorId, sizes.size());
            return sizes;
        } catch (Exception e) {
            log.error("색상 ID로 모든 상품 사이즈 엔티티 조회 중 예상치 못한 오류 발생 - 색상ID: {}",
                    productColorId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                    "상품 사이즈 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}