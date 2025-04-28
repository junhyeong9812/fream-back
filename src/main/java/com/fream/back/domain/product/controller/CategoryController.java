package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.CategoryRequestDto;
import com.fream.back.domain.product.dto.CategoryResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.category.CategoryCommandService;
import com.fream.back.domain.product.service.category.CategoryQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카테고리 관련 컨트롤러
 * 카테고리의 생성, 수정, 삭제, 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;
    private final UserQueryService userQueryService;

    /**
     * 관리자 권한 확인용 메서드
     * 로그인된 사용자의 이메일을 반환합니다.
     *
     * @return 사용자 이메일
     * @throws ProductException 인증된 사용자가 없는 경우
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new ProductException(ProductErrorCode.CATEGORY_CREATION_FAILED, "인증된 사용자가 없습니다.");
    }

    /**
     * 카테고리 생성 API
     *
     * @param request 카테고리 생성 요청 DTO
     * @return 생성된 카테고리 정보
     */
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(@RequestBody CategoryRequestDto request) {
        log.info("카테고리 생성 요청 - 메인 카테고리: {}, 서브 카테고리: {}",
                request.getMainCategoryName(),
                request.getSubCategoryName() != null ? request.getSubCategoryName() : "없음");

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            CategoryResponseDto response = categoryCommandService.createCategory(request);

            log.info("카테고리 생성 성공 - 카테고리ID: {}, 카테고리명: {}",
                    response.getId(),
                    response.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("카테고리 생성 실패 - 메인 카테고리: {}, 오류: {}",
                    request.getMainCategoryName(),
                    e.getMessage(), e);
            throw new ProductException(ProductErrorCode.CATEGORY_CREATION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("카테고리 생성 중 예상치 못한 오류 발생 - 메인 카테고리: {}",
                    request.getMainCategoryName(), e);
            throw new ProductException(ProductErrorCode.CATEGORY_CREATION_FAILED, "카테고리 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리 수정 API
     *
     * @param id 카테고리 ID
     * @param request 카테고리 수정 요청 DTO
     * @return 수정된 카테고리 정보
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable("categoryId") Long id,
            @RequestBody CategoryRequestDto request) {
        log.info("카테고리 수정 요청 - 카테고리ID: {}, 메인 카테고리: {}, 서브 카테고리: {}",
                id,
                request.getMainCategoryName(),
                request.getSubCategoryName() != null ? request.getSubCategoryName() : "없음");

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            CategoryResponseDto response = categoryCommandService.updateCategory(id, request);

            log.info("카테고리 수정 성공 - 카테고리ID: {}, 카테고리명: {}",
                    id,
                    response.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("카테고리 수정 실패 - 카테고리ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.CATEGORY_UPDATE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("카테고리 수정 중 예상치 못한 오류 발생 - 카테고리ID: {}", id, e);
            throw new ProductException(ProductErrorCode.CATEGORY_UPDATE_FAILED, "카테고리 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리 삭제 API
     *
     * @param id 카테고리 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("categoryId") Long id) {
        log.info("카테고리 삭제 요청 - 카테고리ID: {}", id);

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            categoryCommandService.deleteCategory(id);

            log.info("카테고리 삭제 성공 - 카테고리ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("카테고리 삭제 실패 - 카테고리ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.CATEGORY_DELETION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("카테고리 삭제 중 예상치 못한 오류 발생 - 카테고리ID: {}", id, e);
            throw new ProductException(ProductErrorCode.CATEGORY_DELETION_FAILED, "카테고리 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 메인 카테고리 조회 API
     *
     * @return 메인 카테고리 목록
     */
    @GetMapping("/main")
    public ResponseEntity<List<CategoryResponseDto>> getAllMainCategories() {
        log.info("모든 메인 카테고리 조회 요청");

        try {
            List<CategoryResponseDto> categories = categoryQueryService.findAllMainCategories();

            log.info("메인 카테고리 조회 성공 - 카테고리 수: {}", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("메인 카테고리 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND, "카테고리 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 메인 카테고리의 서브 카테고리 조회 API
     *
     * @param mainCategoryName 메인 카테고리명
     * @return 서브 카테고리 목록
     */
    @GetMapping("/sub/{mainCategoryName}")
    public ResponseEntity<List<CategoryResponseDto>> getSubCategoriesByMain(
            @PathVariable("mainCategoryName") String mainCategoryName) {
        log.info("서브 카테고리 조회 요청 - 메인 카테고리: {}", mainCategoryName);

        try {
            List<CategoryResponseDto> categories = categoryQueryService.findSubCategoriesByMainCategory(mainCategoryName);

            log.info("서브 카테고리 조회 성공 - 메인 카테고리: {}, 서브 카테고리 수: {}",
                    mainCategoryName,
                    categories.size());
            return ResponseEntity.ok(categories);
        } catch (IllegalArgumentException e) {
            log.error("서브 카테고리 조회 실패 - 메인 카테고리: {}, 오류: {}",
                    mainCategoryName,
                    e.getMessage(), e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("서브 카테고리 조회 중 예상치 못한 오류 발생 - 메인 카테고리: {}",
                    mainCategoryName, e);
            throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND, "카테고리 조회 중 오류가 발생했습니다.", e);
        }
    }
}