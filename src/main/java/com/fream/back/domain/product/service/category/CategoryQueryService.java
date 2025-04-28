package com.fream.back.domain.product.service.category;

import com.fream.back.domain.product.dto.CategoryResponseDto;
import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 조회(Query) 서비스
 * 카테고리 조회 관련 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;

    /**
     * 모든 메인 카테고리 조회
     *
     * @return 메인 카테고리 응답 DTO 목록
     */
    public List<CategoryResponseDto> findAllMainCategories() {
        log.info("모든 메인 카테고리 조회 요청");

        try {
            List<Category> mainCategories = categoryRepository.findByParentCategoryIsNullOrderByNameDesc();

            List<CategoryResponseDto> categoryDtos = mainCategories.stream()
                    .map(CategoryResponseDto::fromEntity) // 엔티티 -> DTO 변환
                    .collect(Collectors.toList());

            log.info("모든 메인 카테고리 조회 성공 - 카테고리 수: {}", categoryDtos.size());
            return categoryDtos;
        } catch (Exception e) {
            log.error("모든 메인 카테고리 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                    "메인 카테고리 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 메인 카테고리에 대한 서브 카테고리 목록 조회
     *
     * @param mainCategoryName 메인 카테고리명
     * @return 서브 카테고리 응답 DTO 목록
     * @throws ProductException 메인 카테고리가 존재하지 않을 경우
     */
    public List<CategoryResponseDto> findSubCategoriesByMainCategory(String mainCategoryName) {
        log.info("메인 카테고리의 서브 카테고리 목록 조회 - 메인 카테고리: {}", mainCategoryName);

        try {
            Category mainCategory = categoryRepository.findByNameAndParentCategoryIsNull(mainCategoryName)
                    .orElseThrow(() -> {
                        log.warn("메인 카테고리 조회 실패 - 존재하지 않는 메인 카테고리: {}", mainCategoryName);
                        return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                                "메인 카테고리가 존재하지 않습니다: " + mainCategoryName);
                    });

            List<Category> subCategories = categoryRepository.findByParentCategoryOrderByNameDesc(mainCategory);

            List<CategoryResponseDto> categoryDtos = subCategories.stream()
                    .map(CategoryResponseDto::fromEntity) // 엔티티 -> DTO 변환
                    .collect(Collectors.toList());

            log.info("메인 카테고리의 서브 카테고리 목록 조회 성공 - 메인 카테고리: {}, 서브 카테고리 수: {}",
                    mainCategoryName, categoryDtos.size());
            return categoryDtos;
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("메인 카테고리의 서브 카테고리 목록 조회 중 예상치 못한 오류 발생 - 메인 카테고리: {}",
                    mainCategoryName, e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                    "서브 카테고리 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 최상위 카테고리 조회
     * 카테고리의 부모를 재귀적으로 탐색하여 최상위 카테고리를 반환합니다.
     *
     * @param category 카테고리 엔티티
     * @return 최상위 카테고리 엔티티
     */
    public Category findRootCategory(Category category) {
        log.debug("최상위 카테고리 조회 - 카테고리ID: {}, 카테고리명: {}",
                category.getId(), category.getName());

        Category current = category;
        while (current.getParentCategory() != null) {
            current = current.getParentCategory();
            log.debug("상위 카테고리로 이동 - 카테고리ID: {}, 카테고리명: {}",
                    current.getId(), current.getName());
        }

        log.debug("최상위 카테고리 조회 완료 - 카테고리ID: {}, 카테고리명: {}",
                current.getId(), current.getName());
        return current;
    }

    /**
     * ID로 카테고리 조회 및 최상위 카테고리 반환
     *
     * @param categoryId 카테고리 ID
     * @return 최상위 카테고리 엔티티
     * @throws ProductException 카테고리가 존재하지 않을 경우
     */
    public Category findRootCategoryById(Long categoryId) {
        log.info("ID로 카테고리 조회 및 최상위 카테고리 반환 - 카테고리ID: {}", categoryId);

        try {
            Category category = categoryRepository.findWithParentById(categoryId)
                    .orElseThrow(() -> {
                        log.warn("카테고리 조회 실패 - 존재하지 않는 카테고리ID: {}", categoryId);
                        return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                                "해당 카테고리를 찾을 수 없습니다. ID: " + categoryId);
                    });

            Category rootCategory = findRootCategory(category);

            log.info("최상위 카테고리 반환 완료 - 카테고리ID: {}, 카테고리명: {}",
                    rootCategory.getId(), rootCategory.getName());
            return rootCategory;
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("ID로 카테고리 조회 및 최상위 카테고리 반환 중 예상치 못한 오류 발생 - 카테고리ID: {}",
                    categoryId, e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                    "카테고리 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 부모 카테고리 ID로 서브 카테고리 조회
     *
     * @param parentId 부모 카테고리 ID
     * @return 서브 카테고리 응답 DTO 목록
     */
    public List<CategoryResponseDto> findSubCategoriesByParentId(Long parentId) {
        log.info("부모 카테고리 ID로 서브 카테고리 조회 - 부모 카테고리ID: {}", parentId);

        try {
            List<Category> subCategories = categoryRepository.findByParentCategoryId(parentId);

            List<CategoryResponseDto> categoryDtos = subCategories.stream()
                    .map(CategoryResponseDto::fromEntity)
                    .collect(Collectors.toList());

            log.info("부모 카테고리 ID로 서브 카테고리 조회 성공 - 부모 카테고리ID: {}, 서브 카테고리 수: {}",
                    parentId, categoryDtos.size());
            return categoryDtos;
        } catch (Exception e) {
            log.error("부모 카테고리 ID로 서브 카테고리 조회 중 예상치 못한 오류 발생 - 부모 카테고리ID: {}",
                    parentId, e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                    "서브 카테고리 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 부모 카테고리명으로 서브 카테고리 조회
     *
     * @param parentName 부모 카테고리명
     * @return 서브 카테고리 응답 DTO 목록
     */
    public List<CategoryResponseDto> findSubCategoriesByParentName(String parentName) {
        log.info("부모 카테고리명으로 서브 카테고리 조회 - 부모 카테고리명: {}", parentName);

        try {
            List<Category> subCategories = categoryRepository.findByParentCategoryName(parentName);

            List<CategoryResponseDto> categoryDtos = subCategories.stream()
                    .map(CategoryResponseDto::fromEntity)
                    .collect(Collectors.toList());

            log.info("부모 카테고리명으로 서브 카테고리 조회 성공 - 부모 카테고리명: {}, 서브 카테고리 수: {}",
                    parentName, categoryDtos.size());
            return categoryDtos;
        } catch (Exception e) {
            log.error("부모 카테고리명으로 서브 카테고리 조회 중 예상치 못한 오류 발생 - 부모 카테고리명: {}",
                    parentName, e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                    "서브 카테고리 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}