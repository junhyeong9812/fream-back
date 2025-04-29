package com.fream.back.domain.product.service.productSize;

import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.product.entity.enumType.SizeType;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductSizeRepository;
import com.fream.back.domain.product.service.category.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * 상품 사이즈 명령(Command) 서비스
 * 상품 사이즈의 생성, 수정, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductSizeCommandService {

    private final ProductSizeRepository productSizeRepository;
    private final CategoryQueryService categoryQueryService; // 카테고리 쿼리 서비스 주입

    /**
     * 상품 색상에 대한 사이즈 생성
     *
     * @param productColor 상품 색상 엔티티
     * @param categoryId 카테고리 ID
     * @param requestedSizes 요청된 사이즈 목록
     * @param releasePrice 출시 가격
     * @throws ProductException 상품 사이즈 생성 실패 시
     */
    public void createProductSizes(ProductColor productColor, Long categoryId, List<String> requestedSizes, int releasePrice) {
        log.info("상품 사이즈 생성 요청 - 색상ID: {}, 카테고리ID: {}, 요청 사이즈 수: {}",
                productColor.getId(), categoryId, requestedSizes.size());

        try {
            // 최상위 카테고리 찾기
            log.debug("최상위 카테고리 조회 시작 - 카테고리ID: {}", categoryId);
            Category rootCategory = categoryQueryService.findRootCategoryById(categoryId);
            log.debug("최상위 카테고리 조회 성공 - 카테고리명: {}", rootCategory.getName());

            // SizeType 결정
            log.debug("사이즈 타입 결정 시작 - 카테고리명: {}", rootCategory.getName());
            SizeType sizeType = determineSizeType(rootCategory.getName());
            log.debug("사이즈 타입 결정 완료 - 타입: {}", sizeType);

            // 해당 ProductColor의 기존 사이즈 조회
            log.debug("기존 사이즈 조회 시작 - 색상ID: {}", productColor.getId());
            List<String> existingSizes = productSizeRepository.findAllByProductColorId(productColor.getId())
                    .stream()
                    .map(ProductSize::getSize)
                    .toList();
            log.debug("기존 사이즈 조회 완료 - 사이즈 수: {}", existingSizes.size());

            // 요청된 사이즈 중 새로운 사이즈 필터링
            List<String> newSizes = requestedSizes.stream()
                    .filter(size -> !existingSizes.contains(size)) // 기존 사이즈에 없는 경우만 필터링
                    .toList();
            log.debug("새로운 사이즈 필터링 완료 - 새 사이즈 수: {}", newSizes.size());

            // 새로운 사이즈 생성
            int createdCount = 0;
            List<String> invalidSizes = new ArrayList<>();

            for (String size : newSizes) {
                if (isValidSize(size, sizeType)) {
                    log.debug("사이즈 생성 - 사이즈: {}, 타입: {}", size, sizeType);
                    ProductSize productSize = ProductSize.builder()
                            .size(size)
                            .sizeType(sizeType)
                            .purchasePrice(releasePrice)
                            .salePrice(releasePrice)
                            .quantity(0)
                            .productColor(productColor)
                            .build();
                    productSizeRepository.save(productSize);
                    createdCount++;
                } else {
                    log.warn("유효하지 않은 사이즈 발견 - 사이즈: {}, 타입: {}", size, sizeType);
                    invalidSizes.add(size);
                }
            }

            if (!invalidSizes.isEmpty()) {
                log.error("일부 사이즈가 유효하지 않음 - 유효하지 않은 사이즈: {}", invalidSizes);
                throw new ProductException(ProductErrorCode.INVALID_SIZE_TYPE,
                        "유효하지 않은 사이즈가 있습니다: " + String.join(", ", invalidSizes));
            }

            log.info("상품 사이즈 생성 성공 - 색상ID: {}, 생성된 사이즈 수: {}",
                    productColor.getId(), createdCount);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 사이즈 생성 중 예상치 못한 오류 발생 - 색상ID: {}", productColor.getId(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_CREATION_FAILED,
                    "상품 사이즈 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리 이름에 따른 사이즈 타입 결정
     *
     * @param rootCategoryName 최상위 카테고리명
     * @return 사이즈 타입
     * @throws ProductException 해당 카테고리에 맞는 사이즈 타입이 없는 경우
     */
    private SizeType determineSizeType(String rootCategoryName) {
        log.debug("카테고리 이름에 따른 사이즈 타입 결정 - 카테고리명: {}", rootCategoryName);

        switch (rootCategoryName.toUpperCase()) {
            case "CLOTHING":
                return SizeType.CLOTHING;
            case "SHOES":
                return SizeType.SHOES;
            case "ACCESSORIES":
                return SizeType.ACCESSORIES;
            default:
                log.warn("해당 카테고리에 맞는 SizeType이 없음 - 카테고리명: {}", rootCategoryName);
                throw new ProductException(ProductErrorCode.INVALID_SIZE_TYPE,
                        "해당 카테고리에 맞는 SizeType이 존재하지 않습니다: " + rootCategoryName);
        }
    }

    /**
     * 사이즈 유효성 검사
     *
     * @param size 사이즈
     * @param sizeType 사이즈 타입
     * @return 유효성 여부
     */
    private boolean isValidSize(String size, SizeType sizeType) {
        log.debug("사이즈 유효성 검사 - 사이즈: {}, 타입: {}", size, sizeType);
        return Arrays.asList(sizeType.getSizes()).contains(size);
    }

    /**
     * 상품 사이즈 삭제
     *
     * @param sizeId 사이즈 ID
     * @throws ProductException 상품 사이즈 삭제 실패 시
     */
    public void deleteProductSize(Long sizeId) {
        log.info("상품 사이즈 삭제 요청 - 사이즈ID: {}", sizeId);

        try {
            ProductSize productSize = productSizeRepository.findById(sizeId)
                    .orElseThrow(() -> {
                        log.warn("상품 사이즈 삭제 실패 - 존재하지 않는 사이즈ID: {}", sizeId);
                        return new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                                "해당 사이즈를 찾을 수 없습니다: " + sizeId);
                    });

            log.debug("상품 사이즈 조회 성공 - 사이즈: {}, 색상ID: {}",
                    productSize.getSize(), productSize.getProductColor().getId());
            productSizeRepository.delete(productSize);
            log.info("상품 사이즈 삭제 성공 - 사이즈ID: {}, 사이즈: {}",
                    sizeId, productSize.getSize());
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 사이즈 삭제 중 예상치 못한 오류 발생 - 사이즈ID: {}", sizeId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_DELETION_FAILED,
                    "상품 사이즈 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 사이즈 수정
     *
     * @param sizeId 사이즈 ID
     * @param purchasePrice 구매 가격
     * @param salePrice 판매 가격
     * @param quantity 수량
     * @throws ProductException 상품 사이즈 수정 실패 시
     */
    public void updateProductSize(Long sizeId, int purchasePrice, int salePrice, int quantity) {
        log.info("상품 사이즈 수정 요청 - 사이즈ID: {}, 구매가: {}, 판매가: {}, 수량: {}",
                sizeId, purchasePrice, salePrice, quantity);

        try {
            ProductSize productSize = productSizeRepository.findById(sizeId)
                    .orElseThrow(() -> {
                        log.warn("상품 사이즈 수정 실패 - 존재하지 않는 사이즈ID: {}", sizeId);
                        return new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                                "해당 사이즈를 찾을 수 없습니다: " + sizeId);
                    });

            log.debug("상품 사이즈 조회 성공 - 사이즈: {}, 색상ID: {}",
                    productSize.getSize(), productSize.getProductColor().getId());

            productSize.update(purchasePrice, salePrice, quantity);
            log.info("상품 사이즈 수정 성공 - 사이즈ID: {}, 사이즈: {}",
                    sizeId, productSize.getSize());
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 사이즈 수정 중 예상치 못한 오류 발생 - 사이즈ID: {}", sizeId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_UPDATE_FAILED,
                    "상품 사이즈 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 색상에 대한 모든 사이즈 삭제
     *
     * @param productColor 상품 색상 엔티티
     * @throws ProductException 상품 사이즈 삭제 실패 시
     */
    @Transactional
    public void deleteAllSizesByProductColor(ProductColor productColor) {
        log.info("상품 색상에 대한 모든 사이즈 삭제 요청 - 색상ID: {}", productColor.getId());

        try {
            List<ProductSize> sizes = productSizeRepository.findAllByProductColorId(productColor.getId());
            log.debug("상품 색상에 대한 사이즈 조회 성공 - 색상ID: {}, 사이즈 수: {}",
                    productColor.getId(), sizes.size());

            for (ProductSize size : sizes) {
                log.debug("사이즈 삭제 - 사이즈ID: {}, 사이즈: {}", size.getId(), size.getSize());
                deleteProductSize(size.getId());
            }

            // 영속성 컨텍스트를 강제로 플러시
            productSizeRepository.flush();
            productColor.getSizes().clear();

            log.info("상품 색상에 대한 모든 사이즈 삭제 성공 - 색상ID: {}, 삭제된 사이즈 수: {}",
                    productColor.getId(), sizes.size());
        } catch (Exception e) {
            log.error("상품 색상에 대한 모든 사이즈 삭제 중 예상치 못한 오류 발생 - 색상ID: {}",
                    productColor.getId(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_SIZE_DELETION_FAILED,
                    "상품 사이즈 일괄 삭제 중 오류가 발생했습니다.", e);
        }
    }
}