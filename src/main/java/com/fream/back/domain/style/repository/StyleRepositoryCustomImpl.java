package com.fream.back.domain.style.repository;

import com.fream.back.domain.order.entity.QOrderItem;
import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import com.fream.back.domain.style.entity.QMediaUrl;
import com.fream.back.domain.style.entity.QStyle;
import com.fream.back.domain.style.entity.QStyleOrderItem;
import com.fream.back.domain.user.entity.QProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StyleRepositoryCustomImpl implements StyleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

//    @Override
//    public Page<StyleResponseDto> filterStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable) {
//        QStyle style = QStyle.style;
//        QProfile profile = QProfile.profile;
//        QStyleOrderItem styleOrderItem = QStyleOrderItem.styleOrderItem;
//        QOrderItem orderItem = QOrderItem.orderItem;
//        QMediaUrl mediaUrl = QMediaUrl.mediaUrl;
//        QProductSize productSize=QProductSize.productSize;
//        QProductColor productColor = QProductColor.productColor;
//        QProduct product = QProduct.product;
//        QBrand brand = QBrand.brand;
//
//        BooleanBuilder builder = new BooleanBuilder();
//
//        if (filterRequestDto != null) {
//            // 필터 조건 추가
////            if (filterRequestDto.getBrandName() != null) {
////                builder.and(orderItem.isNotNull()
////                        .and(orderItem.productSize.isNotNull())
////                        .and(orderItem.productSize.productColor.isNotNull())
////                        .and(orderItem.productSize.productColor.product.isNotNull())
////                        .and(orderItem.productSize.productColor.product.brand.name.eq(filterRequestDto.getBrandName())));
////            }
//            if (filterRequestDto.getBrandName() != null) {
//                builder.and(orderItem.isNotNull()
//                        .and(orderItem.productSize.isNotNull())
//                        .and(orderItem.productSize.productColor.isNotNull())
//                        .and(orderItem.productSize.productColor.product.isNotNull())
//                        .and(orderItem.productSize.productColor.product.brand.isNotNull())
//                        .and(orderItem.productSize.productColor.product.brand.name.eq(filterRequestDto.getBrandName())));
//            }
//            if (filterRequestDto.getCollectionName() != null) {
//                builder.and(orderItem.isNotNull()
//                        .and(orderItem.productSize.isNotNull())
//                        .and(orderItem.productSize.productColor.isNotNull())
//                        .and(orderItem.productSize.productColor.product.isNotNull())
//                        .and(orderItem.productSize.productColor.product.collection.isNotNull())
//                        .and(orderItem.productSize.productColor.product.collection.name.eq(filterRequestDto.getCollectionName())));
//            }
//
//            if (filterRequestDto.getCategoryId() != null) {
//                builder.and(orderItem.isNotNull()
//                        .and(orderItem.productSize.isNotNull())
//                        .and(orderItem.productSize.productColor.isNotNull())
//                        .and(orderItem.productSize.productColor.product.isNotNull())
//                        .and(orderItem.productSize.productColor.product.category.isNotNull())
//                        .and(orderItem.productSize.productColor.product.category.id.eq(filterRequestDto.getCategoryId())));
//            }
//
//            if (filterRequestDto.getProfileName() != null) {
//                builder.and(profile.isNotNull()
//                        .and(profile.profileName.eq(filterRequestDto.getProfileName())));
//            }
//        }
//
//        // 쿼리 생성
//        var query = queryFactory.select(Projections.constructor(
//                        StyleResponseDto.class,
//                        style.id,
//                        profile.id,
//                        profile.profileName,
//                        profile.profileImageUrl,
//                        style.content,
//                        // 가장 먼저 저장된 MediaUrl
//                        JPAExpressions
//                                .select(mediaUrl.url)
//                                .from(mediaUrl)
//                                .where(mediaUrl.style.eq(style)
//                                        .and(mediaUrl.id.eq(
//                                                JPAExpressions
//                                                        .select(mediaUrl.id.min())
//                                                        .from(mediaUrl)
//                                                        .where(mediaUrl.style.eq(style))
//                                        ))),//.groupBy(mediaUrl.style.id),
//                        style.viewCount,
//                        style.likes.size() // 좋아요 수 계산
//                ))
//                .from(style)
//                .leftJoin(style.profile, profile).fetchJoin()
//                .leftJoin(style.styleOrderItems, styleOrderItem).fetchJoin()
//                .leftJoin(styleOrderItem.orderItem, orderItem).fetchJoin()
//                .leftJoin(orderItem.productSize, productSize).fetchJoin()
//                .leftJoin(productSize.productColor, productColor).fetchJoin()
//                .leftJoin(productColor.product, product).fetchJoin()
//                .leftJoin(product.brand, brand).fetchJoin()
//                .where(builder)
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize());
//
//        // 정렬
////        if (filterRequestDto != null && "popular".equals(filterRequestDto.getSortBy())) {
////            query.orderBy(style.likes.size().desc());
////        } else {
////            query.orderBy(style.id.desc());
////        }
//        if (filterRequestDto != null && "popular".equals(filterRequestDto.getSortBy())) {
//            query.orderBy(style.viewCount.desc(), style.id.desc()); // Sort by viewCount, then by id for ties
//        } else {
//            query.orderBy(style.id.desc()); // Default to latest
//        }
//
//        // 데이터 조회
//        List<StyleResponseDto> content = query.fetch();
//
//        // 총 카운트 계산
//        var countQuery = queryFactory.select(style.count())
//                .from(style)
//                .leftJoin(style.styleOrderItems, styleOrderItem)
//                .leftJoin(styleOrderItem.orderItem, orderItem)
//                .where(builder);
//
//        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
//    }
@Override
public Page<StyleResponseDto> filterStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable) {
    QStyle style = QStyle.style;
    QProfile profile = QProfile.profile;
    QStyleOrderItem styleOrderItem = QStyleOrderItem.styleOrderItem;
    QOrderItem orderItem = QOrderItem.orderItem;
    QMediaUrl mediaUrl = QMediaUrl.mediaUrl;
    QProductSize productSize = QProductSize.productSize;
    QProductColor productColor = QProductColor.productColor;
    QProduct product = QProduct.product;
    QBrand brand = QBrand.brand;

    // 1단계: 필터링 조건에 맞는 스타일 ID 찾기
    BooleanBuilder filterBuilder = new BooleanBuilder();

    if (filterRequestDto != null) {
        if (filterRequestDto.getBrandName() != null) {
            filterBuilder.and(brand.name.eq(filterRequestDto.getBrandName()));
        }
        if (filterRequestDto.getCollectionName() != null) {
            filterBuilder.and(product.collection.name.eq(filterRequestDto.getCollectionName()));
        }
        if (filterRequestDto.getCategoryId() != null) {
            filterBuilder.and(product.category.id.eq(filterRequestDto.getCategoryId()));
        }
        if (filterRequestDto.getProfileName() != null) {
            filterBuilder.and(profile.profileName.eq(filterRequestDto.getProfileName()));
        }
    }

    List<Long> filteredStyleIds = queryFactory
            .select(style.id)
            .from(style)
            .leftJoin(style.profile, profile)
            .leftJoin(style.styleOrderItems, styleOrderItem)
            .leftJoin(styleOrderItem.orderItem, orderItem)
            .leftJoin(orderItem.productSize, productSize)
            .leftJoin(productSize.productColor, productColor)
            .leftJoin(productColor.product, product)
            .leftJoin(product.brand, brand)
            .where(filterBuilder)
            .distinct()
            .fetch();

    // 2단계: 찾은 ID로 실제 필요한 데이터 조회
    var query = queryFactory.select(Projections.constructor(
                    StyleResponseDto.class,
                    style.id,
                    profile.id,
                    profile.profileName,
                    profile.profileImageUrl,
                    style.content,
                    // 가장 먼저 저장된 MediaUrl
                    JPAExpressions
                            .select(mediaUrl.url)
                            .from(mediaUrl)
                            .where(mediaUrl.style.eq(style)
                                    .and(mediaUrl.id.eq(
                                            JPAExpressions
                                                    .select(mediaUrl.id.min())
                                                    .from(mediaUrl)
                                                    .where(mediaUrl.style.eq(style))
                                    ))),
                    style.viewCount,
                    style.likes.size() // 좋아요 수 계산
            ))
            .from(style)
            .leftJoin(style.profile, profile)
            .where(style.id.in(filteredStyleIds))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize());

    // 정렬
    if (filterRequestDto != null && "popular".equals(filterRequestDto.getSortBy())) {
        query.orderBy(style.viewCount.desc(), style.id.desc());
    } else {
        query.orderBy(style.id.desc());
    }

    // 데이터 조회
    List<StyleResponseDto> content = query.fetch();

    // 총 카운트 계산
    long total = filteredStyleIds.size();

    return PageableExecutionUtils.getPage(content, pageable, () -> total);
}


    @Override
    public StyleDetailResponseDto getStyleDetail(Long styleId) {
        QStyle style = QStyle.style;
        QProfile profile = QProfile.profile;
        QMediaUrl mediaUrl = QMediaUrl.mediaUrl;
        QStyleOrderItem styleOrderItem = QStyleOrderItem.styleOrderItem;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductImage productImage = QProductImage.productImage;
        QProductSize productSize = QProductSize.productSize;

        // 1. 미디어 URL 목록 조회
        List<String> mediaUrls = queryFactory
                .select(mediaUrl.url)
                .from(mediaUrl)
                .where(mediaUrl.style.id.eq(styleId))
                .orderBy(mediaUrl.id.asc())
                .fetch();


        List<StyleDetailResponseDto.ProductInfoDto> productInfos = queryFactory
                .select(Projections.fields(
                        StyleDetailResponseDto.ProductInfoDto.class,
                        product.name.as("productName"),
                        product.englishName.as("productEnglishName"),
                        productColor.thumbnailImage.imageUrl.as("thumbnailImageUrl"),
                        productSize.salePrice.min().as("minSalePrice")
                ))
                .from(styleOrderItem)
                .leftJoin(styleOrderItem.orderItem, orderItem)
                .leftJoin(orderItem.productSize, productSize)
                .leftJoin(productSize.productColor, productColor)
                .leftJoin(productColor.product, product)
                .where(styleOrderItem.style.id.eq(styleId)
                        .and(orderItem.isNotNull())
                        .and(productSize.isNotNull())
                        .and(productColor.isNotNull()))
//                .distinct()
                .groupBy(product.id, productColor.id)
                .fetch();

        // 3. 스타일 정보 조회
        StyleDetailResponseDto styleDetail = queryFactory
                .select(Projections.fields(
                        StyleDetailResponseDto.class,
                        style.id.as("id"),
                        profile.id.as("profileId"),
                        profile.profileName.as("profileName"),
                        profile.profileImageUrl.as("profileImageUrl"),
                        style.content.as("content"),
                        ExpressionUtils.as(style.likes.size().longValue(), "likeCount"),
                        ExpressionUtils.as(style.comments.size().longValue(), "commentCount"),
                        style.createdDate.as("createdDate")
                ))
                .from(style)
                .leftJoin(style.profile, profile)
                .where(style.id.eq(styleId))
                .fetchOne();

        // 4. 미디어 URL과 상품 정보를 DTO에 주입
        if (styleDetail != null) {
            styleDetail.setMediaUrls(mediaUrls);
            styleDetail.setProductInfos(productInfos);
        }

        return styleDetail;
    }


    @Override
    public Page<ProfileStyleResponseDto> getStylesByProfile(Long profileId, Pageable pageable) {
        QStyle style = QStyle.style;
        QProfile profile = QProfile.profile;
        QMediaUrl mediaUrl = QMediaUrl.mediaUrl;

        // 첫 번째 미디어 URL 조회
        var query = queryFactory.select(Projections.constructor(
                        ProfileStyleResponseDto.class,
                        style.id,
                        JPAExpressions.select(mediaUrl.url)
                                .from(mediaUrl)
                                .where(mediaUrl.style.id.eq(style.id))
                                .orderBy(mediaUrl.id.asc()) // 첫 번째 URL 가져오기
                                .limit(1),
                        style.likes.size().longValue()  // 좋아요 수
                ))
                .from(style)
                .leftJoin(style.profile, profile)
                .where(profile.id.eq(profileId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 데이터 조회
        List<ProfileStyleResponseDto> content = query.fetch();

        // 총 카운트 계산
        var countQuery = queryFactory.select(style.count())
                .from(style)
                .leftJoin(style.profile, profile)
                .where(profile.id.eq(profileId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
    /**
     * colorId 기준 스타일 수 조회
     *  (StyleOrderItem -> OrderItem -> ProductSize -> ProductColor)
     *  GROUP BY productColor.id
     */
    public Map<Long, Long> styleCountByColorIds(List<Long> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QStyleOrderItem styleOrderItem = QStyleOrderItem.styleOrderItem;
        QStyle style = QStyle.style;
        QOrderItem orderItem = QOrderItem.orderItem;
        QProductSize productSize = QProductSize.productSize;

        // select (colorId, countDistinct(style.id))
        List<Tuple> results = queryFactory.select(
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

        // 결과 -> Map<colorId, styleCount>
        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(productSize.productColor.id),
                        t -> t.get(styleOrderItem.style.id.countDistinct())
                ));
    }
}
