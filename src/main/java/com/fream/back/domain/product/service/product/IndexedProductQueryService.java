package com.fream.back.domain.product.service.product;

import com.fream.back.domain.product.dto.ProductSearchByNameDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.IndexedProductRepository;
import com.fream.back.domain.product.repository.ProductQueryDslRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 인덱스 최적화 상품 검색 서비스
 * 이름 기반 검색과 DB 인덱스를 최대한 활용합니다.
 *
 * 기존 ProductQueryService와 달리 인메모리 캐시 없이
 * 순수하게 DB 인덱스만을 활용한 최적화를 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class IndexedProductQueryService {

    private final IndexedProductRepository indexedProductRepository;
    private final ProductQueryDslRepository productQueryDslRepository; // 스타일/거래 수 조회용

    /**
     * 인덱스 최적화된 상품 검색 (이름 기반)
     *
     * @param searchRequest 이름 기반 검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 결과
     */
    public Page<ProductSearchResponseDto> searchProductsByNames(
            ProductSearchByNameDto searchRequest,
            Pageable pageable) {

        log.info("인덱스 최적화 상품 검색 요청 - 키워드: {}, 브랜드명: {}, 카테고리명: {}",
                searchRequest.getKeyword(),
                searchRequest.getBrandNames(),
                searchRequest.getCategoryNames());

        try {
            // 1) 기본 검색 쿼리 실행 (인덱스 최적화)
            log.debug("인덱스 최적화 상품 검색 쿼리 실행 시작");
            Page<ProductSearchResponseDto> pageResult = indexedProductRepository.searchProductsByNames(
                    searchRequest.getKeyword(),
                    searchRequest.getCategoryNames(),
                    searchRequest.getGenders(),
                    searchRequest.getBrandNames(),
                    searchRequest.getCollectionNames(),
                    searchRequest.getColors(),
                    searchRequest.getSizes(),
                    searchRequest.getMinPrice(),
                    searchRequest.getMaxPrice(),
                    searchRequest.getSortOption(),
                    pageable
            );
            log.debug("인덱스 최적화 상품 검색 쿼리 실행 완료 - 결과 수: {}", pageResult.getTotalElements());

            // 2) colorId 목록 추출
            List<ProductSearchResponseDto> dtoList = pageResult.getContent();
            List<Long> colorIds = dtoList.stream()
                    .map(ProductSearchResponseDto::getColorId)
                    .distinct()
                    .toList();
            log.debug("색상 ID 목록 추출 완료 - 색상 수: {}", colorIds.size());

            // 3) 스타일 수 조회 (기존 Repository 활용)
            log.debug("스타일 수 조회 시작");
            Map<Long, Long> styleCountMap = productQueryDslRepository.styleCountQuery(colorIds);
            log.debug("스타일 수 조회 완료 - 맵 크기: {}", styleCountMap.size());

            // 4) 거래 수 조회 (기존 Repository 활용)
            log.debug("거래 수 조회 시작");
            Map<Long, Long> tradeCountMap = productQueryDslRepository.tradeCountQuery(colorIds);
            log.debug("거래 수 조회 완료 - 맵 크기: {}", tradeCountMap.size());

            // 5) DTO에 추가 정보 설정
            log.debug("스타일 수와 거래 수 설정 시작");
            dtoList.forEach(dto -> {
                Long cId = dto.getColorId();
                dto.setStyleCount(styleCountMap.getOrDefault(cId, 0L));
                dto.setTradeCount(tradeCountMap.getOrDefault(cId, 0L));
            });
            log.debug("스타일 수와 거래 수 설정 완료");

            // 6) 최종 반환 (Page 형태 유지)
            Page<ProductSearchResponseDto> result = new PageImpl<>(dtoList, pageResult.getPageable(), pageResult.getTotalElements());
            log.info("인덱스 최적화 상품 검색 성공 - 총 결과 수: {}, 페이지 수: {}",
                    result.getTotalElements(), result.getTotalPages());
            return result;
        } catch (Exception e) {
            log.error("인덱스 최적화 상품 검색 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, "상품 검색 중 오류가 발생했습니다.", e);
        }
    }
}