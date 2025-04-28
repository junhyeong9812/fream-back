package com.fream.back.domain.product.controller.command;

import com.fream.back.domain.product.dto.ProductCreateRequestDto;
import com.fream.back.domain.product.dto.ProductCreateResponseDto;
import com.fream.back.domain.product.dto.ProductUpdateRequestDto;
import com.fream.back.domain.product.dto.ProductUpdateResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.product.ProductCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 관련 명령 컨트롤러
 * 상품의 생성, 수정, 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/products/command")
@RequiredArgsConstructor
@Slf4j
public class ProductCommandController {

    private final ProductCommandService productCommandService;
    private final UserQueryService userQueryService; // 권한 확인 서비스

    /**
     * SecurityContext에서 이메일 추출
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
        throw new ProductException(ProductErrorCode.PRODUCT_CREATION_FAILED, "인증된 사용자가 없습니다.");
    }

    /**
     * 상품 생성 API
     *
     * @param request 상품 생성 요청 DTO
     * @return 생성된 상품 정보
     */
    @PostMapping
    public ResponseEntity<ProductCreateResponseDto> createProduct(@RequestBody ProductCreateRequestDto request) {
        log.info("상품 생성 요청 - 상품명: {}, 브랜드명: {}", request.getName(), request.getBrandName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 관리자 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            ProductCreateResponseDto response = productCommandService.createProduct(request);

            log.info("상품 생성 성공 - 상품ID: {}, 상품명: {}", response.getId(), response.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("상품 생성 실패 - 상품명: {}, 오류: {}", request.getName(), e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_CREATION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 생성 중 예상치 못한 오류 발생 - 상품명: {}", request.getName(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_CREATION_FAILED, "상품 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 수정 API
     *
     * @param productId 상품 ID
     * @param request 상품 수정 요청 DTO
     * @return 수정된 상품 정보
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductUpdateResponseDto> updateProduct(
            @PathVariable("productId") Long productId,
            @RequestBody ProductUpdateRequestDto request) {

        log.info("상품 수정 요청 - 상품ID: {}, 상품명: {}", productId, request.getName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 관리자 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            ProductUpdateResponseDto response = productCommandService.updateProduct(productId, request);

            log.info("상품 수정 성공 - 상품ID: {}, 상품명: {}", productId, response.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("상품 수정 실패 - 상품ID: {}, 오류: {}", productId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_UPDATE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 수정 중 예상치 못한 오류 발생 - 상품ID: {}", productId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_UPDATE_FAILED, "상품 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 삭제 API
     *
     * @param productId 상품 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("productId") Long productId) {
        log.info("상품 삭제 요청 - 상품ID: {}", productId);

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 관리자 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            productCommandService.deleteProduct(productId);

            log.info("상품 삭제 성공 - 상품ID: {}", productId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("상품 삭제 실패 - 상품ID: {}, 오류: {}", productId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_DELETION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 삭제 중 예상치 못한 오류 발생 - 상품ID: {}", productId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_DELETION_FAILED, "상품 삭제 중 오류가 발생했습니다.", e);
        }
    }
}