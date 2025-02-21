package com.fream.back.domain.style.repository;

import com.fream.back.domain.order.entity.QOrderItem;
import com.fream.back.domain.product.entity.QProduct;
import com.fream.back.domain.product.entity.QProductColor;
import com.fream.back.domain.product.entity.QProductImage;
import com.fream.back.domain.product.entity.QProductSize;
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

    @Override
    public Page<StyleResponseDto> filterStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable) {
        QStyle style = QStyle.style;
        QProfile profile = QProfile.profile;
        QStyleOrderItem styleOrderItem = QStyleOrderItem.styleOrderItem;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMediaUrl mediaUrl = QMediaUrl.mediaUrl;

        BooleanBuilder builder = new BooleanBuilder();

        // 필터 조건 추가
        if (filterRequestDto.getBrandName() != null) {
            builder.and(orderItem.productSize.productColor.product.brand.name.eq(filterRequestDto.getBrandName()));
        }
        if (filterRequestDto.getCollectionName() != null) {
            builder.and(orderItem.productSize.productColor.product.collection.name.eq(filterRequestDto.getCollectionName()));
        }
        if (filterRequestDto.getCategoryId() != null) {
            builder.and(orderItem.productSize.productColor.product.category.id.eq(filterRequestDto.getCategoryId()));
        }
        if (filterRequestDto.getProfileName() != null) {
            builder.and(profile.profileName.eq(filterRequestDto.getProfileName()));
        }

        // 쿼리 생성
        var query = queryFactory.select(Projections.constructor(
                        StyleResponseDto.class,
                        style.id,
                        profile.profileName,
                        profile.profileImageUrl,
                        style.content,
                        // 가장 먼저 저장된 MediaUrl
                        JPAExpressions.select(mediaUrl.url)
                                .from(mediaUrl)
                                .where(mediaUrl.style.eq(style))
                                .orderBy(mediaUrl.id.asc())
                                .limit(1),
                        style.viewCount,
                        style.likes.size() // 좋아요 수 계산
                ))
                .from(style)
                .leftJoin(style.profile, profile)
                .leftJoin(style.styleOrderItems, styleOrderItem)
                .leftJoin(styleOrderItem.orderItem, orderItem)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 정렬
        if ("popular".equals(filterRequestDto.getSortBy())) {
            query.orderBy(style.likes.size().desc());
        } else {
            query.orderBy(style.id.desc());
        }

        // 데이터 조회
        List<StyleResponseDto> content = query.fetch();

        // 총 카운트 계산
        var countQuery = queryFactory.select(style.count())
                .from(style)
                .leftJoin(style.styleOrderItems, styleOrderItem)
                .leftJoin(styleOrderItem.orderItem, orderItem)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
                .distinct()
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
