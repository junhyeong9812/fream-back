package com.fream.back.domain.product.controller.query;

import com.fream.back.domain.product.dto.*;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.filter.FilterService;
import com.fream.back.domain.product.service.kafka.ViewEventProducer;
import com.fream.back.domain.product.service.product.ProductQueryService;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.JwtAuthenticationFilter;
import com.fream.back.global.dto.commonDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 상품 관련 조회 컨트롤러
 * 상품 검색, 상세 조회, 이미지 조회, 필터 기능 등을 제공합니다.
 */
@RestController
@RequestMapping("/products/query")
@RequiredArgsConstructor
@Slf4j
public class ProductQueryController {

    private final ProductQueryService productQueryService;
    private final UserQueryService userQueryService;
    private final ViewEventProducer viewEventProducer;
    private final FilterService filterService;

    /**
     * 상품 검색 API
     * 여러 조건으로 상품을 검색합니다.
     *
     * @param searchRequest 검색 조건 DTO
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 결과
     */
    @GetMapping
    public ResponseEntity<commonDto.PageDto<ProductSearchResponseDto>> searchProducts(
            @ModelAttribute ProductSearchDto searchRequest,
            Pageable pageable) {

        log.info("상품 검색 요청 - 키워드: {}, 카테고리: {}, 브랜드: {}, 페이지: {}, 사이즈: {}",
                searchRequest.getKeyword(),
                searchRequest.getCategoryIds(),
                searchRequest.getBrandIds(),
                pageable.getPageNumber(),
                pageable.getPageSize());

        try {
            // 유효성 검증
            searchRequest.validate();

            Page<ProductSearchResponseDto> pageResult = productQueryService.searchProducts(
                    searchRequest.getKeyword(),
                    searchRequest.getCategoryIds(),
                    searchRequest.getGenders(),
                    searchRequest.getBrandIds(),
                    searchRequest.getCollectionIds(),
                    searchRequest.getColors(),
                    searchRequest.getSizes(),
                    searchRequest.getMinPrice(),
                    searchRequest.getMaxPrice(),
                    searchRequest.getSortOption(),
                    pageable);

            commonDto.PageDto<ProductSearchResponseDto> response = toPageDto(pageResult);

            log.info("상품 검색 성공 - 총 결과 수: {}, 페이지 수: {}",
                    pageResult.getTotalElements(),
                    pageResult.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("상품 검색 실패 - 오류: {}", e.getMessage(), e);
            throw new ProductException(ProductErrorCode.FILTER_INVALID_PARAMS, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 검색 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, "상품 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Page 객체를 PageDto로 변환
     *
     * @param <T> 페이지 내용의 타입
     * @param pageResult 페이지 결과
     * @return 변환된 PageDto
     */
    private <T> commonDto.PageDto<T> toPageDto(Page<T> pageResult) {
        return new commonDto.PageDto<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),  // 현재 페이지 index(0-based)
                pageResult.getSize()
        );
    }

    /**
     * 상품 상세 조회 API
     *
     * @param productId 상품 ID
     * @param colorName 색상명
     * @return 상품 상세 정보
     */
    @GetMapping("/{productId}/detail")
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(
            @PathVariable("productId") Long productId,
            @RequestParam("colorName") String colorName) {

        log.info("상품 상세 조회 요청 - 상품ID: {}, 색상명: {}", productId, colorName);

        try {
            // 1) 상품 상세 (DB 조회)
            ProductDetailResponseDto detailDto = productQueryService.getProductDetail(productId, colorName);

            // 2) 이메일 추출 (익명 시 "anonymous")
            String email = SecurityUtils.extractEmailOrAnonymous();
            log.debug("사용자 이메일 추출 - 이메일: {}", email);

            // 3) 로그인 사용자라면 나이, 성별 조회
            Integer age = 0;
            Gender gender = Gender.OTHER;
            if (!"anonymousUser".equals(email)) {
                try {
                    // SecurityContext에 저장된 userInfo 꺼내기
                    JwtAuthenticationFilter.UserInfo userInfo = SecurityUtils.extractUserInfo();
                    age = (userInfo.getAge() == null) ? 0 : userInfo.getAge();
                    gender = (userInfo.getGender() == null) ? Gender.OTHER : userInfo.getGender();

                    log.debug("사용자 정보 추출 완료 - 나이: {}, 성별: {}", age, gender);
                } catch (IllegalArgumentException e) {
                    // 만약 이메일이 있지만 User가 없다면 anonymous로 처리
                    log.warn("사용자 정보 조회 실패 - 이메일: {}, 오류: {}", email, e.getMessage());
                    email = "anonymous";
                }
            }

            // 4) Producer에게 카프카 이벤트 발행
            viewEventProducer.sendViewEvent(detailDto.getColorId(), email, age, gender);
            log.debug("조회 이벤트 발행 완료 - 색상ID: {}, 이메일: {}", detailDto.getColorId(), email);

            log.info("상품 상세 조회 성공 - 상품ID: {}, 색상ID: {}", productId, detailDto.getColorId());
            // 5) 결과 반환
            return ResponseEntity.ok(detailDto);
        } catch (IllegalArgumentException e) {
            log.error("상품 상세 조회 실패 - 상품ID: {}, 색상명: {}, 오류: {}", productId, colorName, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 상세 조회 중 예상치 못한 오류 발생 - 상품ID: {}, 색상명: {}", productId, colorName, e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, "상품 상세 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 이미지 조회 API
     *
     * @param productId 상품 ID
     * @param imageName 이미지명
     * @return 이미지 바이트 배열
     */
    @GetMapping("/{productId}/images")
    public ResponseEntity<byte[]> getProductImage(
            @PathVariable("productId") Long productId,
            @RequestParam("imageName") String imageName
    ) {
        log.info("상품 이미지 조회 요청 - 상품ID: {}, 이미지명: {}", productId, imageName);

        try {
            // 실제 경로: /home/ubuntu/fream/product/{productId}/{imageName}
            String baseDir = "/home/ubuntu/fream";
            String directory = "product/" + productId;

            String fullPath = baseDir + File.separator + directory + File.separator + imageName;
            File imageFile = new File(fullPath);

//            String baseDir = "C:/Users/pickj/webserver/dockerVolums/fream";
//            String directory = "product/" + productId;
//            String fullPath = Paths.get(baseDir, directory, imageName).toString();
//            File imageFile = new File(fullPath);

            if (!imageFile.exists()) {
                log.error("이미지 파일이 존재하지 않음 - 경로: {}", fullPath);
                throw new IllegalArgumentException("이미지 파일이 존재하지 않습니다. fullPath=" + fullPath);
            }

            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

            String mimeType = Files.probeContentType(imageFile.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            log.info("상품 이미지 조회 성공 - 상품ID: {}, 이미지명: {}, 크기: {} bytes",
                    productId, imageName, imageBytes.length);
            return ResponseEntity.ok()
                    .header("Content-Type", mimeType)
                    .body(imageBytes);
        } catch (IllegalArgumentException e) {
            log.error("상품 이미지 조회 실패 - 상품ID: {}, 이미지명: {}, 오류: {}",
                    productId, imageName, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.IMAGE_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("상품 이미지 조회 중 예상치 못한 오류 발생 - 상품ID: {}, 이미지명: {}",
                    productId, imageName, e);
            throw new ProductException(ProductErrorCode.IMAGE_NOT_FOUND, "이미지 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 필터 데이터 조회 API
     *
     * @return 필터 데이터
     */
    @GetMapping("/filters")
    public ResponseEntity<FilterDataResponseDto> getFilterData() {
        log.info("필터 데이터 조회 요청");

        try {
            FilterDataResponseDto filterData = filterService.getAllFilterData();

            log.info("필터 데이터 조회 성공 - 카테고리 수: {}, 브랜드 수: {}, 컬렉션 수: {}",
                    filterData.getCategories().size(),
                    filterData.getBrands().size(),
                    filterData.getCollections().size());
            return ResponseEntity.ok(filterData);
        } catch (Exception e) {
            log.error("필터 데이터 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.FILTER_INVALID_PARAMS, "필터 데이터 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 필터 카운트 API
     *
     * @param searchRequest 검색 조건 DTO
     * @return 필터 조건에 맞는 상품 개수
     */
    @PostMapping("/filters/count")
    public ResponseEntity<FilterCountResponseDto> countProductsByFilter(
            @RequestBody ProductSearchDto searchRequest) {

        log.info("필터 카운트 요청 - 키워드: {}, 카테고리: {}, 브랜드: {}",
                searchRequest.getKeyword(),
                searchRequest.getCategoryIds(),
                searchRequest.getBrandIds());

        try {
            // 검증 로직
            searchRequest.validate();

            // 필터 조건에 맞는 상품 개수 조회
            long count = productQueryService.countProductsByFilter(
                    searchRequest.getKeyword(),
                    searchRequest.getCategoryIds(),
                    searchRequest.getGenders(),
                    searchRequest.getBrandIds(),
                    searchRequest.getCollectionIds(),
                    searchRequest.getColors(),
                    searchRequest.getSizes(),
                    searchRequest.getMinPrice(),
                    searchRequest.getMaxPrice());

            log.info("필터 카운트 성공 - 총 상품 수: {}", count);
            return ResponseEntity.ok(FilterCountResponseDto.builder()
                    .totalCount(count)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("필터 카운트 실패 - 오류: {}", e.getMessage(), e);
            throw new ProductException(ProductErrorCode.FILTER_INVALID_PARAMS, e.getMessage(), e);
        } catch (Exception e) {
            log.error("필터 카운트 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.FILTER_INVALID_PARAMS, "필터 카운트 중 오류가 발생했습니다.", e);
        }
    }
}