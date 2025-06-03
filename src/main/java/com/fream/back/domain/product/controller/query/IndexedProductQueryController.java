package com.fream.back.domain.product.controller.query;

import com.fream.back.domain.product.dto.ProductSearchByNameDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.product.IndexedProductQueryService;
import com.fream.back.global.dto.commonDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인덱스 최적화 상품 검색 컨트롤러
 * 이름 기반 검색으로 인덱스를 최대한 활용합니다.
 */
@RestController
@RequestMapping("/products/indexed")
@RequiredArgsConstructor
@Slf4j
public class IndexedProductQueryController {

    private final IndexedProductQueryService indexedProductQueryService;

    /**
     * 인덱스 최적화 상품 검색 API (이름 기반)
     * 브랜드명, 카테고리명, 컬렉션명으로 검색하여 인덱스를 최대한 활용합니다.
     *
     * @param searchRequest 이름 기반 검색 조건 DTO
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 결과
     */
    @GetMapping("/search")
    public ResponseEntity<commonDto.PageDto<ProductSearchResponseDto>> searchProductsByNames(
            @ModelAttribute ProductSearchByNameDto searchRequest,
            Pageable pageable) {

        log.info("인덱스 최적화 상품 검색 요청 - 키워드: {}, 브랜드명: {}, 카테고리명: {}, 페이지: {}, 사이즈: {}",
                searchRequest.getKeyword(),
                searchRequest.getBrandNames(),
                searchRequest.getCategoryNames(),
                pageable.getPageNumber(),
                pageable.getPageSize());

        try {
            // 유효성 검증
            searchRequest.validate();

            Page<ProductSearchResponseDto> pageResult = indexedProductQueryService.searchProductsByNames(
                    searchRequest, pageable);

            commonDto.PageDto<ProductSearchResponseDto> response = toPageDto(pageResult);

            log.info("인덱스 최적화 상품 검색 성공 - 총 결과 수: {}, 페이지 수: {}",
                    pageResult.getTotalElements(),
                    pageResult.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("인덱스 최적화 상품 검색 실패 - 오류: {}", e.getMessage(), e);
            throw new ProductException(ProductErrorCode.FILTER_INVALID_PARAMS, e.getMessage(), e);
        } catch (Exception e) {
            log.error("인덱스 최적화 상품 검색 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, "상품 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Page 객체를 PageDto로 변환
     */
    private <T> commonDto.PageDto<T> toPageDto(Page<T> pageResult) {
        return new commonDto.PageDto<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),
                pageResult.getSize()
        );
    }
}