package com.fream.back.domain.product.service.productColor;

import com.fream.back.domain.product.dto.ProductColorCreateRequestDto;
import com.fream.back.domain.product.dto.ProductColorUpdateRequestDto;
import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.product.entity.enumType.ColorType;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductColorRepository;
import com.fream.back.domain.product.service.interest.InterestCommandService;
import com.fream.back.domain.product.service.product.ProductEntityService;
import com.fream.back.domain.product.service.productDetailImage.ProductDetailImageCommandService;
import com.fream.back.domain.product.service.productDetailImage.ProductDetailImageQueryService;
import com.fream.back.domain.product.service.productImage.ProductImageCommandService;
import com.fream.back.domain.product.service.productImage.ProductImageQueryService;
import com.fream.back.domain.product.service.productSize.ProductSizeCommandService;
import com.fream.back.domain.product.service.productSize.ProductSizeQueryService;
import com.fream.back.global.utils.FileUtils;
import com.fream.back.global.utils.NginxCachePurgeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 상품 색상 명령(Command) 서비스
 * 상품 색상의 생성, 수정, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductColorCommandService {

    private final ProductColorRepository productColorRepository;
    private final ProductSizeCommandService productSizeCommandService;
    private final FileUtils fileUtils;
    private final ProductEntityService productEntityService;
    private final ProductSizeQueryService productSizeQueryService;
    private final InterestCommandService interestCommandService;
    private final ProductImageCommandService productImageCommandService;
    private final ProductDetailImageCommandService productDetailImageCommandService;
    private final ProductImageQueryService productImageQueryService;
    private final ProductDetailImageQueryService productDetailImageQueryService;
    private final NginxCachePurgeUtil nginxCachePurgeUtil;
    private final JobLauncher jobLauncher;      // 배치 런처

    @Qualifier("createSizesJob")
    @Autowired
    private Job createSizesJob;           // 위에서 정의한 잡

    /**
     * 상품 색상 생성
     *
     * @param requestDto 색상·컨텐츠·사이즈 등 정보
     * @param productImage (단일) 썸네일 이미지
     * @param productImages (다중) 일반 이미지
     * @param productDetailImages (다중) 상세 이미지
     * @param productId 상품 ID
     * @return 생성된 상품 색상 ID
     * @throws ProductException 상품 색상 생성 실패 시
     */
    public Long createProductColor(
            ProductColorCreateRequestDto requestDto,
            MultipartFile productImage,
            List<MultipartFile> productImages,
            List<MultipartFile> productDetailImages,
            Long productId
    ) {
        log.info("상품 색상 생성 요청 - 상품ID: {}, 색상명: {}", productId, requestDto.getColorName());

        try {
            // 1) 색상 유효성 검증
            log.debug("색상 유효성 검증 시작 - 색상명: {}", requestDto.getColorName());
            validateColorType(requestDto.getColorName());
            log.debug("색상 유효성 검증 완료");

            // 2) 상품 조회
            log.debug("상품 조회 시작 - 상품ID: {}", productId);
            Product product = productEntityService.findById(productId);
            log.debug("상품 조회 성공 - 상품명: {}", product.getName());

            // 3) ProductColor 엔티티 생성
            log.debug("상품 색상 엔티티 생성 시작");
            ProductColor productColor = ProductColor.builder()
                    .colorName(requestDto.getColorName())
                    .content(requestDto.getContent())
                    .product(product)
                    .build();

            // 4) 디렉토리: "product/{productId}"
            String directory = "product/" + product.getId();
            log.debug("디렉토리 경로 설정 - 경로: {}", directory);

            // (A) 썸네일 이미지 저장
            if (productImage != null && !productImage.isEmpty()) {
                log.debug("썸네일 이미지 저장 시작");
                // saveFile → "thumbnail_xxx.jpg"
                String thumbnailFilename = fileUtils.saveFile(directory, "thumbnail_", productImage);
                log.debug("썸네일 이미지 저장 완료 - 파일명: {}", thumbnailFilename);

                // DB에는 파일명만 저장
                ProductImage thumbnail = ProductImage.builder()
                        .imageUrl(thumbnailFilename) // ex) "thumbnail_abc.jpg"
                        .build();
                productColor.addThumbnailImage(thumbnail);
            }

            // (B) 일반 이미지 (다중)
            if (productImages != null && !productImages.isEmpty()) {
                log.debug("일반 이미지 저장 시작 - 이미지 수: {}", productImages.size());
                for (MultipartFile file : productImages) {
                    String imageFilename = fileUtils.saveFile(directory, "ProductImage_", file);
                    ProductImage productImageEntity = ProductImage.builder()
                            .imageUrl(imageFilename)
                            .build();
                    productColor.addProductImage(productImageEntity);
                }
                log.debug("일반 이미지 저장 완료");
            }

            // (C) 상세 이미지 (다중)
            if (productDetailImages != null && !productDetailImages.isEmpty()) {
                log.debug("상세 이미지 저장 시작 - 이미지 수: {}", productDetailImages.size());
                for (MultipartFile file : productDetailImages) {
                    String detailFilename = fileUtils.saveFile(directory, "ProductDetailImage_", file);
                    ProductDetailImage detailImage = ProductDetailImage.builder()
                            .imageUrl(detailFilename)
                            .build();
                    productColor.addProductDetailImage(detailImage);
                }
                log.debug("상세 이미지 저장 완료");
            }

            // 5) DB 저장
            log.debug("상품 색상 DB 저장 시작");
            ProductColor savedColor = productColorRepository.save(productColor);
            log.debug("상품 색상 DB 저장 완료 - 색상ID: {}", savedColor.getId());

            // 6) 사이즈 생성
            log.debug("상품 사이즈 생성 시작 - 사이즈 수: {}", requestDto.getSizes().size());
            productSizeCommandService.createProductSizes(
                    savedColor,
                    product.getCategory().getId(),
                    requestDto.getSizes(),
                    product.getReleasePrice()
            );
            log.debug("상품 사이즈 생성 완료");

            log.info("상품 색상 생성 성공 - 상품ID: {}, 색상ID: {}, 색상명: {}",
                    productId, savedColor.getId(), savedColor.getColorName());
            return savedColor.getId();
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (IllegalArgumentException e) {
            log.error("상품 색상 생성 실패 - 상품ID: {}, 색상명: {}, 오류: {}",
                    productId, requestDto.getColorName(), e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_CREATION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 색상 생성 중 예상치 못한 오류 발생 - 상품ID: {}, 색상명: {}",
                    productId, requestDto.getColorName(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_CREATION_FAILED,
                    "상품 색상 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 색상 타입 유효성 검증
     *
     * @param colorName 색상명
     * @throws ProductException 유효하지 않은if! 색상인 경우
     */
    private void validateColorType(String colorName) {
        log.debug("색상 타입 유효성 검증 - 색상명: {}", colorName);

        boolean isValid = Arrays.stream(ColorType.values())
                .anyMatch(colorType ->
                        colorType.name().equalsIgnoreCase(colorName.trim()) ||
                                colorType.getDisplayName().equals(colorName.trim())
                ); // 두 가지 방식 비교

        if (!isValid) {
            log.warn("유효하지 않은 색상 - 색상명: {}", colorName);
            throw new ProductException(ProductErrorCode.INVALID_COLOR_TYPE,
                    "유효하지 않은 색상입니다: " + colorName);
        }

        log.debug("색상 타입 유효성 검증 완료");
    }

    /**
     * 상품 색상 수정
     *
     * @param productColorId 상품 색상 ID
     * @param requestDto 수정 요청 DTO
     * @param thumbnailImage 새 썸네일 이미지
     * @param newImages 새 일반 이미지 목록
     * @param newDetailImages 새 상세 이미지 목록
     * @throws ProductException 상품 색상 수정 실패 시
     */
    @Transactional
    public void updateProductColor(
            Long productColorId,
            ProductColorUpdateRequestDto requestDto,
            MultipartFile thumbnailImage,
            List<MultipartFile> newImages,
            List<MultipartFile> newDetailImages
    ) {
        log.info("상품 색상 수정 요청 - 색상ID: {}, 색상명: {}", productColorId, requestDto.getColorName());

        try {
            // 1) ProductColor 조회
            log.debug("상품 색상 조회 시작 - 색상ID: {}", productColorId);
            ProductColor productColor = productColorRepository.findById(productColorId)
                    .orElseThrow(() -> {
                        log.warn("상품 색상 수정 실패 - 존재하지 않는 색상ID: {}", productColorId);
                        return new ProductException(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND,
                                "해당 색상을 찾을 수 없습니다: " + productColorId);
                    });
            log.debug("상품 색상 조회 성공 - 색상명: {}", productColor.getColorName());

            // 색상 유효성 검증
            if (!productColor.getColorName().equals(requestDto.getColorName())) {
                log.debug("색상 유효성 검증 시작 - 새 색상명: {}", requestDto.getColorName());
                validateColorType(requestDto.getColorName());
                log.debug("색상 유효성 검증 완료");
            }

            // 2) 디렉토리: "product/{productId}"
            Product product = productColor.getProduct();
            String directory = "product/" + product.getId();
            log.debug("디렉토리 경로 설정 - 경로: {}", directory);

            // (A) 썸네일 교체
            if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
                log.debug("썸네일 이미지 교체 시작");
                if (productColor.getThumbnailImage() != null) {
                    // 기존 파일 삭제
                    String oldThumb = productColor.getThumbnailImage().getImageUrl(); // ex) "thumbnail_abc.jpg"
                    log.debug("기존 썸네일 삭제 - 파일명: {}", oldThumb);
                    fileUtils.deleteFile(directory, oldThumb);
                }
                // 새 파일 저장
                String newThumbFile = fileUtils.saveFile(directory, "thumbnail_", thumbnailImage);
                log.debug("새 썸네일 저장 - 파일명: {}", newThumbFile);
                ProductImage newThumbnail = ProductImage.builder()
                        .imageUrl(newThumbFile)
                        .build();
                productColor.addThumbnailImage(newThumbnail);
            }

            // (B) 기존 일반 이미지 삭제 (requestDto.getExistingImages()만 남긴다)
            if (requestDto.getExistingImages() != null) {
                log.debug("기존 일반 이미지 삭제 시작 - 유지할 이미지 수: {}", requestDto.getExistingImages().size());
                int removedCount = 0;
                for (ProductImage image : new ArrayList<>(productColor.getProductImages())) {
                    if (!requestDto.getExistingImages().contains(image.getImageUrl())) {
                        // 파일 삭제
                        log.debug("일반 이미지 삭제 - 파일명: {}", image.getImageUrl());
                        fileUtils.deleteFile(directory, image.getImageUrl());
                        productColor.getProductImages().remove(image);
                        removedCount++;
                    }
                }
                log.debug("기존 일반 이미지 삭제 완료 - 삭제된 이미지 수: {}", removedCount);
            }

            // (C) 새 일반 이미지 추가
            if (newImages != null && !newImages.isEmpty()) {
                log.debug("새 일반 이미지 추가 시작 - 이미지 수: {}", newImages.size());
                for (MultipartFile file : newImages) {
                    String newImageFile = fileUtils.saveFile(directory, "ProductImage_", file);
                    log.debug("새 일반 이미지 저장 - 파일명: {}", newImageFile);
                    ProductImage newImageEntity = ProductImage.builder()
                            .imageUrl(newImageFile)
                            .build();
                    productColor.addProductImage(newImageEntity);
                }
                log.debug("새 일반 이미지 추가 완료");
            }

            // (D) 기존 상세 이미지 삭제
            if (requestDto.getExistingDetailImages() != null) {
                log.debug("기존 상세 이미지 삭제 시작 - 유지할 이미지 수: {}", requestDto.getExistingDetailImages().size());
                int removedCount = 0;
                for (ProductDetailImage detailImage : new ArrayList<>(productColor.getProductDetailImages())) {
                    if (!requestDto.getExistingDetailImages().contains(detailImage.getImageUrl())) {
                        log.debug("상세 이미지 삭제 - 파일명: {}", detailImage.getImageUrl());
                        fileUtils.deleteFile(directory, detailImage.getImageUrl());
                        productColor.getProductDetailImages().remove(detailImage);
                        removedCount++;
                    }
                }
                log.debug("기존 상세 이미지 삭제 완료 - 삭제된 이미지 수: {}", removedCount);
            }

            // (E) 새 상세 이미지 추가
            if (newDetailImages != null && !newDetailImages.isEmpty()) {
                log.debug("새 상세 이미지 추가 시작 - 이미지 수: {}", newDetailImages.size());
                for (MultipartFile file : newDetailImages) {
                    String detailFilename = fileUtils.saveFile(directory, "ProductDetailImage_", file);
                    log.debug("새 상세 이미지 저장 - 파일명: {}", detailFilename);
                    ProductDetailImage newDetailImage = ProductDetailImage.builder()
                            .imageUrl(detailFilename)
                            .build();
                    productColor.addProductDetailImage(newDetailImage);
                }
                log.debug("새 상세 이미지 추가 완료");
            }

            // (F) 사이즈 업데이트
            if (requestDto.getSizes() != null) {
                log.debug("사이즈 업데이트 시작 - 요청 사이즈 수: {}", requestDto.getSizes().size());
                List<String> updatedSizes = requestDto.getSizes();

                // 기존 사이즈 목록
                List<String> existingSizes = productColor.getSizes()
                        .stream()
                        .map(ProductSize::getSize)
                        .toList();
                log.debug("기존 사이즈 목록 - 사이즈 수: {}", existingSizes.size());

                // 1) 삭제할 사이즈
                List<String> sizesToRemove = existingSizes.stream()
                        .filter(size -> !updatedSizes.contains(size))
                        .toList();

                log.debug("삭제할 사이즈 목록 - 사이즈 수: {}", sizesToRemove.size());
                for (String size : sizesToRemove) {
                    ProductSize sizeEntity = productColor.getSizes()
                            .stream()
                            .filter(existingSize -> existingSize.getSize().equals(size))
                            .findFirst()
                            .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_SIZE_NOT_FOUND,
                                    "해당 사이즈를 찾을 수 없습니다: " + size));

                    log.debug("사이즈 삭제 - 사이즈: {}, 사이즈ID: {}", size, sizeEntity.getId());
                    productSizeCommandService.deleteProductSize(sizeEntity.getId());
                    productColor.getSizes().remove(sizeEntity);
                }

                // 2) 새로 추가할 사이즈
                List<String> newSizes = updatedSizes.stream()
                        .filter(size -> !existingSizes.contains(size))
                        .toList();

                if (!newSizes.isEmpty()) {
                    log.debug("새 사이즈 추가 - 사이즈 수: {}", newSizes.size());
                    productSizeCommandService.createProductSizes(
                            productColor,
                            productColor.getProduct().getCategory().getId(),
                            newSizes,
                            productColor.getProduct().getReleasePrice()
                    );
                }
                log.debug("사이즈 업데이트 완료");
            }

            // (G) colorName, content 업데이트
            log.debug("색상명 및 내용 업데이트 - 이전 색상명: {}, 새 색상명: {}",
                    productColor.getColorName(), requestDto.getColorName());
            productColor.update(requestDto.getColorName(), requestDto.getContent());

            // 3) 저장
            log.debug("상품 색상 DB 저장 시작");
            productColorRepository.save(productColor);
            log.info("상품 색상 수정 성공 - 색상ID: {}, 색상명: {}", productColorId, productColor.getColorName());
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 색상 수정 중 예상치 못한 오류 발생 - 색상ID: {}", productColorId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_UPDATE_FAILED,
                    "상품 색상 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 색상 삭제
     *
     * @param productColorId 상품 색상 ID
     * @throws ProductException 상품 색상 삭제 실패 시
     */
    @Transactional
    public void deleteProductColor(Long productColorId) {
        log.info("상품 색상 삭제 요청 - 색상ID: {}", productColorId);

        try {
            // 1) 조회
            log.debug("상품 색상 조회 시작 - 색상ID: {}", productColorId);
            ProductColor productColor = productColorRepository.findById(productColorId)
                    .orElseThrow(() -> {
                        log.warn("상품 색상 삭제 실패 - 존재하지 않는 색상ID: {}", productColorId);
                        return new ProductException(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND,
                                "해당 색상을 찾을 수 없습니다: " + productColorId);
                    });
            log.debug("상품 색상 조회 성공 - 색상명: {}", productColor.getColorName());

            // 2) 디렉토리: product/{productId}
            Product product = productColor.getProduct();
            String directory = "product/" + product.getId();
            log.debug("디렉토리 경로 설정 - 경로: {}", directory);

            // 관심 상품 삭제
            log.debug("관심 상품 삭제 시작 - 색상ID: {}", productColorId);
            interestCommandService.deleteAllInterestsByProductColor(productColor);
            log.debug("관심 상품 삭제 완료");

            // 사이즈 삭제
            log.debug("사이즈 삭제 시작 - 색상ID: {}", productColorId);
            productSizeCommandService.deleteAllSizesByProductColor(productColor);
            log.debug("사이즈 삭제 완료");

            // (A) 썸네일 삭제
            if (productColor.getThumbnailImage() != null) {
                log.debug("썸네일 삭제 시작");
                String thumbFileName = productColor.getThumbnailImage().getImageUrl(); // ex) "thumbnail_abc.jpg"
                log.debug("썸네일 파일 삭제 - 파일명: {}", thumbFileName);
                fileUtils.deleteFile(directory, thumbFileName);
                log.debug("썸네일 DB 삭제 - 이미지ID: {}", productColor.getThumbnailImage().getId());
                productImageCommandService.deleteProductImage(productColor.getThumbnailImage().getId());
                log.debug("썸네일 삭제 완료");
            }

            // (B) 일반 이미지 삭제
            if (productImageQueryService.existsByProductColorId(productColorId)) {
                log.debug("일반 이미지 삭제 시작");
                List<ProductImage> images = productImageQueryService.findAllByProductColorId(productColorId);
                log.debug("일반 이미지 조회 완료 - 이미지 수: {}", images.size());

                for (ProductImage image : images) {
                    // ex) "ProductImage_abc.jpg"
                    if (fileUtils.existsFile(directory, image.getImageUrl())) {
                        log.debug("일반 이미지 파일 삭제 - 파일명: {}", image.getImageUrl());
                        fileUtils.deleteFile(directory, image.getImageUrl());
                    }
                    log.debug("일반 이미지 DB 삭제 - 이미지ID: {}", image.getId());
                    productImageCommandService.deleteProductImage(image.getId());
                }
                log.debug("일반 이미지 삭제 완료");
            }

            // (C) 상세 이미지 삭제
            if (productDetailImageQueryService.existsByProductColorId(productColorId)) {
                log.debug("상세 이미지 삭제 시작");
                List<ProductDetailImage> detailImages = productDetailImageQueryService.findAllByProductColorId(productColorId);
                log.debug("상세 이미지 조회 완료 - 이미지 수: {}", detailImages.size());

                for (ProductDetailImage detailImage : detailImages) {
                    // ex) "ProductDetailImage_abc.jpg"
                    if (fileUtils.existsFile(directory, detailImage.getImageUrl())) {
                        log.debug("상세 이미지 파일 삭제 - 파일명: {}", detailImage.getImageUrl());
                        fileUtils.deleteFile(directory, detailImage.getImageUrl());
                    }
                    log.debug("상세 이미지 DB 삭제 - 이미지ID: {}", detailImage.getId());
                    productDetailImageCommandService.deleteProductDetailImage(detailImage.getId());
                }
                log.debug("상세 이미지 삭제 완료");
            }

            // 컬렉션 비움
            log.debug("이미지 컬렉션 비우기");
            productColor.getProductImages().clear();
            productColor.getProductDetailImages().clear();

            // 삭제
            log.debug("상품 색상 DB 삭제 시작");
            productColorRepository.delete(productColor);
            log.info("상품 색상 삭제 성공 - 색상ID: {}", productColorId);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("상품 색상 삭제 중 예상치 못한 오류 발생 - 색상ID: {}", productColorId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_DELETION_FAILED,
                    "상품 색상 삭제 중 오류가 발생했습니다.", e);
        }
    }
}