package com.fream.back.domain.product.repository;

import com.fream.back.domain.order.entity.BidStatus;
import com.fream.back.domain.order.entity.QOrderBid;
import com.fream.back.domain.order.entity.QOrderItem;
import com.fream.back.domain.product.dto.ProductDetailResponseDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.style.entity.QStyle;
import com.fream.back.domain.style.entity.QStyleOrderItem;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ProductQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    // JPAQueryFactory를 생성자로 주입받아 초기화
    public ProductQueryDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * 상품 검색 및 필터링 메서드
     *
     * @param keyword        키워드 검색 조건
     * @param categoryIds    카테고리 ID 목록
     * @param genders        성별 조건
     * @param brandIds       브랜드 ID 목록
     * @param collectionIds  컬렉션 ID 목록
     * @param colors         색상 조건
     * @param sizes          사이즈 조건
     * @param minPrice       최소 가격 조건
     * @param maxPrice       최대 가격 조건
     * @param pageable       페이징 정보
     * @return 페이징된 상품 목록
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
            SortOption sortOption,
            Pageable pageable) {

        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;
        QInterest interest = QInterest.interest;
        QBrand brand = QBrand.brand;

        System.out.println("Keyword: " + keyword);
        System.out.println("Category IDs: " + categoryIds);
        System.out.println("Genders: " + genders);
        System.out.println("Brand IDs: " + brandIds);
        System.out.println("Collection IDs: " + collectionIds);
        System.out.println("Colors: " + colors);
        System.out.println("Sizes: " + sizes);
        System.out.println("Min Price: " + minPrice);
        System.out.println("Max Price: " + maxPrice);


        JPQLQuery<Tuple> query = queryFactory.select(
                        product.id,
                        product.name,
                        product.englishName,
                        product.releasePrice,
                        productColor.id,
                        productColor.colorName,
                        productColor.thumbnailImage.imageUrl,
                        productSize.purchasePrice.min(), // 최소 구매가
                        interest.count(), // 관심 수
                        product.brand.name
                )
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(product.brand, brand)  // 브랜드 조인
                .leftJoin(productColor.thumbnailImage, QProductImage.productImage) // Thumbnail 이미지 조인 추가
                .leftJoin(productColor.sizes, productSize)
                .leftJoin(productColor.interests, interest)
                .where(
                        logPredicate("Keyword Predicate", buildKeywordPredicate(keyword, product, productColor, productSize)),
                        logPredicate("Category Predicate", buildCategoryPredicate(categoryIds, product)),
                        logPredicate("Gender Predicate", buildGenderPredicate(genders, product)),
                        logPredicate("Brand Predicate", buildBrandPredicate(brandIds, product)),
                        logPredicate("Collection Predicate", buildCollectionPredicate(collectionIds, product)),
                        logPredicate("Color Predicate", buildColorPredicate(colors, productColor)),
                        logPredicate("Size Predicate", buildSizePredicate(sizes, productSize)),
                        logPredicate("Price Predicate", buildPricePredicate(minPrice, maxPrice, productSize))
                )
                .groupBy(product.id, productColor.id, QProductImage.productImage.imageUrl)
                .distinct();

        // 정렬 조건 적용
        if (sortOption != null) {
            if ("price".equalsIgnoreCase(sortOption.getField())) {
                query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                        ? productSize.purchasePrice.min().asc()
                        : productSize.purchasePrice.min().desc());
            } else if ("releaseDate".equalsIgnoreCase(sortOption.getField())) {
                query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                        ? product.releaseDate.asc()
                        : product.releaseDate.desc());
            } else if ("interestCount".equalsIgnoreCase(sortOption.getField())) {
                query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                        ? productColor.interests.size().sum().asc()
                        : productColor.interests.size().sum().desc());
            }
        } else {
            query.orderBy(product.id.asc()); // 기본 정렬
        }
        System.out.println("Offset: " + pageable.getOffset());
        System.out.println("Page Size: " + pageable.getPageSize());
        // 2. 페이징 처리
        List<Tuple> results = query.offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        results.forEach(tuple -> {
            System.out.println("Product ID: " + tuple.get(product.id));
            System.out.println("Color ID: " + tuple.get(productColor.id));
            System.out.println("Thumbnail URL: " + tuple.get(QProductImage.productImage.imageUrl));
            System.out.println("Color Name: " + tuple.get(productColor.colorName));
            System.out.println("Lowest Price: " + tuple.get(productSize.purchasePrice.min()));
            System.out.println("Interest Count: " + tuple.get(interest.count()));
        });


        System.out.println("results = " + results);

// 추가 조회로 이미지 및 컬러 정보 조회
        List<Long> productIds = results.stream()
                .map(tuple -> tuple.get(product.id))
                .distinct()
                .toList();
//
//        List<ProductColor> productColors = queryFactory.selectFrom(productColor)
//                .join(productColor.thumbnailImage, QProductImage.productImage)
//                .where(productColor.product.id.in(productIds))
//                .fetch();

        // 5. 결과 매핑
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
        System.out.println("content = " + content);

        // 6. 전체 데이터 수 조회
        long total = queryFactory.select(product.id.countDistinct())
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(productColor.sizes, productSize)
                .where(
                        buildKeywordPredicate(keyword, product, productColor, productSize),
                        buildCategoryPredicate(categoryIds, product),
                        buildGenderPredicate(genders, product),
                        buildBrandPredicate(brandIds, product),
                        buildCollectionPredicate(collectionIds, product),
                        buildColorPredicate(colors, productColor),
                        buildSizePredicate(sizes, productSize),
                        buildPricePredicate(minPrice, maxPrice, productSize)
                )
                .fetchOne();
        System.out.println("total = " + total);


        // 7. 결과 반환
        return new PageImpl<>(content, pageable, total);
    }
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

        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;

        // 전체 데이터 수 조회 쿼리
        return queryFactory.select(product.id.countDistinct())
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(productColor.sizes, productSize)
                .where(
                        buildKeywordPredicate(keyword, product, productColor, productSize),
                        buildCategoryPredicate(categoryIds, product),
                        buildGenderPredicate(genders, product),
                        buildBrandPredicate(brandIds, product),
                        buildCollectionPredicate(collectionIds, product),
                        buildColorPredicate(colors, productColor),
                        buildSizePredicate(sizes, productSize),
                        buildPricePredicate(minPrice, maxPrice, productSize)
                )
                .fetchOne() != null ? queryFactory.select(product.id.countDistinct())
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(productColor.sizes, productSize)
                .where(
                        buildKeywordPredicate(keyword, product, productColor, productSize),
                        buildCategoryPredicate(categoryIds, product),
                        buildGenderPredicate(genders, product),
                        buildBrandPredicate(brandIds, product),
                        buildColorPredicate(colors, productColor),
                        buildSizePredicate(sizes, productSize),
                        buildPricePredicate(minPrice, maxPrice, productSize)
                )
                .fetchOne() : 0L;
    }

    // Predicate 출력 로깅 메서드
    private BooleanExpression logPredicate(String predicateName, BooleanExpression predicate) {
        System.out.println(predicateName + ": " + (predicate != null ? predicate.toString() : "null"));
        return predicate;
    }


    private BooleanExpression buildKeywordPredicate(String keyword, QProduct product, QProductColor productColor, QProductSize productSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        String trimmedKeyword = "%" + keyword.trim().toLowerCase() + "%";
        return product.name.lower().like(trimmedKeyword)
                .or(product.englishName.lower().like(trimmedKeyword))
                .or(productColor.colorName.lower().like(trimmedKeyword))
                .or(productSize.size.like(trimmedKeyword));
    }

    // 카테고리 조건 빌드
    private BooleanExpression buildCategoryPredicate(List<Long> categoryIds, QProduct product) {
        return categoryIds == null || categoryIds.isEmpty() ? null : product.category.id.in(categoryIds);
    }

    // 성별 조건 빌드
    private BooleanExpression buildGenderPredicate(List<GenderType> genders, QProduct product) {
        return genders == null || genders.isEmpty() ? null : product.gender.in(genders);
    }

    // 브랜드 조건 빌드
    private BooleanExpression buildBrandPredicate(List<Long> brandIds, QProduct product) {
        return brandIds == null || brandIds.isEmpty() ? null : product.brand.id.in(brandIds);
    }

    // 컬렉션 조건 빌드
    private BooleanExpression buildCollectionPredicate(List<Long> collectionIds, QProduct product) {
        return collectionIds == null || collectionIds.isEmpty() ? null : product.collection.id.in(collectionIds);
    }

    // 색상 조건 빌드
    private BooleanExpression buildColorPredicate(List<String> colors, QProductColor productColor) {
        return colors == null || colors.isEmpty() ? null : productColor.colorName.in(colors);
    }

    // 사이즈 조건 빌드
    private BooleanExpression buildSizePredicate(List<String> sizes, QProductSize productSize) {
        return sizes == null || sizes.isEmpty() ? null : productSize.size.in(sizes);
    }

    // 가격 조건 빌드
    private BooleanExpression buildPricePredicate(Integer minPrice, Integer maxPrice, QProductSize productSize) {
        BooleanExpression predicate = null;
        if (minPrice != null) {
            predicate = productSize.purchasePrice.goe(minPrice); // 최소 가격 조건
        }
        if (maxPrice != null) {
            predicate = predicate == null ? productSize.purchasePrice.loe(maxPrice) : predicate.and(productSize.purchasePrice.loe(maxPrice));
        }
        return predicate;
    }

    public ProductDetailResponseDto findProductDetail(Long productId, String colorName) {
        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;
        QProductImage productImage = QProductImage.productImage;
        QInterest interest = QInterest.interest;
        QBrand brand = QBrand.brand;

        // 기본 정보와 썸네일 이미지 조인
        Tuple result = queryFactory.select(
                        product.id,
                        product.name,
                        product.englishName,
                        product.releasePrice,
                        productColor.id,
                        productColor.colorName,
                        productImage.imageUrl, // 썸네일 이미지 URL
                        productColor.content,
                        interest.count(),
                        product.brand.name
                )
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(product.brand, brand)
                .leftJoin(productColor.thumbnailImage, productImage) // 썸네일 이미지 조인
                .leftJoin(productColor.interests, interest)
                .where(
                        product.id.eq(productId),
                        productColor.colorName.eq(colorName)
                )
                .groupBy(
                        product.id,
                        productColor.id
                )
                .fetchFirst();

        if (result == null) {
            throw new IllegalArgumentException("해당 상품 또는 색상이 존재하지 않습니다.");
        }

        // 기본 정보 추출
        Long id = result.get(product.id);
        String name = result.get(product.name);
        String englishName = result.get(product.englishName);
        Integer releasePrice = result.get(product.releasePrice);
        Long colorId = result.get(productColor.id);
        String thumbnailImageUrl = result.get(productImage.imageUrl); // 썸네일 이미지 URL 가져오기
        String content = result.get(productColor.content);
        Long interestCount = result.get(interest.count());
        String brandName=result.get(product.brand.name);

        // ProductSize 정보 리스트 생성
        List<ProductDetailResponseDto.SizeDetailDto> sizeDetails = queryFactory
                .selectFrom(productSize)
                .where(productSize.productColor.colorName.eq(colorName))
                .fetch()
                .stream()
                .map(size -> ProductDetailResponseDto.SizeDetailDto.builder()
                        .size(size.getSize())
                        .purchasePrice(size.getPurchasePrice())
                        .salePrice(size.getSalePrice())
                        .quantity(size.getQuantity())
                        .build())
                .toList();

        // 다른 색상 정보 리스트 생성
        List<ProductDetailResponseDto.ColorDetailDto> otherColors = queryFactory.select(
                        productColor.id,
                        productColor.colorName,
                        productImage.imageUrl,
                        productColor.content
                )
                .from(productColor)
                .leftJoin(productColor.thumbnailImage, productImage)
                .where(
                        productColor.product.id.eq(productId),
                        productColor.colorName.ne(colorName) // 현재 색상 제외
                )
                .fetch()
                .stream()
                .map(color -> ProductDetailResponseDto.ColorDetailDto.builder()
                        .colorId(color.get(productColor.id))
                        .colorName(color.get(productColor.colorName))
                        .thumbnailImageUrl(color.get(productImage.imageUrl))
                        .content(color.get(productColor.content))
                        .build())
                .toList();

        // DTO 생성 및 반환
        return ProductDetailResponseDto.builder()
                .id(id)
                .name(name)
                .englishName(englishName)
                .releasePrice(releasePrice)
                .thumbnailImageUrl(thumbnailImageUrl)
                .brandName(brandName)
                .colorId(colorId)
                .colorName(colorName)
                .content(content)
                .interestCount(interestCount)
                .sizes(sizeDetails)
                .otherColors(otherColors) // 다른 색상 정보 추가
                .build();
    }

    public Page<ProductSearchResponseDto> searchProductsByColorIds(
            List<Long> colorIds,
            SortOption sortOption,
            Pageable pageable
    ) {
        if (colorIds == null || colorIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // QProduct, QProductColor, QProductSize, etc.
        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;
        QInterest interest = QInterest.interest;

        // 기본 쿼리
        JPQLQuery<Tuple> query = queryFactory.select(
                        product.id,
                        product.name,
                        product.englishName,
                        product.releasePrice,
                        productColor.id,
                        productColor.colorName,
                        productColor.thumbnailImage.imageUrl,
                        productSize.purchasePrice.min(), // 최저 구매가
                        interest.count(),                 // 관심 수
                        product.brand.name
                )
                .from(product)
                .leftJoin(product.colors, productColor)
                .leftJoin(productColor.sizes, productSize)
                .leftJoin(productColor.interests, interest)
                .where(productColor.id.in(colorIds))   // colorId로 필터
                .groupBy(product.id, productColor.id)
                .distinct();

        // 정렬 로직
        if (sortOption != null) {
            // 예: field = "price", order = "asc"
            if ("price".equalsIgnoreCase(sortOption.getField())) {
                query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                        ? productSize.purchasePrice.min().asc()
                        : productSize.purchasePrice.min().desc());
            } else if ("releaseDate".equalsIgnoreCase(sortOption.getField())) {
                query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                        ? product.releaseDate.asc()
                        : product.releaseDate.desc());
            } else if ("interestCount".equalsIgnoreCase(sortOption.getField())) {
                query.orderBy("asc".equalsIgnoreCase(sortOption.getOrder())
                        ? productColor.interests.size().sum().asc()
                        : productColor.interests.size().sum().desc());
            } else {
                // 기본 정렬
                query.orderBy(product.id.asc());
            }
        } else {
            query.orderBy(product.id.asc());
        }

        // 페이징
        long total = query.fetchCount();  // QueryDSL에서 fetchCount() (버전에 따라 .fetch().size()로 대체)
        List<Tuple> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<ProductSearchResponseDto> content = results.stream()
                .map(tuple -> ProductSearchResponseDto.builder()
                        .id(tuple.get(product.id))
                        .name(tuple.get(product.name))
                        .englishName(tuple.get(product.englishName))
                        .releasePrice(tuple.get(product.releasePrice))
                        .colorId(tuple.get(productColor.id))
                        .colorName(tuple.get(productColor.colorName))
                        .thumbnailImageUrl(tuple.get(productColor.thumbnailImage.imageUrl))
                        .price(tuple.get(productSize.purchasePrice.min()))
                        .interestCount(tuple.get(interest.count()))
                        .brandName(tuple.get(product.brand.name))
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public Map<Long, Long> tradeCountQuery(List<Long> colorIds) {
        QOrderBid orderBid = QOrderBid.orderBid;
        QProductSize productSize = QProductSize.productSize;

        List<Tuple> list = queryFactory.select(
                        productSize.productColor.id,
                        orderBid.id.count()
                )
                .from(orderBid)
                .join(orderBid.productSize, productSize)
                .where(
                        productSize.productColor.id.in(colorIds),
                        orderBid.status.eq(BidStatus.COMPLETED)
                )
                .groupBy(productSize.productColor.id)
                .fetch();

        return list.stream().collect(Collectors.toMap(
                t -> t.get(productSize.productColor.id),
                t -> t.get(orderBid.id.count())
        ));
    }
    public Map<Long, Long> styleCountQuery(List<Long> colorIds) {
        QStyleOrderItem styleOrderItem = QStyleOrderItem.styleOrderItem;
        QStyle style = QStyle.style;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProductSize productSize = QProductSize.productSize;

        // 튜플: (colorId, distinctCountOfStyleId)
        List<Tuple> list = queryFactory.select(
                        productSize.productColor.id,
                        styleOrderItem.style.id.countDistinct()
                )
                .from(styleOrderItem)
                .join(styleOrderItem.style, style)
                .join(styleOrderItem.orderItem, orderItem)
                .join(orderItem.productSize, productSize)
                .where(productSize.productColor.id.in(colorIds))
                .groupBy(productSize.productColor.id)
                .fetch();

        // 변환: Map<colorId, styleCount>
        return list.stream().collect(Collectors.toMap(
                t -> t.get(productSize.productColor.id),
                t -> t.get(styleOrderItem.style.id.countDistinct())
        ));
    }



}


//