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
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .join(orderBid.user, user) // User 엔티티 조인
                .join(orderBid.productSize, productSize)
                .join(productSize.productColor, productColor)
                .join(productColor.product, product)
                .join(productColor.thumbnailImage, productImage)
                .leftJoin(orderBid.order, order)
                .leftJoin(order.orderShipment, orderShipment)
                .where(
                        email != null ? user.email.eq(email) : null, // 이메일 조건
                        bidStatus != null ? orderBid.status.stringValue().eq(bidStatus) : null,
                        orderStatus != null ? order.status.stringValue().eq(orderStatus) : null
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count Query
        long total = queryFactory
                .select(orderBid.id.count())
                .from(orderBid)
                .join(orderBid.user, user) // User 엔티티 조인
                .leftJoin(orderBid.order, order)
                .where(
                        email != null ? user.email.eq(email) : null, // 이메일 조건
                        bidStatus != null ? orderBid.status.stringValue().eq(bidStatus) : null,
                        orderStatus != null ? order.status.stringValue().eq(orderStatus) : null
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public OrderBidStatusCountDto countOrderBidsByStatus(String email) {
        QOrderBid orderBid = QOrderBid.orderBid;
        QUser user = QUser.user;

        long pendingCount = queryFactory
                .select(orderBid.count())
                .from(orderBid)
                .where(orderBid.status.eq(BidStatus.PENDING)
                        .and(orderBid.user.email.eq(email)))
                .fetchOne();

        long matchedCount = queryFactory
                .select(orderBid.count())
                .from(orderBid)
                .where(orderBid.status.eq(BidStatus.MATCHED)
                        .and(orderBid.user.email.eq(email)))
                .fetchOne();

        long cancelledOrCompletedCount = queryFactory
                .select(orderBid.count())
                .from(orderBid)
                .where(orderBid.status.in(
                               BidStatus.CANCELLED, BidStatus.COMPLETED)
                        .and(orderBid.user.email.eq(email)))
                .fetchOne();

        return new OrderBidStatusCountDto(pendingCount, matchedCount, cancelledOrCompletedCount);
    }
    @Override
    public OrderBidResponseDto findOrderBidById(Long orderBidId,String email) {
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
                        orderBid.user.email.eq(email) // 이메일 조건 추가
                )
                .fetchOne();
    }

    /**
     * colorId 기준 거래(완료) 수 조회
     *  (OrderBid -> ProductSize -> ProductColor)
     *  status = COMPLETED
     */
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
