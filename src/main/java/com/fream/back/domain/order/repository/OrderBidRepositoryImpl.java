package com.fream.back.domain.order.repository;

import com.fream.back.domain.order.dto.OrderBidResponseDto;
import com.fream.back.domain.order.dto.OrderBidStatusCountDto;
import com.fream.back.domain.order.entity.BidStatus;
import com.fream.back.domain.order.entity.QOrder;
import com.fream.back.domain.order.entity.QOrderBid;
import com.fream.back.domain.product.entity.QProduct;
import com.fream.back.domain.product.entity.QProductColor;
import com.fream.back.domain.product.entity.QProductImage;
import com.fream.back.domain.product.entity.QProductSize;
import com.fream.back.domain.shipment.entity.QOrderShipment;
import com.fream.back.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 입찰 저장소 구현체
 */
@Repository
@RequiredArgsConstructor
public class OrderBidRepositoryImpl implements OrderBidRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderBidResponseDto> findOrderBidsByFilters(String email, String bidStatus, String orderStatus, Pageable pageable) {
        QOrderBid orderBid = QOrderBid.orderBid;
        QOrder order = QOrder.order;
        QProductSize productSize = QProductSize.productSize;
        QProductColor productColor = QProductColor.productColor;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;
        QOrderShipment orderShipment = QOrderShipment.orderShipment;
        QUser user = QUser.user;

        // 조건 빌더 생성
        BooleanBuilder whereBuilder = new BooleanBuilder();

        // 이메일 조건 추가
        if (StringUtils.hasText(email)) {
            whereBuilder.and(user.email.eq(email));
        }

        // 입찰 상태 조건 추가
        if (StringUtils.hasText(bidStatus)) {
            whereBuilder.and(orderBid.status.stringValue().eq(bidStatus));
        }

        // 주문 상태 조건 추가
        if (StringUtils.hasText(orderStatus)) {
            whereBuilder.and(order.status.stringValue().eq(orderStatus));
        }

        // Main Query
        List<OrderBidResponseDto> content = queryFactory
                .select(Projections.constructor(
                        OrderBidResponseDto.class,
                        orderBid.id,
                        product.id,
                        product.name,
                        product.englishName,
                        productSize.size,
                        productColor.colorName,
                        productImage.imageUrl,
                        orderBid.bidPrice,
                        orderBid.status.stringValue(),
                        order.status.stringValue(),
                        orderShipment.status.stringValue(),
                        orderBid.createdDate,
                        orderBid.modifiedDate
                ))
                .from(orderBid)
                .join(orderBid.user, user)
                .join(orderBid.productSize, productSize)
                .join(productSize.productColor, productColor)
                .join(productColor.product, product)
                .join(productColor.thumbnailImage, productImage)
                .leftJoin(orderBid.order, order)
                .leftJoin(order.orderShipment, orderShipment)
                .where(whereBuilder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count Query
        Long total = queryFactory
                .select(orderBid.id.count())
                .from(orderBid)
                .join(orderBid.user, user)
                .leftJoin(orderBid.order, order)
                .where(whereBuilder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public OrderBidStatusCountDto countOrderBidsByStatus(String email) {
        QOrderBid orderBid = QOrderBid.orderBid;

        // 대기 중 상태 개수 조회
        Long pendingCount = queryFactory
                .select(orderBid.count())
                .from(orderBid)
                .where(orderBid.status.eq(BidStatus.PENDING)
                        .and(orderBid.user.email.eq(email)))
                .fetchOne();

        // 매칭 완료 상태 개수 조회
        Long matchedCount = queryFactory
                .select(orderBid.count())
                .from(orderBid)
                .where(orderBid.status.eq(BidStatus.MATCHED)
                        .and(orderBid.user.email.eq(email)))
                .fetchOne();

        // 취소 또는 완료 상태 개수 조회
        Long cancelledOrCompletedCount = queryFactory
                .select(orderBid.count())
                .from(orderBid)
                .where(orderBid.status.in(
                                BidStatus.CANCELLED, BidStatus.COMPLETED)
                        .and(orderBid.user.email.eq(email)))
                .fetchOne();

        // null 값 처리
        pendingCount = pendingCount != null ? pendingCount : 0L;
        matchedCount = matchedCount != null ? matchedCount : 0L;
        cancelledOrCompletedCount = cancelledOrCompletedCount != null ? cancelledOrCompletedCount : 0L;

        return new OrderBidStatusCountDto(pendingCount, matchedCount, cancelledOrCompletedCount);
    }

    @Override
    public OrderBidResponseDto findOrderBidById(Long orderBidId, String email) {
        QOrderBid orderBid = QOrderBid.orderBid;
        QOrder order = QOrder.order;
        QProductSize productSize = QProductSize.productSize;
        QProductColor productColor = QProductColor.productColor;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;
        QOrderShipment orderShipment = QOrderShipment.orderShipment;
        QUser user = QUser.user;

        return queryFactory
                .select(Projections.constructor(
                        OrderBidResponseDto.class,
                        orderBid.id,
                        product.id,
                        product.name,
                        product.englishName,
                        productSize.size,
                        productColor.colorName,
                        productImage.imageUrl,
                        orderBid.bidPrice,
                        orderBid.status.stringValue(),
                        order.status.stringValue(),
                        orderShipment.status.stringValue(),
                        orderBid.createdDate,
                        orderBid.modifiedDate
                ))
                .from(orderBid)
                .join(orderBid.user, user)
                .join(orderBid.productSize, productSize)
                .join(productSize.productColor, productColor)
                .join(productColor.product, product)
                .join(productColor.thumbnailImage, productImage)
                .leftJoin(orderBid.order, order)
                .leftJoin(order.orderShipment, orderShipment)
                .where(
                        orderBid.id.eq(orderBidId),
                        orderBid.user.email.eq(email)
                )
                .fetchOne();
    }

    @Override
    public Map<Long, Long> tradeCountByColorIds(List<Long> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QOrderBid orderBid = QOrderBid.orderBid;
        QProductSize productSize = QProductSize.productSize;

        List<Tuple> results = queryFactory.select(
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

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(productSize.productColor.id),
                        t -> t.get(orderBid.id.count())
                ));
    }
}