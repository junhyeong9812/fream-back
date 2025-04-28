package com.fream.back.domain.product.service.category;

import com.fream.back.domain.product.dto.CategoryRequestDto;
import com.fream.back.domain.product.dto.CategoryResponseDto;
import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.CategoryRepository;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 명령(Command) 서비스
 * 카테고리의 생성, 수정, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * 카테고리 생성
     *
     * @param request 카테고리 생성 요청 DTO
     * @return 생성된 카테고리 응답 DTO
     * @throws ProductException 카테고리 생성 실패 시
     */
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        log.info("카테고리 생성 요청 - 메인 카테고리: {}, 서브 카테고리: {}",
                request.getMainCategoryName(),
                request.getSubCategoryName() != null ? request.getSubCategoryName() : "없음");

        try {
            // 상위 카테고리가 없는 경우 (메인 카테고리)
            if (request.getSubCategoryName() == null) {
                log.debug("메인 카테고리 생성 시작 - 메인 카테고리: {}", request.getMainCategoryName());

                if (categoryRepository.existsByNameAndParentCategoryIsNull(request.getMainCategoryName())) {
                    log.warn("메인 카테고리 생성 실패 - 이미 존재하는 메인 카테고리: {}", request.getMainCategoryName());
                    throw new ProductException(ProductErrorCode.CATEGORY_ALREADY_EXISTS,
                            "이미 존재하는 메인 카테고리 이름입니다: " + request.getMainCategoryName());
                }

                Category mainCategory = categoryRepository.save(
                        Category.builder()
                                .name(request.getMainCategoryName())
                                .build());

                log.info("메인 카테고리 생성 성공 - 카테고리ID: {}, 카테고리명: {}",
                        mainCategory.getId(), mainCategory.getName());
                return CategoryResponseDto.fromEntity(mainCategory);
            }

            // 상위 카테고리가 있는 경우 (서브 카테고리)
            log.debug("서브 카테고리 생성 시작 - 메인 카테고리: {}, 서브 카테고리: {}",
                    request.getMainCategoryName(), request.getSubCategoryName());

            Category mainCategory = categoryRepository.findByNameAndParentCategoryIsNull(request.getMainCategoryName())
                    .orElseThrow(() -> {
                        log.warn("서브 카테고리 생성 실패 - 존재하지 않는 메인 카테고리: {}", request.getMainCategoryName());
                        return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                                "상위 카테고리가 존재하지 않습니다: " + request.getMainCategoryName());
                    });

            if (categoryRepository.existsByNameAndParentCategory(request.getSubCategoryName(), mainCategory)) {
                log.warn("서브 카테고리 생성 실패 - 이미 존재하는 서브 카테고리: {}, 메인 카테고리: {}",
                        request.getSubCategoryName(), request.getMainCategoryName());
                throw new ProductException(ProductErrorCode.CATEGORY_ALREADY_EXISTS,
                        "같은 상위 카테고리 아래에 동일한 이름의 서브 카테고리가 존재합니다: " + request.getSubCategoryName());
            }

            Category subCategory = Category.builder()
                    .name(request.getSubCategoryName())
                    .parentCategory(mainCategory)
                    .build();

            categoryRepository.save(subCategory);

            log.info("서브 카테고리 생성 성공 - 카테고리ID: {}, 카테고리명: {}, 상위 카테고리: {}",
                    subCategory.getId(), subCategory.getName(), mainCategory.getName());
            return CategoryResponseDto.fromEntity(subCategory);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("카테고리 생성 중 예상치 못한 오류 발생 - 메인 카테고리: {}", request.getMainCategoryName(), e);
            throw new ProductException(ProductErrorCode.CATEGORY_CREATION_FAILED, "카테고리 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리 수정
     *
     * @param id 카테고리 ID
     * @param request 카테고리 수정 요청 DTO
     * @return 수정된 카테고리 응답 DTO
     * @throws ProductException 카테고리 수정 실패 시
     */
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        log.info("카테고리 수정 요청 - 카테고리ID: {}, 메인 카테고리: {}, 서브 카테고리: {}",
                id,
                request.getMainCategoryName(),
                request.getSubCategoryName() != null ? request.getSubCategoryName() : "없음");

        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("카테고리 수정 실패 - 존재하지 않는 카테고리ID: {}", id);
                        return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                                "존재하지 않는 카테고리입니다. ID: " + id);
                    });

            // 메인 카테고리명 변경 요청이 있는 경우
            if (request.getMainCategoryName() != null) {
                // 자신이 메인 카테고리인 경우
                if (category.getParentCategory() == null) {
                    if (!category.getName().equals(request.getMainCategoryName()) &&
                            categoryRepository.existsByNameAndParentCategoryIsNull(request.getMainCategoryName())) {
                        log.warn("카테고리 수정 실패 - 이미 존재하는 메인 카테고리명: {}", request.getMainCategoryName());
                        throw new ProductException(ProductErrorCode.CATEGORY_ALREADY_EXISTS,
                                "이미 존재하는 메인 카테고리 이름입니다: " + request.getMainCategoryName());
                    }

                    log.debug("메인 카테고리명 변경 - 이전: {}, 이후: {}",
                            category.getName(), request.getMainCategoryName());
                    category.updateName(request.getMainCategoryName());
                }
                // 자신이 서브 카테고리인 경우, 부모 카테고리명 변경
                else {
                    Category parentCategory = category.getParentCategory();
                    if (!parentCategory.getName().equals(request.getMainCategoryName()) &&
                            categoryRepository.existsByNameAndParentCategoryIsNull(request.getMainCategoryName())) {
                        log.warn("카테고리 수정 실패 - 이미 존재하는 메인 카테고리명: {}", request.getMainCategoryName());
                        throw new ProductException(ProductErrorCode.CATEGORY_ALREADY_EXISTS,
                                "이미 존재하는 메인 카테고리 이름입니다: " + request.getMainCategoryName());
                    }

                    log.debug("부모 카테고리명 변경 - 이전: {}, 이후: {}",
                            parentCategory.getName(), request.getMainCategoryName());
                    parentCategory.updateName(request.getMainCategoryName());
                }
            }

            // 서브 카테고리명 변경 요청이 있고, 자신이 서브 카테고리인 경우
            if (request.getSubCategoryName() != null && category.getParentCategory() != null) {
                if (!category.getName().equals(request.getSubCategoryName()) &&
                        categoryRepository.existsByNameAndParentCategory(
                                request.getSubCategoryName(), category.getParentCategory())) {
                    log.warn("카테고리 수정 실패 - 이미 존재하는 서브 카테고리명: {}, 메인 카테고리: {}",
                            request.getSubCategoryName(), category.getParentCategory().getName());
                    throw new ProductException(ProductErrorCode.CATEGORY_ALREADY_EXISTS,
                            "같은 상위 카테고리 아래에 동일한 이름의 서브 카테고리가 존재합니다: " + request.getSubCategoryName());
                }

                log.debug("서브 카테고리명 변경 - 이전: {}, 이후: {}",
                        category.getName(), request.getSubCategoryName());
                category.updateName(request.getSubCategoryName());
            }

            log.info("카테고리 수정 성공 - 카테고리ID: {}", id);
            return CategoryResponseDto.fromEntity(category);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("카테고리 수정 중 예상치 못한 오류 발생 - 카테고리ID: {}", id, e);
            throw new ProductException(ProductErrorCode.CATEGORY_UPDATE_FAILED, "카테고리 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리 삭제
     *
     * @param id 카테고리 ID
     * @throws ProductException 카테고리 삭제 실패 시
     */
    public void deleteCategory(Long id) {
        log.info("카테고리 삭제 요청 - 카테고리ID: {}", id);

        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("카테고리 삭제 실패 - 존재하지 않는 카테고리ID: {}", id);
                        return new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND,
                                "존재하지 않는 카테고리입니다. ID: " + id);
                    });

            // 하위 카테고리가 있는 경우 확인
            if (categoryRepository.existsByParentCategory(category)) {
                log.warn("카테고리 삭제 실패 - 하위 카테고리가 존재하는 카테고리ID: {}", id);
                throw new ProductException(ProductErrorCode.CATEGORY_IN_USE,
                        "하위 카테고리를 먼저 삭제해야 합니다.");
            }

            // 상품이 포함된 경우 확인
            boolean hasAssociatedProducts = productRepository.existsByCategory(category);
            if (hasAssociatedProducts) {
                log.warn("카테고리 삭제 실패 - 연관된 상품이 존재하는 카테고리ID: {}", id);
                throw new ProductException(ProductErrorCode.CATEGORY_IN_USE,
                        "포함된 상품을 먼저 삭제해야 합니다.");
            }

            categoryRepository.delete(category);
            log.info("카테고리 삭제 성공 - 카테고리ID: {}, 카테고리명: {}", id, category.getName());
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("카테고리 삭제 중 예상치 못한 오류 발생 - 카테고리ID: {}", id, e);
            throw new ProductException(ProductErrorCode.CATEGORY_DELETION_FAILED, "카테고리 삭제 중 오류가 발생했습니다.", e);
        }
    }
}