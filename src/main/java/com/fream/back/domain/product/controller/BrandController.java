package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.BrandRequestDto;
import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.service.brand.BrandCommandService;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandCommandService brandCommandService;
    private final BrandQueryService brandQueryService;

    private final UserQueryService userQueryService;

    // 관리자 권한 확인용 메서드
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    @PostMapping
    public ResponseEntity<BrandResponseDto> createBrand(@RequestBody BrandRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        return ResponseEntity.ok(brandCommandService.createBrand(request));
    }

    @PutMapping("/{brandId}")
    public ResponseEntity<BrandResponseDto> updateBrand(@PathVariable("brandId") Long id, @RequestBody BrandRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        return ResponseEntity.ok(brandCommandService.updateBrand(id, request));
    }

    @DeleteMapping("/{brandName}")
    public ResponseEntity<Void> deleteBrand(@PathVariable("brandName") String name) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        brandCommandService.deleteBrand(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BrandResponseDto>> getAllBrands() {
        return ResponseEntity.ok(brandQueryService.findAllBrands());
    }

    @GetMapping("/{brandName}")
    public ResponseEntity<BrandResponseDto> getBrandByName(@PathVariable("brandName") String name) {
        return ResponseEntity.ok(brandQueryService.findByName(name));
    }
}