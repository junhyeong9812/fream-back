package com.fream.back.domain.product.service.productSize;

import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.product.entity.enumType.SizeType;
import com.fream.back.domain.product.repository.ProductSizeRepository;
import com.fream.back.domain.product.service.category.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductSizeCommandService {

    private final ProductSizeRepository productSizeRepository;
    private final CategoryQueryService categoryQueryService; // 카테고리 쿼리 서비스 주입

    public void createProductSizes(ProductColor productColor, Long categoryId, List<String> requestedSizes, int releasePrice) {
        // 최상위 카테고리 찾기
        Category rootCategory = categoryQueryService.findRootCategoryById(categoryId);

        // SizeType 결정
        SizeType sizeType = determineSizeType(rootCategory.getName());

        // 해당 ProductColor의 기존 사이즈 조회
        List<String> existingSizes = productSizeRepository.findAllByProductColorId(productColor.getId())
                .stream()
                .map(ProductSize::getSize)
                .toList();

        // 요청된 사이즈 중 새로운 사이즈 필터링
        List<String> newSizes = requestedSizes.stream()
                .filter(size -> !existingSizes.contains(size)) // 기존 사이즈에 없는 경우만 필터링
                .toList();

        // 새로운 사이즈 생성
        newSizes.forEach(size -> {
            if (isValidSize(size, sizeType)) {
                ProductSize productSize = ProductSize.builder()
                        .size(size)
                        .sizeType(sizeType)
                        .purchasePrice(releasePrice)
                        .salePrice(releasePrice)
                        .quantity(0)
                        .productColor(productColor)
                        .build();
                productSizeRepository.save(productSize);
            } else {
                throw new IllegalArgumentException("유효하지 않은 사이즈입니다: " + size);
            }
        });
    }


    private SizeType determineSizeType(String rootCategoryName) {
        switch (rootCategoryName.toUpperCase()) {
            case "CLOTHING":
                return SizeType.CLOTHING;
            case "SHOES":
                return SizeType.SHOES;
            case "ACCESSORIES":
                return SizeType.ACCESSORIES;
            default:
                throw new IllegalArgumentException("해당 카테고리에 맞는 SizeType이 존재하지 않습니다.");
        }
    }

    private boolean isValidSize(String size, SizeType sizeType) {
        return Arrays.asList(sizeType.getSizes()).contains(size);
    }
    public void deleteProductSize(Long sizeId) {
        ProductSize productSize = productSizeRepository.findById(sizeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사이즈를 찾을 수 없습니다."));
        System.out.println("DeleteProductSize = " + productSize);
        productSizeRepository.delete(productSize);
    }
    public void updateProductSize(Long sizeId, int purchasePrice, int salePrice, int quantity) {
        ProductSize productSize = productSizeRepository.findById(sizeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사이즈를 찾을 수 없습니다."));

        productSize.update(purchasePrice, salePrice, quantity);
    }
    @Transactional
    public void deleteAllSizesByProductColor(ProductColor productColor) {
        List<ProductSize> sizes = productSizeRepository.findAllByProductColorId(productColor.getId());
        sizes.forEach(size -> {
            deleteProductSize(size.getId());
        });

        // 영속성 컨텍스트를 강제로 플러시
//        productSizeRepository.deleteAll(sizes);
        productSizeRepository.flush();
        productColor.getSizes().clear();
    }


}