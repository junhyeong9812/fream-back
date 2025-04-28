package com.fream.back.domain.product.controller.query;

import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.product.service.interest.InterestQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관심 상품 관련 조회 컨트롤러
 * 사용자의 관심 상품 목록 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/interests")
@RequiredArgsConstructor
@Slf4j
public class InterestQueryController {

    private final InterestQueryService interestQueryService;

    /**
     * 사용자의 관심 상품 목록 조회 API
     *
     * @param userId 사용자 ID
     * @param field 정렬 필드(선택)
     * @param order 정렬 방향(선택)
     * @param pageable 페이징 정보
     * @return 페이징된 관심 상품 목록
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Page<ProductSearchResponseDto>> getUserInterestProducts(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "order", required = false) String order,
            Pageable pageable) {

        log.info("사용자 관심 상품 조회 요청 - 사용자ID: {}, 페이지: {}, 사이즈: {}, 정렬: {}, 방향: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), field, order);

        try {
            // SortOption 객체 생성
            SortOption sortOption = new SortOption(field, order);

            Page<ProductSearchResponseDto> response = interestQueryService.findUserInterestProducts(userId, sortOption, pageable);

            log.info("사용자 관심 상품 조회 성공 - 사용자ID: {}, 관심 상품 수: {}",
                    userId, response.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("사용자 관심 상품 조회 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.INTEREST_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("사용자 관심 상품 조회 중 예상치 못한 오류 발생 - 사용자ID: {}", userId, e);
            throw new ProductException(ProductErrorCode.INTEREST_NOT_FOUND, "관심 상품 조회 중 오류가 발생했습니다.", e);
        }
    }
}