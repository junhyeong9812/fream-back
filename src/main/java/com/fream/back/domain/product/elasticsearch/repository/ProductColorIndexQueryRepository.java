package com.fream.back.domain.product.elasticsearch.repository;

import com.fream.back.domain.product.elasticsearch.dto.ProductColorIndexDto;
import com.fream.back.domain.product.elasticsearch.dto.ProductColorSizeRow;
import com.fream.back.domain.product.entity.*;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductColorIndexQueryRepository {

    private final JPAQueryFactory queryFactory;
    // (1) "기본 정보" + minPrice, maxPrice, interestCount
    public List<ProductColorIndexDto> findAllForIndexingDto() {
        QProductColor pc = QProductColor.productColor;
        QProduct p = QProduct.product;
        QBrand b = QBrand.brand;
        QCategory c = QCategory.category;
        QCollection coll = QCollection.collection;
        QProductSize ps = QProductSize.productSize;
        QInterest it = QInterest.interest;
        QProductImage pi = QProductImage.productImage; // 썸네일 이미지 엔티티

        return queryFactory
                .select(Projections.fields(
                        ProductColorIndexDto.class,
                        pc.id.as("colorId"),
                        p.id.as("productId"),
                        p.name.as("productName"),
                        p.englishName.as("productEnglishName"),
                        b.name.as("brandName"),
                        c.name.as("categoryName"),
                        coll.name.as("collectionName"),
                        b.id.as("brandId"),
                        c.id.as("categoryId"),
                        coll.id.as("collectionId"),
                        pc.colorName.as("colorName"),
//                        p.gender.stringValue().as("gender"),
                        p.gender.as("gender"),
                        p.releasePrice.as("releasePrice"),
                        ps.purchasePrice.min().as("minPrice"),
                        ps.purchasePrice.max().as("maxPrice"),
                        it.id.countDistinct().as("interestCount"),
                        p.releaseDate.as("releaseDate"),
                        pi.imageUrl.as("thumbnailUrl")
                ))
                .from(pc)
                .leftJoin(pc.product, p)
                .leftJoin(p.brand, b)
                .leftJoin(p.category, c)
                .leftJoin(p.collection, coll)
                .leftJoin(pc.thumbnailImage, pi)
                .leftJoin(pc.sizes, ps)      // minPrice, maxPrice를 위해 sizes 조인
                .leftJoin(pc.interests, it) // interestCount를 위해
                .groupBy(pc.id)
                .fetch();
    }

    // (2) 사이즈 목록: colorId, size
    public List<ProductColorSizeRow> findAllSizesForIndexing() {
        QProductColor pc = QProductColor.productColor;
        QProductSize ps = QProductSize.productSize;

        return queryFactory
                .select(Projections.fields(
                        ProductColorSizeRow.class,
                        pc.id.as("colorId"),
                        ps.size.as("size")
                ))
                .from(pc)
                .leftJoin(pc.sizes, ps)
                .fetch();
    }

    // 단건 조회(특정 colorId)도 동일한 방식
    public ProductColorIndexDto findOneForIndexingDto(Long colorId) {
        QProductColor pc = QProductColor.productColor;
        QProduct p = QProduct.product;
        QBrand b = QBrand.brand;
        QCategory c = QCategory.category;
        QCollection coll = QCollection.collection;
        QProductSize ps = QProductSize.productSize;
        QInterest it = QInterest.interest;
        QProductImage pi = QProductImage.productImage; // 썸네일 이미지 엔티티

        return queryFactory
                .select(Projections.fields(
                        ProductColorIndexDto.class,
                        pc.id.as("colorId"),
                        p.id.as("productId"),
                        p.name.as("productName"),
                        p.englishName.as("productEnglishName"),
                        b.name.as("brandName"),
                        c.name.as("categoryName"),
                        coll.name.as("collectionName"),
                        b.id.as("brandId"),
                        c.id.as("categoryId"),
                        coll.id.as("collectionId"),
                        pc.colorName.as("colorName"),
//                        p.gender.stringValue().as("gender"),
                        p.gender.as("gender"),
                        p.releasePrice.as("releasePrice"),
                        ps.purchasePrice.min().as("minPrice"),
                        ps.purchasePrice.max().as("maxPrice"),
                        it.id.countDistinct().as("interestCount"),
                        p.releaseDate.as("releaseDate"),
                        pi.imageUrl.as("thumbnailUrl")
                ))
                .from(pc)
                .leftJoin(pc.product, p)
                .leftJoin(p.brand, b)
                .leftJoin(p.category, c)
                .leftJoin(p.collection, coll)
                .leftJoin(pc.thumbnailImage, pi)
                .leftJoin(pc.sizes, ps)
                .leftJoin(pc.interests, it)
                .where(pc.id.eq(colorId))
                .groupBy(pc.id)
                .fetchOne();
    }
    public List<ProductColorSizeRow> findSizesByColorId(Long colorId) {
        QProductColor pc = QProductColor.productColor;
        QProductSize ps = QProductSize.productSize;

        return queryFactory
                .select(Projections.fields(
                        ProductColorSizeRow.class,
                        pc.id.as("colorId"),
                        ps.size.as("size")
                ))
                .from(pc)
                .leftJoin(pc.sizes, ps)
                .where(pc.id.eq(colorId))
                .fetch();
    }


//    public List<ProductColor> findAllForIndexing() {
//        QProductColor productColor = QProductColor.productColor;
//        QProduct product = QProduct.product;
//        QBrand brand = QBrand.brand;
//        QCategory category = QCategory.category;
//        QCollection collection = QCollection.collection;
//        QProductSize productSize = QProductSize.productSize;
//        QInterest interest = QInterest.interest;
//
//        // productColor와 연관된 모든 정보(brand, category, collection 등) 조인
//        // size들의 purchasePrice 최솟값 / 최댓값, interestCount(관심 수)도 같이 구할 수 있음
//        // 아래는 예시 로직. 실제로는 groupBy, min(), max() 등 사용
//
//        return queryFactory
//                .selectFrom(productColor)
//                .leftJoin(productColor.product, product)
//                .leftJoin(product.brand, brand)
//                .leftJoin(product.category, category)
//                .leftJoin(product.collection, collection)
//                .leftJoin(productColor.sizes, productSize)
//                .leftJoin(productColor.interests, interest)
//                .distinct()
//                .fetch();
//    }
//    public ProductColor findOneForIndexing(Long colorId) {
//        QProductColor productColor = QProductColor.productColor;
//        QProduct product = QProduct.product;
//        QBrand brand = QBrand.brand;
//        QCategory category = QCategory.category;
//        QCollection collection = QCollection.collection;
//        QProductSize productSize = QProductSize.productSize;
//        QInterest interest = QInterest.interest;
//
//        return queryFactory
//                .selectFrom(productColor)
//                .leftJoin(productColor.product, product)
//                .leftJoin(product.brand, brand)     // brand
//                .leftJoin(product.category, category)  // category
//                .leftJoin(product.collection, collection)  // collection
//                .leftJoin(productColor.sizes, productSize)
//                .leftJoin(productColor.interests, interest)
//                .where(productColor.id.eq(colorId))
//                .distinct()
//                .fetchOne();
//    }
}
