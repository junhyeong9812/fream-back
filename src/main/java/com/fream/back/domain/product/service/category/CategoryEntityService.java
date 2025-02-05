package com.fream.back.domain.product.service.category;

import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryEntityService {

    private final CategoryRepository categoryRepository;

    public Category findMainCategoryByName(String name) {
        return categoryRepository.findByNameAndParentCategoryIsNull(name)
                .orElseThrow(() -> new IllegalArgumentException("메인 카테고리가 존재하지 않습니다."));
    }

    public Category findSubCategoryByName(String name, String mainCategoryName) {
        Category mainCategory = findMainCategoryByName(mainCategoryName);
        return categoryRepository.findByNameAndParentCategory(name, mainCategory)
                .orElseThrow(() -> new IllegalArgumentException("서브 카테고리가 존재하지 않습니다."));
    }
}