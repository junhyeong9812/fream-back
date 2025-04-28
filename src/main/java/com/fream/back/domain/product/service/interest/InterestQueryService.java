package com.fream.back.domain.product.service.interest;

import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.InterestQueryDslRepository;
import com.fream.back.domain.product.repository.SortOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관심 상품 조회(Query) 서비스
 * 관심 상품 목록 조회 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InterestQueryService {

    private final InterestQueryDslRepository interestQueryDslRepository;

    /**
     * 사용자의 관심 상품 목록 조회
     *
     * @param userId 사용자 ID
     * @param sortOption 정렬 옵션
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 응답 DTO
     * @throws ProductException 관심 상품 조회 실패 시
     */
    public Page<ProductSearchResponseDto> findUserInterestProducts(
            Long userId,
            SortOption sortOption,
            Pageable pageable) {
        log.info("사용자 관심 상품 목록 조회 요청 - 사용자ID: {}, 페이지: {}, 사이즈: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        try {
            log.debug("정렬 옵션 - 필드: {}, 순서: {}",
                    sortOption.getField(), sortOption.getOrder());

            Page<ProductSearchResponseDto> result = interestQueryDslRepository.findUserInterestProducts(
                    userId, sortOption, pageable);

            log.info("사용자 관심 상품 목록 조회 성공 - 사용자ID: {}, 관심 상품 수: {}",
                    userId, result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("사용자 관심 상품 목록 조회 중 예상치 못한 오류 발생 - 사용자ID: {}", userId, e);
            throw new ProductException(ProductErrorCode.INTEREST_NOT_FOUND,
                    "관심 상품 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}