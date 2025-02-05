package com.fream.back.domain.product.service.category;

import com.fream.back.domain.product.dto.CategoryRequestDto;
import com.fream.back.domain.product.dto.CategoryResponseDto;
import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.repository.CategoryRepository;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;


    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        // 상위 카테고리가 없는 경우 (메인 카테고리)
        if (request.getSubCategoryName() == null) {
            if (categoryRepository.existsByNameAndParentCategoryIsNull(request.getMainCategoryName())) {
                throw new IllegalArgumentException("이미 존재하는 메인 카테고리 이름입니다.");
            }
            Category mainCategory = categoryRepository.save(Category.builder().name(request.getMainCategoryName()).build());
            return CategoryResponseDto.fromEntity(mainCategory);
        }

        // 상위 카테고리가 있는 경우 (서브 카테고리)
        Category mainCategory = categoryRepository.findByNameAndParentCategoryIsNull(request.getMainCategoryName())
                .orElseThrow(() -> new IllegalArgumentException("상위 카테고리가 존재하지 않습니다."));

        if (categoryRepository.existsByNameAndParentCategory(request.getSubCategoryName(), mainCategory)) {
            throw new IllegalArgumentException("같은 상위 카테고리 아래에 동일한 이름의 서브 카테고리가 존재합니다.");
        }

        Category subCategory = Category.builder()
                .name(request.getSubCategoryName())
                .parentCategory(mainCategory)
                .build();
        categoryRepository.save(subCategory);
        return CategoryResponseDto.fromEntity(subCategory);
    }

    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (request.getMainCategoryName() != null) {
            category.updateName(request.getMainCategoryName());
        }
        if (request.getSubCategoryName() != null && category.getParentCategory() != null) {
            category.getParentCategory().updateName(request.getSubCategoryName());
        }
        return CategoryResponseDto.fromEntity(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        // 상품이 포함된 경우 예외 발생
        boolean hasAssociatedProducts = productRepository.existsByCategory(category);
        if (hasAssociatedProducts) {
            throw new IllegalArgumentException("포함된 상품을 먼저 삭제해야 합니다.");
        }

        categoryRepository.delete(category);
    }

}
