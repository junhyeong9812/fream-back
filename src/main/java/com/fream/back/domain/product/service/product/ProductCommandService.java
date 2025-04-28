package com.fream.back.domain.product.service.product;

import com.fream.back.domain.product.dto.ProductCreateRequestDto;
import com.fream.back.domain.product.dto.ProductCreateResponseDto;
import com.fream.back.domain.product.dto.ProductUpdateRequestDto;
import com.fream.back.domain.product.dto.ProductUpdateResponseDto;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.entity.Collection;
import com.fream.back.domain.product.entity.Product;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductRepository;
import com.fream.back.domain.product.service.brand.BrandEntityService;
import com.fream.back.domain.product.service.category.CategoryEntityService;
import com.fream.back.domain.product.service.collection.CollectionCommandService;
import com.fream.back.domain.product.service.productColor.ProductColorCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 명령(Command) 서비스
 * 상품의 생성, 수정, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final BrandEntityService brandEntityService;
    private final CategoryEntityService categoryEntityService;
    private final CollectionCommandService collectionCommandService;
    private final ProductColorCommandService productColorCommandService;

    /**
     * 상품 생성
     *
     * @param request 상품 생성 요청 DTO
     * @return 생성된 상품 응답 DTO
     * @throws ProductException 상품 생성 실패 시
     */
    public ProductCreateResponseDto createProduct(ProductCreateRequestDto request) {
        log.info("상품 생성 요청 - 상품명: {}, 브랜드명: {}", request.getName(), request.getBrandName());

        try {
            // 브랜드 엔티티 확인
            log.debug("브랜드 조회 시작 - 브랜드명: {}", request.getBrandName());
            Brand brand = brandEntityService.findByName(request.getBrandName());
            log.debug("브랜드 조회 성공 - 브랜드ID: {}", brand.getId());

            // 서브 카테고리 확인
            log.debug("카테고리 조회 시작 - 메인 카테고리: {}, 서브 카테고리: {}",
                    request.getMainCategoryName(), request.getCategoryName());
            Category category = categoryEntityService.findSubCategoryByName(
                    request.getCategoryName(),
                    request.getMainCategoryName()
            );
            log.debug("카테고리 조회 성공 - 카테고리ID: {}", category.getId());

            // 컬렉션 확인 및 생성
            Collection collection = null;
            if (request.getCollectionName() != null) {
                log.debug("컬렉션 조회/생성 시작 - 컬렉션명: {}", request.getCollectionName());
                collection = collectionCommandService.createOrGetCollection(request.getCollectionName());
                log.debug("컬렉션 조회/생성 성공 - 컬렉션ID: {}", collection.getId());
            }

            // 상품 생성
            log.debug("상품 엔티티 생성 시작");
            Product product = Product.builder()
                    .name(request.getName())
                    .englishName(request.getEnglishName())
                    .releasePrice(request.getReleasePrice())
                    .modelNumber(request.getModelNumber())
                    .releaseDate(request.getReleaseDate())
                    .gender(request.getGender())
                    .brand(brand)
                    .category(category)
                    .collection(collection)
                    .build();

            // 저장
            Product savedProduct = productRepository.save(product);
            log.info("상품 생성 성공 - 상품ID: {}, 상품명: {}", savedProduct.getId(), savedProduct.getName());

            // DTO 변환 후 반환
            return ProductCreateResponseDto.fromEntity(savedProduct);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 생성 중 예상치 못한 오류 발생 - 상품명: {}", request.getName(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_CREATION_FAILED, "상품 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 수정
     *
     * @param productId 상품 ID
     * @param request 상품 수정 요청 DTO
     * @return 수정된 상품 응답 DTO
     * @throws ProductException 상품 수정 실패 시
     */
    public ProductUpdateResponseDto updateProduct(Long productId, ProductUpdateRequestDto request) {
        log.info("상품 수정 요청 - 상품ID: {}, 상품명: {}", productId, request.getName());

        try {
            // 기존 Product 조회
            log.debug("기존 상품 조회 시작 - 상품ID: {}", productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("상품 수정 실패 - 존재하지 않는 상품ID: {}", productId);
                        return new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                                "존재하지 않는 상품입니다. ID: " + productId);
                    });
            log.debug("기존 상품 조회 성공 - 상품명: {}", product.getName());

            // 브랜드 확인 (필요시)
            Brand brand = null;
            if (request.getBrandName() != null) {
                log.debug("브랜드 조회 시작 - 브랜드명: {}", request.getBrandName());
                brand = brandEntityService.findByName(request.getBrandName());
                log.debug("브랜드 조회 성공 - 브랜드ID: {}", brand.getId());
            }

            // 카테고리 확인 (필요시)
            Category category = null;
            if (request.getCategoryName() != null && request.getMainCategoryName() != null) {
                log.debug("카테고리 조회 시작 - 메인 카테고리: {}, 서브 카테고리: {}",
                        request.getMainCategoryName(), request.getCategoryName());
                category = categoryEntityService.findSubCategoryByName(
                        request.getCategoryName(), request.getMainCategoryName());
                log.debug("카테고리 조회 성공 - 카테고리ID: {}", category.getId());
            }

            // 컬렉션 확인 및 생성
            Collection collection = null;
            if (request.getCollectionName() != null) {
                log.debug("컬렉션 조회/생성 시작 - 컬렉션명: {}", request.getCollectionName());
                collection = collectionCommandService.createOrGetCollection(request.getCollectionName());
                log.debug("컬렉션 조회/생성 성공 - 컬렉션ID: {}", collection.getId());
            }

            // 업데이트 수행
            log.debug("상품 업데이트 시작");
            product.updateProduct(request, brand, category, collection);
            log.info("상품 업데이트 성공 - 상품ID: {}, 상품명: {}", productId, product.getName());

            // DTO 변환 후 반환
            return ProductUpdateResponseDto.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .englishName(product.getEnglishName())
                    .releasePrice(product.getReleasePrice())
                    .modelNumber(product.getModelNumber())
                    .releaseDate(product.getReleaseDate())
                    .gender(request.getGender())
                    .brandName(product.getBrand().getName())
                    .categoryName(product.getCategory().getName())
                    .collectionName(product.getCollection() != null ? product.getCollection().getName() : null)
                    .build();
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 수정 중 예상치 못한 오류 발생 - 상품ID: {}", productId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_UPDATE_FAILED, "상품 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 삭제
     *
     * @param productId 상품 ID
     * @throws ProductException 상품 삭제 실패 시
     */
    @Transactional
    public void deleteProduct(Long productId) {
        log.info("상품 삭제 요청 - 상품ID: {}", productId);

        try {
            log.debug("상품 조회 시작 - 상품ID: {}", productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("상품 삭제 실패 - 존재하지 않는 상품ID: {}", productId);
                        return new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                                "존재하지 않는 상품입니다. ID: " + productId);
                    });
            log.debug("상품 조회 성공 - 상품명: {}, 색상 수: {}",
                    product.getName(), product.getColors().size());

            // ProductColor 삭제
            log.debug("상품 색상 삭제 시작 - 색상 수: {}", product.getColors().size());
            product.getColors().forEach(productColor -> {
                log.debug("색상 삭제 - 색상ID: {}, 색상명: {}",
                        productColor.getId(), productColor.getColorName());
                productColorCommandService.deleteProductColor(productColor.getId());
            });
            log.debug("상품 색상 삭제 완료");

            // Product 삭제
            log.debug("상품 삭제 시작 - 상품ID: {}", productId);
            productRepository.delete(product);
            log.info("상품 삭제 성공 - 상품ID: {}", productId);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 삭제 중 예상치 못한 오류 발생 - 상품ID: {}", productId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_DELETION_FAILED, "상품 삭제 중 오류가 발생했습니다.", e);
        }
    }
}