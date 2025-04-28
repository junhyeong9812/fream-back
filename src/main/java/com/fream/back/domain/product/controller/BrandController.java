package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.BrandRequestDto;
import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.brand.BrandCommandService;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 브랜드 관련 컨트롤러
 * 브랜드의 생성, 수정, 삭제, 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Slf4j
public class BrandController {

    private final BrandCommandService brandCommandService;
    private final BrandQueryService brandQueryService;
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
        throw new ProductException(ProductErrorCode.BRAND_CREATION_FAILED, "인증된 사용자가 없습니다.");
    }

    /**
     * 브랜드 생성 API
     *
     * @param request 브랜드 생성 요청 DTO
     * @return 생성된 브랜드 정보
     */
    @PostMapping
    public ResponseEntity<ResponseDto<BrandResponseDto>> createBrand(@RequestBody BrandRequestDto request) {
        log.info("브랜드 생성 요청 - 브랜드명: {}", request.getName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            BrandResponseDto response = brandCommandService.createBrand(request);

            log.info("브랜드 생성 성공 - 브랜드ID: {}, 브랜드명: {}", response.getId(), response.getName());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("브랜드 생성 실패 - 브랜드명: {}, 오류: {}", request.getName(), e.getMessage(), e);
            throw new ProductException(ProductErrorCode.BRAND_CREATION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("브랜드 생성 중 예상치 못한 오류 발생 - 브랜드명: {}", request.getName(), e);
            throw new ProductException(ProductErrorCode.BRAND_CREATION_FAILED, "브랜드 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드 수정 API
     *
     * @param id      브랜드 ID
     * @param request 브랜드 수정 요청 DTO
     * @return 수정된 브랜드 정보
     */
    @PutMapping("/{brandId}")
    public ResponseEntity<ResponseDto<BrandResponseDto>> updateBrand(
            @PathVariable("brandId") Long id,
            @RequestBody BrandRequestDto request
    ) {
        log.info("브랜드 수정 요청 - 브랜드ID: {}, 브랜드명: {}", id, request.getName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            BrandResponseDto response = brandCommandService.updateBrand(id, request);

            log.info("브랜드 수정 성공 - 브랜드ID: {}, 브랜드명: {}", id, response.getName());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("브랜드 수정 실패 - 브랜드ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.BRAND_UPDATE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("브랜드 수정 중 예상치 못한 오류 발생 - 브랜드ID: {}", id, e);
            throw new ProductException(ProductErrorCode.BRAND_UPDATE_FAILED, "브랜드 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드 삭제 API
     *
     * @param name 브랜드명
     * @return 성공 응답
     */
    @DeleteMapping("/{brandName}")
    public ResponseEntity<ResponseDto<Void>> deleteBrand(@PathVariable("brandName") String name) {
        log.info("브랜드 삭제 요청 - 브랜드명: {}", name);

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            brandCommandService.deleteBrand(name);

            log.info("브랜드 삭제 성공 - 브랜드명: {}", name);
            return ResponseEntity.ok(ResponseDto.success(null));
        } catch (IllegalArgumentException e) {
            log.error("브랜드 삭제 실패 - 브랜드명: {}, 오류: {}", name, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.BRAND_DELETION_FAILED, e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("브랜드 삭제 실패 (사용 중) - 브랜드명: {}, 오류: {}", name, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.BRAND_IN_USE, e.getMessage(), e);
        } catch (Exception e) {
            log.error("브랜드 삭제 중 예상치 못한 오류 발생 - 브랜드명: {}", name, e);
            throw new ProductException(ProductErrorCode.BRAND_DELETION_FAILED, "브랜드 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 브랜드 조회 API
     *
     * @return 브랜드 목록
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<BrandResponseDto>>> getAllBrands() {
        log.info("모든 브랜드 조회 요청");

        try {
            List<BrandResponseDto> response = brandQueryService.findAllBrands();

            log.info("브랜드 조회 성공 - 브랜드 수: {}", response.size());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            log.error("브랜드 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 페이징된 브랜드 조회 API
     *
     * @param pageable 페이징 정보
     * @return 페이징된 브랜드 목록
     */
    @GetMapping("/page")
    public ResponseEntity<ResponseDto<Page<BrandResponseDto>>> getBrandsPaging(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("페이징된 브랜드 조회 요청 - 페이지: {}, 사이즈: {}", pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<BrandResponseDto> response = brandQueryService.findBrandsPaging(pageable);

            log.info("페이징된 브랜드 조회 성공 - 총 브랜드 수: {}, 현재 페이지: {}", response.getTotalElements(), response.getNumber());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            log.error("페이징된 브랜드 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드 검색 API (페이징)
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 브랜드 목록
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<BrandResponseDto>>> searchBrands(
            @RequestParam String keyword,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("브랜드 검색 요청 - 키워드: {}, 페이지: {}, 사이즈: {}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<BrandResponseDto> response = brandQueryService.searchBrandsByName(keyword, pageable);

            log.info("브랜드 검색 성공 - 키워드: {}, 검색 결과 수: {}", keyword, response.getTotalElements());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            log.error("브랜드 검색 중 예상치 못한 오류 발생 - 키워드: {}", keyword, e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드 ID로 조회 API
     *
     * @param id 브랜드 ID
     * @return 브랜드 정보
     */
    @GetMapping("/{brandId:[0-9]+}")
    public ResponseEntity<ResponseDto<BrandResponseDto>> getBrandById(@PathVariable("brandId") Long id) {
        log.info("브랜드 ID로 조회 요청 - 브랜드ID: {}", id);

        try {
            BrandResponseDto response = brandQueryService.findBrandById(id);

            log.info("브랜드 ID로 조회 성공 - 브랜드ID: {}, 브랜드명: {}", id, response.getName());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("브랜드 ID로 조회 실패 - 브랜드ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("브랜드 ID로 조회 중 예상치 못한 오류 발생 - 브랜드ID: {}", id, e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드명으로 조회 API
     *
     * @param name 브랜드명
     * @return 브랜드 정보
     */
    @GetMapping("/name/{brandName}")
    public ResponseEntity<ResponseDto<BrandResponseDto>> getBrandByName(@PathVariable("brandName") String name) {
        log.info("브랜드명으로 조회 요청 - 브랜드명: {}", name);

        try {
            BrandResponseDto response = brandQueryService.findByName(name);

            log.info("브랜드명으로 조회 성공 - 브랜드명: {}, 브랜드ID: {}", name, response.getId());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("브랜드명으로 조회 실패 - 브랜드명: {}, 오류: {}", name, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("브랜드명으로 조회 중 예상치 못한 오류 발생 - 브랜드명: {}", name, e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 조회 중 오류가 발생했습니다.", e);
        }
    }
}