package com.fream.back.domain.product.service.category;

import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 엔티티 서비스
 * 카테고리 엔티티를 조회하는 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CategoryEntityService {

    private final CategoryRepository categoryRepository;

    /**
     * 이름으로 메인 카테고리 조회
     *
     * @param name 카테고리명
     * @return 메인 카테고리 엔티티
     * @throws ProductException 메인 카테고리가 존재하지 않을 경우
     */
    public Category findMainCategoryByName(String name) {
        log.debug("이름으로 메인 카테고리 조회 - 카테고리명: {}", name);

        return categoryRepository.findByNameAndParentCategoryIsNull(name)
                .orElseThrow(() -> {
                    log.warn("메인 카테고리 조회 실패 - 존재하지 않는 카테고리명: {}", name);
                    return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                            "메인 카테고리가 존재하지 않습니다: " + name);
                });
    }

    /**
     * 이름과 메인 카테고리명으로 서브 카테고리 조회
     *
     * @param name 서브 카테고리명
     * @param mainCategoryName 메인 카테고리명
     * @return 서브 카테고리 엔티티
     * @throws ProductException 메인 카테고리 또는 서브 카테고리가 존재하지 않을 경우
     */
    public Category findSubCategoryByName(String name, String mainCategoryName) {
        log.debug("이름과 메인 카테고리명으로 서브 카테고리 조회 - 서브 카테고리명: {}, 메인 카테고리명: {}",
                name, mainCategoryName);

        try {
            Category mainCategory = findMainCategoryByName(mainCategoryName);

            return categoryRepository.findByNameAndParentCategory(name, mainCategory)
                    .orElseThrow(() -> {
                        log.warn("서브 카테고리 조회 실패 - 존재하지 않는 서브 카테고리명: {}, 메인 카테고리명: {}",
                                name, mainCategoryName);
                        return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                                "서브 카테고리가 존재하지 않습니다: " + name);
                    });
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("서브 카테고리 조회 중 예상치 못한 오류 발생 - 서브 카테고리명: {}, 메인 카테고리명: {}",
                    name, mainCategoryName, e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                    "카테고리 조회 중 오류가 발생했습니다.", e);
        }
    }
}