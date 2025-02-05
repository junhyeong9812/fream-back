package com.fream.back.domain.product.controller.command;

import com.fream.back.domain.product.dto.ProductColorCreateRequestDto;
import com.fream.back.domain.product.dto.ProductColorUpdateRequestDto;
import com.fream.back.domain.product.elasticsearch.service.ProductColorIndexingService;
import com.fream.back.domain.product.service.productColor.ProductColorCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.utils.NginxCachePurgeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/product-colors")
@RequiredArgsConstructor
public class ProductColorCommandController {

    private final ProductColorCommandService productColorCommandService;
    private final UserQueryService userQueryService; // 권한 확인 서비스
    private final NginxCachePurgeUtil nginxCachePurgeUtil;
    private final ProductColorIndexingService productColorIndexingService;

    // SecurityContext에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> createProductColor(
            @PathVariable("productId") Long productId,
            @RequestPart("requestDto") ProductColorCreateRequestDto requestDto,
            @RequestPart("thumbnailImage") MultipartFile thumbnailImage,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {

        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        Long newColorId = productColorCommandService.createProductColor(requestDto, thumbnailImage, images, detailImages, productId);
        productColorIndexingService.indexColorById(newColorId);
        nginxCachePurgeUtil.purgeProductCache();
        nginxCachePurgeUtil.purgeEsCache();
        return ResponseEntity.ok().build();
    }




    @PutMapping("/{productColorId}")
    public ResponseEntity<Void> updateProductColor(
            @PathVariable("productColorId") Long productColorId,
            @RequestPart("requestDto") ProductColorUpdateRequestDto requestDto,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "newDetailImages", required = false) List<MultipartFile> newDetailImages) {

        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        productColorCommandService.updateProductColor(productColorId, requestDto, thumbnailImage, newImages, newDetailImages);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productColorId}")
    public ResponseEntity<Void> deleteProductColor(@PathVariable("productColorId") Long productColorId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        productColorCommandService.deleteProductColor(productColorId);
        // 2) ES 인덱스에서도 삭제
        productColorIndexingService.deleteColorFromIndex(productColorId);
        // 3) Nginx 캐시 Purge
        nginxCachePurgeUtil.purgeProductCache();
        nginxCachePurgeUtil.purgeEsCache();
        return ResponseEntity.noContent().build();
    }
}
