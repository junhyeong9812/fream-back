package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.BrandRequestDto;
import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.service.brand.BrandCommandService;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ResponseDto<BrandResponseDto>> createBrand(@RequestBody BrandRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        BrandResponseDto response = brandCommandService.createBrand(request);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @PutMapping("/{brandId}")
    public ResponseEntity<ResponseDto<BrandResponseDto>> updateBrand(
            @PathVariable("brandId") Long id,
            @RequestBody BrandRequestDto request
    ) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        BrandResponseDto response = brandCommandService.updateBrand(id, request);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @DeleteMapping("/{brandName}")
    public ResponseEntity<ResponseDto<Void>> deleteBrand(@PathVariable("brandName") String name) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        brandCommandService.deleteBrand(name);
        return ResponseEntity.ok(ResponseDto.success(null));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<BrandResponseDto>>> getAllBrands() {
        List<BrandResponseDto> response = brandQueryService.findAllBrands();
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    // 페이징된 브랜드 조회
    @GetMapping("/page")
    public ResponseEntity<ResponseDto<Page<BrandResponseDto>>> getBrandsPaging(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<BrandResponseDto> response = brandQueryService.findBrandsPaging(pageable);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    // 브랜드 검색 (페이징)
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<BrandResponseDto>>> searchBrands(
            @RequestParam String keyword,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<BrandResponseDto> response = brandQueryService.searchBrandsByName(keyword, pageable);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @GetMapping("/{brandId:[0-9]+}")
    public ResponseEntity<ResponseDto<BrandResponseDto>> getBrandById(@PathVariable("brandId") Long id) {
        BrandResponseDto response = brandQueryService.findBrandById(id);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @GetMapping("/name/{brandName}")
    public ResponseEntity<ResponseDto<BrandResponseDto>> getBrandByName(@PathVariable("brandName") String name) {
        BrandResponseDto response = brandQueryService.findByName(name);
        return ResponseEntity.ok(ResponseDto.success(response));
    }
}