package com.fream.back.domain.product.service.productColor;

import com.fream.back.domain.product.dto.ProductColorCreateRequestDto;
import com.fream.back.domain.product.dto.ProductColorUpdateRequestDto;
import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.product.entity.enumType.ColorType;
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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
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
     * ProductColor 생성
     * @param requestDto  색상·컨텐츠·사이즈 등 정보
     * @param productImage  (단일) 썸네일 이미지
     * @param productImages (다중) 일반 이미지
     * @param productDetailImages (다중) 상세 이미지
     * @param productId 상품 ID
     * @return 생성된 ProductColor ID
     */
    public Long createProductColor(
            ProductColorCreateRequestDto requestDto,
            MultipartFile productImage,
            List<MultipartFile> productImages,
            List<MultipartFile> productDetailImages,
            Long productId
    ) {
        // 1) 색상 유효성 검증
        validateColorType(requestDto.getColorName());

        // 2) 상품 조회
        Product product = productEntityService.findById(productId);

        // 3) ProductColor 엔티티 생성
        ProductColor productColor = ProductColor.builder()
                .colorName(requestDto.getColorName())
                .content(requestDto.getContent())
                .product(product)
                .build();

        // 4) 디렉토리: "product/{productId}"
        String directory = "product/" + product.getId();

        // (A) 썸네일 이미지 저장
        if (productImage != null && !productImage.isEmpty()) {
            // saveFile → "thumbnail_xxx.jpg"
            String thumbnailFilename = fileUtils.saveFile(directory, "thumbnail_", productImage);
            // DB에는 파일명만 저장
            ProductImage thumbnail = ProductImage.builder()
                    .imageUrl(thumbnailFilename) // ex) "thumbnail_abc.jpg"
                    .build();
            productColor.addThumbnailImage(thumbnail);
        }

        // (B) 일반 이미지 (다중)
        if (productImages != null && !productImages.isEmpty()) {
            for (MultipartFile file : productImages) {
                String imageFilename = fileUtils.saveFile(directory, "ProductImage_", file);
                ProductImage productImageEntity = ProductImage.builder()
                        .imageUrl(imageFilename)
                        .build();
                productColor.addProductImage(productImageEntity);
            }
        }

        // (C) 상세 이미지 (다중)
        if (productDetailImages != null && !productDetailImages.isEmpty()) {
            for (MultipartFile file : productDetailImages) {
                String detailFilename = fileUtils.saveFile(directory, "ProductDetailImage_", file);
                ProductDetailImage detailImage = ProductDetailImage.builder()
                        .imageUrl(detailFilename)
                        .build();
                productColor.addProductDetailImage(detailImage);
            }
        }

        // 5) DB 저장
        ProductColor savedColor = productColorRepository.save(productColor);

        // 6) 사이즈 생성
        productSizeCommandService.createProductSizes(
                savedColor,
                product.getCategory().getId(),
                requestDto.getSizes(),
                product.getReleasePrice()
        );

        return savedColor.getId();
    }

    private void validateColorType(String colorName) {
        boolean isValid = Arrays.stream(ColorType.values())
                .anyMatch(colorType ->
                        colorType.name().equalsIgnoreCase(colorName.trim()) ||
                                colorType.getDisplayName().equals(colorName.trim())
                ); // 두 가지 방식 비교
        if (!isValid) {
            throw new IllegalArgumentException("유효하지 않은 색상입니다: " + colorName);
        }
    }

    /**
     * ProductColor 수정
     */
    @Transactional
    public void updateProductColor(
            Long productColorId,
            ProductColorUpdateRequestDto requestDto,
            MultipartFile thumbnailImage,       // 새 썸네일
            List<MultipartFile> newImages,       // 새 일반 이미지
            List<MultipartFile> newDetailImages  // 새 상세 이미지
    ) {
        // 1) ProductColor 조회
        ProductColor productColor = productColorRepository.findById(productColorId)
                .orElseThrow(() -> new IllegalArgumentException("해당 색상을 찾을 수 없습니다."));

        // 2) 디렉토리: "product/{productId}"
        Product product = productColor.getProduct();
        String directory = "product/" + product.getId();

        // (A) 썸네일 교체
        if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            if (productColor.getThumbnailImage() != null) {
                // 기존 파일 삭제
                String oldThumb = productColor.getThumbnailImage().getImageUrl(); // ex) "thumbnail_abc.jpg"
                fileUtils.deleteFile(directory, oldThumb);
            }
            // 새 파일 저장
            String newThumbFile = fileUtils.saveFile(directory, "thumbnail_", thumbnailImage);
            ProductImage newThumbnail = ProductImage.builder()
                    .imageUrl(newThumbFile)
                    .build();
            productColor.addThumbnailImage(newThumbnail);
        }

        // (B) 기존 일반 이미지 삭제 (requestDto.getExistingImages()만 남긴다)
        if (requestDto.getExistingImages() != null) {
            productColor.getProductImages().removeIf(image -> {
                if (!requestDto.getExistingImages().contains(image.getImageUrl())) {
                    // 파일 삭제
                    fileUtils.deleteFile(directory, image.getImageUrl());
                    return true;
                }
                return false;
            });
        }

        // (C) 새 일반 이미지 추가
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String newImageFile = fileUtils.saveFile(directory, "ProductImage_", file);
                ProductImage newImageEntity = ProductImage.builder()
                        .imageUrl(newImageFile)
                        .build();
                productColor.addProductImage(newImageEntity);
            }
        }

        // (D) 기존 상세 이미지 삭제
        if (requestDto.getExistingDetailImages() != null) {
            productColor.getProductDetailImages().removeIf(detailImage -> {
                if (!requestDto.getExistingDetailImages().contains(detailImage.getImageUrl())) {
                    fileUtils.deleteFile(directory, detailImage.getImageUrl());
                    return true;
                }
                return false;
            });
        }

        // (E) 새 상세 이미지 추가
        if (newDetailImages != null && !newDetailImages.isEmpty()) {
            for (MultipartFile file : newDetailImages) {
                String detailFilename = fileUtils.saveFile(directory, "ProductDetailImage_", file);
                ProductDetailImage newDetailImage = ProductDetailImage.builder()
                        .imageUrl(detailFilename)
                        .build();
                productColor.addProductDetailImage(newDetailImage);
            }
        }

        // (F) 사이즈 업데이트
        if (requestDto.getSizes() != null) {
            List<String> updatedSizes = requestDto.getSizes();

            // 기존 사이즈 목록
            List<String> existingSizes = productColor.getSizes()
                    .stream()
                    .map(ProductSize::getSize)
                    .toList();

            // 1) 삭제할 사이즈
            existingSizes.stream()
                    .filter(size -> !updatedSizes.contains(size))
                    .forEach(size -> {
                        ProductSize sizeEntity = productColor.getSizes()
                                .stream()
                                .filter(existingSize -> existingSize.getSize().equals(size))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("해당 사이즈를 찾을 수 없습니다: " + size));

                        productSizeCommandService.deleteProductSize(sizeEntity.getId());
                        productColor.getSizes().remove(sizeEntity);
                    });

            // 2) 새로 추가할 사이즈
            List<String> newSizes = updatedSizes.stream()
                    .filter(size -> !existingSizes.contains(size))
                    .toList();

            if (!newSizes.isEmpty()) {
                productSizeCommandService.createProductSizes(
                        productColor,
                        productColor.getProduct().getCategory().getId(),
                        newSizes,
                        productColor.getProduct().getReleasePrice()
                );
            }
        }

        // (G) colorName, content 업데이트
        productColor.update(requestDto.getColorName(), requestDto.getContent());

        // 3) 저장
        productColorRepository.save(productColor);
    }

    /**
     * ProductColor 삭제
     */
    @Transactional
    public void deleteProductColor(Long productColorId) {
        // 1) 조회
        ProductColor productColor = productColorRepository.findById(productColorId)
                .orElseThrow(() -> new IllegalArgumentException("해당 색상을 찾을 수 없습니다."));

        // 2) 디렉토리: product/{productId}
        Product product = productColor.getProduct();
        String directory = "product/" + product.getId();

        // 관심 상품 삭제
        interestCommandService.deleteAllInterestsByProductColor(productColor);

        // 사이즈 삭제
        productSizeCommandService.deleteAllSizesByProductColor(productColor);

        // (A) 썸네일 삭제
        if (productColor.getThumbnailImage() != null) {
            String thumbFileName = productColor.getThumbnailImage().getImageUrl(); // ex) "thumbnail_abc.jpg"
            fileUtils.deleteFile(directory, thumbFileName);
            productImageCommandService.deleteProductImage(productColor.getThumbnailImage().getId());
        }

        // (B) 일반 이미지 삭제
        if (productImageQueryService.existsByProductColorId(productColorId)) {
            productImageQueryService.findAllByProductColorId(productColorId).forEach(image -> {
                // ex) "ProductImage_abc.jpg"
                if (fileUtils.existsFile(directory, image.getImageUrl())) {
                    fileUtils.deleteFile(directory, image.getImageUrl());
                }
                productImageCommandService.deleteProductImage(image.getId());
            });
        }

        // (C) 상세 이미지 삭제
        if (productDetailImageQueryService.existsByProductColorId(productColorId)) {
            productDetailImageQueryService.findAllByProductColorId(productColorId).forEach(detailImage -> {
                // ex) "ProductDetailImage_abc.jpg"
                if (fileUtils.existsFile(directory, detailImage.getImageUrl())) {
                    fileUtils.deleteFile(directory, detailImage.getImageUrl());
                }
                productDetailImageCommandService.deleteProductDetailImage(detailImage.getId());
            });
        }

        // 컬렉션 비움
        productColor.getProductImages().clear();
        productColor.getProductDetailImages().clear();

        // 삭제
        productColorRepository.delete(productColor);
    }


}