package com.fream.back.domain.product.controller.query;

import com.fream.back.domain.product.dto.*;
import com.fream.back.domain.product.entity.Product;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.service.product.ProductEntityService;
import com.fream.back.domain.product.service.product.ProductQueryService;
import com.fream.back.domain.product.service.productColor.ProductColorQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 상품 조회 API 컨트롤러
 */
@RestController
@RequestMapping("/admin/products/query")
@RequiredArgsConstructor
public class ProductQueryControllerForAdmin {

    private final ProductQueryService productQueryService;
    private final ProductEntityService productEntityService;
    private final ProductColorQueryService productColorQueryService;
    private final UserQueryService userQueryService;

    // SecurityContext에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    /**
     * 상품 상세 정보 조회 (관리자용)
     */
    @GetMapping("/{productId}/detail")
    public ResponseEntity<ProductDetailAdminResponseDto> getProductDetailForAdmin(@PathVariable("productId") Long productId) {
        // 관리자 권한 확인
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        // 상품 엔티티 조회
        Product product = productEntityService.findById(productId);

        // 응답 DTO 변환
        ProductDetailAdminResponseDto responseDto = ProductDetailAdminResponseDto.fromEntity(product);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 상품 색상 상세 정보 조회 (관리자용)
     */
    @GetMapping("/colors/{colorId}")
    public ResponseEntity<ProductColorDetailAdminResponseDto> getProductColorDetail(@PathVariable("colorId") Long colorId) {
        // 관리자 권한 확인
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        // 상품 색상 엔티티 조회
        ProductColor productColor = productColorQueryService.findById(colorId);

        // 응답 DTO 변환
        ProductColorDetailAdminResponseDto responseDto = ProductColorDetailAdminResponseDto.fromEntity(productColor);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 상품의 모든 색상 정보 조회 (관리자용)
     */
    @GetMapping("/{productId}/colors")
    public ResponseEntity<List<ProductColorDetailAdminResponseDto>> getProductColors(@PathVariable("productId") Long productId) {
        // 관리자 권한 확인
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        // 상품 엔티티 조회
        Product product = productEntityService.findById(productId);

        // 색상 정보 변환
        List<ProductColorDetailAdminResponseDto> colorDtos = product.getColors().stream()
                .map(ProductColorDetailAdminResponseDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(colorDtos);
    }
}