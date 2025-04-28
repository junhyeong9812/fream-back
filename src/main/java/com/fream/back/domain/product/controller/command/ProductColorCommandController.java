package com.fream.back.domain.product.controller.command;

import com.fream.back.domain.product.dto.ProductColorCreateRequestDto;
import com.fream.back.domain.product.dto.ProductColorUpdateRequestDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.productColor.ProductColorCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.utils.NginxCachePurgeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 상품 색상 관련 명령 컨트롤러
 * 상품 색상의 생성, 수정, 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/product-colors")
@RequiredArgsConstructor
@Slf4j
public class ProductColorCommandController {

    private final ProductColorCommandService productColorCommandService;
    private final UserQueryService userQueryService; // 권한 확인 서비스
    private final NginxCachePurgeUtil nginxCachePurgeUtil;

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
        throw new ProductException(ProductErrorCode.PRODUCT_COLOR_CREATION_FAILED, "인증된 사용자가 없습니다.");
    }

    /**
     * 상품 색상 생성 API
     *
     * @param productId 상품 ID
     * @param requestDto 생성 요청 DTO
     * @param thumbnailImage 썸네일 이미지
     * @param images 이미지 리스트(선택)
     * @param detailImages 상세 이미지 리스트(선택)
     * @return 성공 응답
     */
    @PostMapping("/{productId}")
    public ResponseEntity<Void> createProductColor(
            @PathVariable("productId") Long productId,
            @RequestPart("requestDto") ProductColorCreateRequestDto requestDto,
            @RequestPart("thumbnailImage") MultipartFile thumbnailImage,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {

        log.info("상품 색상 생성 요청 - 상품ID: {}, 색상명: {}", productId, requestDto.getColorName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 관리자 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            Long newColorId = productColorCommandService.createProductColor(requestDto, thumbnailImage, images, detailImages, productId);
            nginxCachePurgeUtil.purgeProductCache();
            nginxCachePurgeUtil.purgeEsCache();

            log.info("상품 색상 생성 성공 - 상품ID: {}, 색상ID: {}", productId, newColorId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("상품 색상 생성 실패 - 상품ID: {}, 오류: {}", productId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_CREATION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 색상 생성 중 예상치 못한 오류 발생 - 상품ID: {}", productId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_CREATION_FAILED, "상품 색상 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 색상 수정 API
     *
     * @param productColorId 상품 색상 ID
     * @param requestDto 수정 요청 DTO
     * @param thumbnailImage 새 썸네일 이미지(선택)
     * @param newImages 새 이미지 리스트(선택)
     * @param newDetailImages 새 상세 이미지 리스트(선택)
     * @return 성공 응답
     */
    @PutMapping("/{productColorId}")
    public ResponseEntity<Void> updateProductColor(
            @PathVariable("productColorId") Long productColorId,
            @RequestPart("requestDto") ProductColorUpdateRequestDto requestDto,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "newDetailImages", required = false) List<MultipartFile> newDetailImages) {

        log.info("상품 색상 수정 요청 - 색상ID: {}, 색상명: {}", productColorId, requestDto.getColorName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 관리자 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            productColorCommandService.updateProductColor(productColorId, requestDto, thumbnailImage, newImages, newDetailImages);
            nginxCachePurgeUtil.purgeProductCache();
            nginxCachePurgeUtil.purgeEsCache();

            log.info("상품 색상 수정 성공 - 색상ID: {}", productColorId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("상품 색상 수정 실패 - 색상ID: {}, 오류: {}", productColorId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_UPDATE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 색상 수정 중 예상치 못한 오류 발생 - 색상ID: {}", productColorId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_UPDATE_FAILED, "상품 색상 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 색상 삭제 API
     *
     * @param productColorId 상품 색상 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{productColorId}")
    public ResponseEntity<Void> deleteProductColor(@PathVariable("productColorId") Long productColorId) {
        log.info("상품 색상 삭제 요청 - 색상ID: {}", productColorId);

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 관리자 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            productColorCommandService.deleteProductColor(productColorId);
            nginxCachePurgeUtil.purgeProductCache();
            nginxCachePurgeUtil.purgeEsCache();

            log.info("상품 색상 삭제 성공 - 색상ID: {}", productColorId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("상품 색상 삭제 실패 - 색상ID: {}, 오류: {}", productColorId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_DELETION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 색상 삭제 중 예상치 못한 오류 발생 - 색상ID: {}", productColorId, e);
            throw new ProductException(ProductErrorCode.PRODUCT_COLOR_DELETION_FAILED, "상품 색상 삭제 중 오류가 발생했습니다.", e);
        }
    }
}