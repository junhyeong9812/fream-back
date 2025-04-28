package com.fream.back.domain.product.service.product;

import com.fream.back.domain.order.repository.OrderBidRepository;
import com.fream.back.domain.product.dto.ProductDetailResponseDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.ProductQueryDslRepository;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.style.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 상품 조회(Query) 서비스
 * 상품 조회 관련 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductQueryService {

    private final ProductQueryDslRepository productQueryDslRepository;
    private final StyleRepository styleRepository;
    private final OrderBidRepository orderBidRepository;

    /**
     * 상품 검색
     *
     * @param keyword 검색 키워드
     * @param categoryIds 카테고리 ID 목록
     * @param genders 성별 목록
     * @param brandIds 브랜드 ID 목록
     * @param collectionIds 컬렉션 ID 목록
     * @param colors 색상 목록
     * @param sizes 사이즈 목록
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param sortOptions 정렬 옵션
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 응답 DTO
     * @throws ProductException 상품 검색 실패 시
     */
    public Page<ProductSearchResponseDto> searchProducts(
            String keyword,
            List<Long> categoryIds,
            List<GenderType> genders,
            List<Long> brandIds,
            List<Long> collectionIds,
            List<String> colors,
            List<String> sizes,
            Integer minPrice,
            Integer maxPrice,
            SortOption sortOptions,
            Pageable pageable) {

        log.info("상품 검색 요청 - 키워드: {}, 카테고리 수: {}, 브랜드 수: {}",
                keyword,
                categoryIds != null ? categoryIds.size() : 0,
                brandIds != null ? brandIds.size() : 0);

        try {
            // 1) 기본 검색 쿼리 실행
            log.debug("상품 검색 쿼리 실행 시작");
            Page<ProductSearchResponseDto> pageResult = productQueryDslRepository.searchProducts(
                    keyword,
                    categoryIds,
                    genders,
                    brandIds,
                    collectionIds,
                    colors,
                    sizes,
                    minPrice,
                    maxPrice,
                    sortOptions,
                    pageable
            );
            log.debug("상품 검색 쿼리 실행 완료 - 결과 수: {}", pageResult.getTotalElements());

            // 2) colorId 목록 추출
            List<ProductSearchResponseDto> dtoList = pageResult.getContent();
            List<Long> colorIds = dtoList.stream()
                    .map(ProductSearchResponseDto::getColorId)
                    .distinct()
                    .toList();
            log.debug("색상 ID 목록 추출 완료 - 색상 수: {}", colorIds.size());

            // 3) 스타일 수 조회
            log.debug("스타일 수 조회 시작");
            Map<Long, Long> styleCountMap = styleRepository.styleCountByColorIds(colorIds);
            log.debug("스타일 수 조회 완료 - 맵 크기: {}", styleCountMap.size());

            // 4) 거래 수 조회
            log.debug("거래 수 조회 시작");
            Map<Long, Long> tradeCountMap = orderBidRepository.tradeCountByColorIds(colorIds);
            log.debug("거래 수 조회 완료 - 맵 크기: {}", tradeCountMap.size());

            // 5) loop 돌면서 setStyleCount / setTradeCount
            log.debug("스타일 수와 거래 수 설정 시작");
            dtoList.forEach(dto -> {
                Long cId = dto.getColorId();
                dto.setStyleCount(styleCountMap.getOrDefault(cId, 0L));
                dto.setTradeCount(tradeCountMap.getOrDefault(cId, 0L));
            });
            log.debug("스타일 수와 거래 수 설정 완료");

            // 6) 최종 반환(Page형태)
            Page<ProductSearchResponseDto> result = new PageImpl<>(dtoList, pageResult.getPageable(), pageResult.getTotalElements());
            log.info("상품 검색 성공 - 총 결과 수: {}, 페이지 수: {}",
                    result.getTotalElements(), result.getTotalPages());
            return result;
        } catch (Exception e) {
            log.error("상품 검색 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND, "상품 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 상세 조회
     *
     * @param productId 상품 ID
     * @param colorName 색상명
     * @return 상품 상세 응답 DTO
     * @throws ProductException 상품 상세 조회 실패 시
     */
    public ProductDetailResponseDto getProductDetail(Long productId, String colorName) {
        log.info("상품 상세 조회 요청 - 상품ID: {}, 색상명: {}", productId, colorName);

        try {
            ProductDetailResponseDto detailDto = productQueryDslRepository.findProductDetail(productId, colorName);
            log.info("상품 상세 조회 성공 - 상품ID: {}, 색상ID: {}", productId, detailDto.getColorId());
            return detailDto;
        } catch (InvalidDataAccessApiUsageException e) {
            // 데이터 접근 계층의 예외를 명시적인 예외로 변환
            log.error("상품 상세 조회 실패 - 상품ID: {}, 색상명: {}, 오류: {}",
                    productId, colorName, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                    "해당 상품 또는 색상이 존재하지 않습니다.", e);
        } catch (Exception e) {
            log.error("상품 상세 조회 중 예상치 못한 오류 발생 - 상품ID: {}, 색상명: {}",
                    productId, colorName, e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                    "상품 상세 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 색상 ID 목록으로 상품 검색
     * ES colorId 목록을 기반으로 DB 재조회
     *
     * @param colorIds 색상 ID 목록
     * @param sortOption 정렬 옵션
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 응답 DTO
     * @throws ProductException 상품 검색 실패 시
     */
    public Page<ProductSearchResponseDto> searchProductsByColorIds(
            List<Long> colorIds,
            SortOption sortOption,
            Pageable pageable
    ) {
        log.info("색상 ID 목록으로 상품 검색 요청 - 색상 수: {}",
                colorIds != null ? colorIds.size() : 0);

        try {
            if (colorIds == null || colorIds.isEmpty()) {
                log.warn("색상 ID 목록이 비어 있음 - 빈 결과 반환");
                return Page.empty(pageable);
            }

            log.debug("색상 ID 목록으로 상품 검색 쿼리 실행 시작");
            Page<ProductSearchResponseDto> result = productQueryDslRepository.searchProductsByColorIds(
                    colorIds, sortOption, pageable
            );
            log.info("색상 ID 목록으로 상품 검색 성공 - 총 결과 수: {}", result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("색상 ID 목록으로 상품 검색 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND,
                    "상품 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 필터 조건으로 상품 개수 조회
     *
     * @param keyword 검색 키워드
     * @param categoryIds 카테고리 ID 목록
     * @param genders 성별 목록
     * @param brandIds 브랜드 ID 목록
     * @param collectionIds 컬렉션 ID 목록
     * @param colors 색상 목록
     * @param sizes 사이즈 목록
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @return 조건에 맞는 상품 개수
     * @throws ProductException 상품 개수 조회 실패 시
     */
    public long countProductsByFilter(
            String keyword,
            List<Long> categoryIds,
            List<GenderType> genders,
            List<Long> brandIds,
            List<Long> collectionIds,
            List<String> colors,
            List<String> sizes,
            Integer minPrice,
            Integer maxPrice) {

        log.info("필터 조건으로 상품 개수 조회 요청 - 키워드: {}, 카테고리 수: {}, 브랜드 수: {}",
                keyword,
                categoryIds != null ? categoryIds.size() : 0,
                brandIds != null ? brandIds.size() : 0);

        try {
            long count = productQueryDslRepository.countProductsByFilter(
                    keyword,
                    categoryIds,
                    genders,
                    brandIds,
                    collectionIds,
                    colors,
                    sizes,
                    minPrice,
                    maxPrice
            );
            log.info("필터 조건으로 상품 개수 조회 성공 - 개수: {}", count);
            return count;
        } catch (Exception e) {
            log.error("필터 조건으로 상품 개수 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.FILTER_INVALID_PARAMS,
                    "상품 개수 조회 중 오류가 발생했습니다.", e);
        }
    }
}