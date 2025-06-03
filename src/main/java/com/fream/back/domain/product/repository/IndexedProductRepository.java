package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 인덱스 최적화 상품 검색 Repository
 * DB 인덱스만을 활용하여 빠른 검색을 제공합니다. (인메모리 캐시 없음)
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class IndexedProductRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 브랜드명으로 ID 조회 (idx_brand_name 인덱스 활용)
     */
    public List<Long> findBrandIdsByNames(List<String> brandNames) {
        if (brandNames == null || brandNames.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("브랜드명으로 ID 조회 - 브랜드명 수: {}", brandNames.size());

        QBrand brand = QBrand.brand;
        List<Long> brandIds = queryFactory.select(brand.id)
                .from(brand)
                .where(brand.name.in(brandNames))  // idx_brand_name 인덱스 사용
                .fetch();

        log.debug("브랜드 ID 조회 완료 - 찾은 ID 수: {}", brandIds.size());
        return brandIds;
    }

    /**
     * 카테고리명으로 ID 조회 (idx_category_name 인덱스 활용)
     */
    public List<Long> findCategoryIdsByNames(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("카테고리명으로 ID 조회 - 카테고리명 수: {}", categoryNames.size());

        QCategory category = QCategory.category;
        List<Long> categoryIds = queryFactory.select(category.id)
                .from(category)
                .where(category.name.in(categoryNames))  // idx_category_name 인덱스 사용
                .fetch();

        log.debug("카테고리 ID 조회 완료 - 찾은 ID 수: {}", categoryIds.size());
        return categoryIds;
    }

    /**
     * 컬렉션명으로 ID 조회 (idx_collection_name 인덱스 활용)
     */
    public List<Long> findCollectionIdsByNames(List<String> collectionNames) {
        if (collectionNames == null || collectionNames.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("컬렉션명으로 ID 조회 - 컬렉션명 수: {}", collectionNames.size());

        QCollection collection = QCollection.collection;
        List<Long> collectionIds = queryFactory.select(collection.id)
                .from(collection)
                .where(collection.name.in(collectionNames))  // idx_collection_name 인덱스 사용
                .fetch();

        log.debug("컬렉션 ID 조회 완료 - 찾은 ID 수: {}", collectionIds.size());
        return collectionIds;
    }

    /**
     * 인덱스 최적화된 상품 검색 (이름 기반)
     */
    public Page<ProductSearchResponseDto> searchProductsByNames(
            String keyword,
            List<String> categoryNames,
            List<GenderType> genders,
            List<String> brandNames,
            List<String> collectionNames,
            List<String> colors,
            List<String> sizes,
            Integer minPrice,
            Integer maxPrice,
            SortOption sortOption,
            Pageable pageable) {

        log.info("인덱스 최적화 상품 검색 시작 - 키워드: {}, 브랜드명: {}, 카테고리명: {}",
                keyword, brandNames, categoryNames);

        // 1단계: 이름 → ID 변환 (각각 인덱스 활용)
        List<Long> brandIds = findBrandIdsByNames(brandNames);
        List<Long> categoryIds = findCategoryIdsByNames(categoryNames);
        List<Long> collectionIds = findCollectionIdsByNames(collectionNames);

        log.debug("이름 → ID 변환 완료 - 브랜드ID: {}, 카테고리ID: {}, 컬렉션ID: {}",
                brandIds.size(), categoryIds.size(), collectionIds.size());

        // 2단계: ID 기반 상품 검색 (인덱스 최적화)
        return searchProductsByIdsOptimized(
                keyword, categoryIds, genders, brandIds, collectionIds,
                colors, sizes, minPrice, maxPrice, sortOption, pageable
        );
    }

    /**
     * ID 기반 상품 검색 (인덱스 최적화)
     */
    private Page<ProductSearchResponseDto> searchProductsByIdsOptimized(
            String keyword,
            List<Long> categoryIds,
            List<GenderType> genders,
            List<Long> brandIds,
            List<Long> collectionIds,
            List<String> colors,
            List<String> sizes,
            Integer minPrice,
            Integer maxPrice,
            SortOption sortOption,
            Pageable pageable) {

        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;
        QInterest interest = QInterest.interest;

        JPQLQuery<Tuple> query = queryFactory.select(
                        product.id,
                        product.name,
                        product.englishName,
                        product.releasePrice,
                        productColor.id,
                        productColor.colorName,
                        productColor.thumbnailImage.imageUrl,
                        productSize.purchasePrice.min(), // idx_product_size_purchase_price 인덱스 활용
                        interest.count(),
                        product.brand.name
                )
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(productColor.sizes, productSize)
                .leftJoin(productColor.interests, interest)
                .where(
                        buildKeywordPredicateOptimized(keyword, product, productColor, productSize),
                        buildCategoryIdsPredicate(categoryIds, product),     // idx_product_category 사용
                        buildGenderPredicate(genders, product),              // idx_product_gender 사용
                        buildBrandIdsPredicate(brandIds, product),           // idx_product_brand 사용
                        buildCollectionIdsPredicate(collectionIds, product), // idx_product_collection 사용
                        buildColorPredicate(colors, productColor),           // idx_product_color_name 사용
                        buildSizePredicate(sizes, productSize),
                        buildPricePredicate(minPrice, maxPrice, productSize) // idx_product_size_purchase_price 사용
                )
                .groupBy(product.id, productColor.id)
                .distinct();

        // 정렬 조건 적용 (인덱스 활용)
        applySortingOptimized(query, sortOption, product, productSize, productColor);

        // 페이징 처리
        List<Tuple> results = query.offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회 (동일한 WHERE 조건)
        long total = countProductsByConditions(
                keyword, categoryIds, genders, brandIds, collectionIds,
                colors, sizes, minPrice, maxPrice
        );

        // 결과 매핑
        List<ProductSearchResponseDto> content = results.stream()
                .map(tuple -> ProductSearchResponseDto.builder()
                        .id(tuple.get(product.id))
                        .name(tuple.get(product.name))
                        .englishName(tuple.get(product.englishName))
                        .releasePrice(tuple.get(product.releasePrice))
                        .thumbnailImageUrl(tuple.get(productColor.thumbnailImage.imageUrl))
                        .colorId(tuple.get(productColor.id))
                        .colorName(tuple.get(productColor.colorName))
                        .price(tuple.get(productSize.purchasePrice.min()))
                        .interestCount(tuple.get(interest.count()))
                        .brandName(tuple.get(product.brand.name))
                        .build())
                .toList();

        log.info("인덱스 최적화 상품 검색 완료 - 결과 수: {}, 총 개수: {}", content.size(), total);
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 키워드 검색 조건 (인덱스 최적화)
     */
    private BooleanExpression buildKeywordPredicateOptimized(String keyword, QProduct product,
                                                             QProductColor productColor, QProductSize productSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String[] keywords = keyword.trim().toLowerCase().split("\\s+|\\-|\\(|\\)");
        BooleanExpression result = null;

        for (String k : keywords) {
            if (k.trim().isEmpty()) continue;

            String likePattern = "%" + k.trim() + "%";
            // idx_product_name, idx_product_english_name, idx_product_color_name 인덱스 활용
            BooleanExpression expr = product.name.lower().like(likePattern)
                    .or(product.englishName.lower().like(likePattern))
                    .or(productColor.colorName.lower().like(likePattern))
                    .or(productSize.size.like(likePattern));

            result = (result == null) ? expr : result.and(expr);
        }
        return result;
    }

    /**
     * 브랜드 ID 조건 (idx_product_brand 인덱스 사용)
     */
    private BooleanExpression buildBrandIdsPredicate(List<Long> brandIds, QProduct product) {
        return brandIds == null || brandIds.isEmpty() ? null : product.brand.id.in(brandIds);
    }

    /**
     * 카테고리 ID 조건 (idx_product_category 인덱스 사용)
     */
    private BooleanExpression buildCategoryIdsPredicate(List<Long> categoryIds, QProduct product) {
        return categoryIds == null || categoryIds.isEmpty() ? null : product.category.id.in(categoryIds);
    }

    /**
     * 컬렉션 ID 조건 (idx_product_collection 인덱스 사용)
     */
    private BooleanExpression buildCollectionIdsPredicate(List<Long> collectionIds, QProduct product) {
        return collectionIds == null || collectionIds.isEmpty() ? null : product.collection.id.in(collectionIds);
    }

    /**
     * 성별 조건 (idx_product_gender 인덱스 사용)
     */
    private BooleanExpression buildGenderPredicate(List<GenderType> genders, QProduct product) {
        return genders == null || genders.isEmpty() ? null : product.gender.in(genders);
    }

    /**
     * 색상 조건 (idx_product_color_name 인덱스 사용)
     */
    private BooleanExpression buildColorPredicate(List<String> colors, QProductColor productColor) {
        return colors == null || colors.isEmpty() ? null : productColor.colorName.in(colors);
    }

    /**
     * 사이즈 조건
     */
    private BooleanExpression buildSizePredicate(List<String> sizes, QProductSize productSize) {
        return sizes == null || sizes.isEmpty() ? null : productSize.size.in(sizes);
    }

    /**
     * 가격 조건 (idx_product_size_purchase_price 인덱스 사용)
     */
    private BooleanExpression buildPricePredicate(Integer minPrice, Integer maxPrice, QProductSize productSize) {
        BooleanExpression predicate = null;
        if (minPrice != null) {
            predicate = productSize.purchasePrice.goe(minPrice);
        }
        if (maxPrice != null) {
            BooleanExpression maxPredicate = productSize.purchasePrice.loe(maxPrice);
            predicate = predicate == null ? maxPredicate : predicate.and(maxPredicate);
        }
        return predicate;
    }

    /**
     * 정렬 조건 적용 (인덱스 최적화)
     */
    private void applySortingOptimized(JPQLQuery<Tuple> query, SortOption sortOption,
                                       QProduct product, QProductSize productSize, QProductColor productColor) {
        if (sortOption != null) {
            switch (sortOption.getField().toLowerCase()) {
                case "price":
                    // idx_product_size_purchase_price 인덱스 활용
                    query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                            ? productSize.purchasePrice.min().asc()
                            : productSize.purchasePrice.min().desc());
                    break;
                case "releasedate":
                    // idx_product_date 인덱스 활용
                    query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                            ? product.releaseDate.asc()
                            : product.releaseDate.desc());
                    break;
                case "interestcount":
                    query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                            ? productColor.interests.size().sum().asc()
                            : productColor.interests.size().sum().desc());
                    break;
                default:
                    query.orderBy(product.id.asc()); // 기본 정렬
            }
        } else {
            query.orderBy(product.id.asc());
        }
    }

    /**
     * 조건에 맞는 상품 개수 조회 (인덱스 최적화)
     */
    private long countProductsByConditions(String keyword, List<Long> categoryIds, List<GenderType> genders,
                                           List<Long> brandIds, List<Long> collectionIds, List<String> colors,
                                           List<String> sizes, Integer minPrice, Integer maxPrice) {

        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;

        Long count = queryFactory.select(product.id.countDistinct())
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(productColor.sizes, productSize)
                .where(
                        buildKeywordPredicateOptimized(keyword, product, productColor, productSize),
                        buildCategoryIdsPredicate(categoryIds, product),
                        buildGenderPredicate(genders, product),
                        buildBrandIdsPredicate(brandIds, product),
                        buildCollectionIdsPredicate(collectionIds, product),
                        buildColorPredicate(colors, productColor),
                        buildSizePredicate(sizes, productSize),
                        buildPricePredicate(minPrice, maxPrice, productSize)
                )
                .fetchOne();

        return count != null ? count : 0L;
    }
}