package com.fream.back.domain.product.service.product;

import com.fream.back.domain.order.repository.OrderBidRepository;
import com.fream.back.domain.product.dto.ProductDetailResponseDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.repository.ProductQueryDslRepository;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.style.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductQueryDslRepository productQueryDslRepository;
    private final StyleRepository styleRepository;
    private final OrderBidRepository orderBidRepository;

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
        // 2) colorId 목록 추출
        List<ProductSearchResponseDto> dtoList = pageResult.getContent();
        List<Long> colorIds = dtoList.stream()
                .map(ProductSearchResponseDto::getColorId)
                .distinct()
                .toList();

//        // 3) styleCountMap
//        Map<Long, Long> styleCountMap = productQueryDslRepository.styleCountQuery(colorIds);
//
//        // 4) tradeCountMap
//        Map<Long, Long> tradeCountMap = productQueryDslRepository.tradeCountQuery(colorIds);

        // 3) 스타일 수 조회 (styleCountByColorIds)
        Map<Long, Long> styleCountMap =
                styleRepository.styleCountByColorIds(colorIds);

        // 4) 거래 수 조회 (tradeCountByColorIds)
        Map<Long, Long> tradeCountMap =
                orderBidRepository.tradeCountByColorIds(colorIds);

        // 5) loop 돌면서 setStyleCount / setTradeCount
        dtoList.forEach(dto -> {
            Long cId = dto.getColorId();
            dto.setStyleCount(styleCountMap.getOrDefault(cId, 0L));
            dto.setTradeCount(tradeCountMap.getOrDefault(cId, 0L));
        });

        // 6) 최종 반환(Page형태)
        return new PageImpl<>(dtoList, pageResult.getPageable(), pageResult.getTotalElements());
    }

    public ProductDetailResponseDto getProductDetail(Long productId, String colorName) {
        try {
            return productQueryDslRepository.findProductDetail(productId, colorName);
        } catch (InvalidDataAccessApiUsageException e) {
            // 데이터 접근 계층의 예외를 명시적인 IllegalArgumentException으로 변환
            throw new IllegalArgumentException("해당 상품 또는 색상이 존재하지 않습니다.", e);
        }
    }
    // “ES colorId 목록”을 기반으로 DB 재조회
    public Page<ProductSearchResponseDto> searchProductsByColorIds(
            List<Long> colorIds,
            SortOption sortOption,
            Pageable pageable
    ) {
        return productQueryDslRepository.searchProductsByColorIds(
                colorIds, sortOption, pageable
        );
    }



}

