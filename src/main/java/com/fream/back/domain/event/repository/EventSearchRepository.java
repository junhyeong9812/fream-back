package com.fream.back.domain.event.repository;

import com.fream.back.domain.event.dto.EventSearchCondition;
import com.fream.back.domain.event.entity.Event;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.fream.back.domain.event.entity.QEvent.event;
import static com.fream.back.domain.product.entity.QBrand.brand;

@Repository
@RequiredArgsConstructor
public class EventSearchRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 조건에 맞는 이벤트 검색 (페이징 포함)
     */
    public Page<Event> searchEvents(EventSearchCondition condition, Pageable pageable) {
        // 조건에 맞는 총 개수 조회
        Long total = queryFactory
                .select(event.count())
                .from(event)
                .join(event.brand, brand)
                .where(
                        keywordContains(condition.getKeyword()),
                        brandIdEquals(condition.getBrandId()),
                        isActiveEquals(condition.getIsActive()),
                        startDateAfter(condition.getStartDate()),
                        endDateBefore(condition.getEndDate())
                )
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 조건에 맞는 이벤트 조회
        List<Event> results = queryFactory
                .selectFrom(event)
                .join(event.brand, brand).fetchJoin()
                .where(
                        keywordContains(condition.getKeyword()),
                        brandIdEquals(condition.getBrandId()),
                        isActiveEquals(condition.getIsActive()),
                        startDateAfter(condition.getStartDate()),
                        endDateBefore(condition.getEndDate())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(pageable.getSort(), event))
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 제목 키워드 검색 조건
     */
    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? event.title.containsIgnoreCase(keyword) : null;
    }

    /**
     * 브랜드 ID 일치 조건
     */
    private BooleanExpression brandIdEquals(Long brandId) {
        return brandId != null ? event.brand.id.eq(brandId) : null;
    }

    /**
     * 활성 상태 조건
     */
    private BooleanExpression isActiveEquals(Boolean isActive) {
        if (isActive == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        if (isActive) {
            // 현재 활성화 상태 (시작일 <= 현재 < 종료일)
            return event.startDate.loe(now).and(event.endDate.gt(now));
        } else {
            // 종료되었거나 아직 시작하지 않은 이벤트 (현재 < 시작일 OR 종료일 <= 현재)
            return event.startDate.gt(now).or(event.endDate.loe(now));
        }
    }

    /**
     * 시작일 이후 조건
     */
    private BooleanExpression startDateAfter(LocalDateTime startDate) {
        return startDate != null ? event.startDate.goe(startDate) : null;
    }

    /**
     * 종료일 이전 조건
     */
    private BooleanExpression endDateBefore(LocalDateTime endDate) {
        return endDate != null ? event.endDate.loe(endDate) : null;
    }

    /**
     * Spring Data JPA의 Sort 객체를 Querydsl의 OrderSpecifier 배열로 변환
     */
    @SuppressWarnings("unchecked")
    private OrderSpecifier<?>[] getOrderSpecifier(Sort sort, EntityPathBase<?> path) {
        if (sort.isEmpty()) {
            return new OrderSpecifier[0];
        }

        PathBuilder<Object> pathBuilder = new PathBuilder<>(Object.class, path.getMetadata());

        return sort.stream()
                .map(order -> {
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                    return new OrderSpecifier(direction, pathBuilder.get(order.getProperty()));
                })
                .toArray(OrderSpecifier[]::new);
    }

    /**
     * 단일 OrderSpecifier 생성
     */
    @SuppressWarnings("unchecked")
    private <T extends Comparable<?>> OrderSpecifier<?> getSingleOrderSpecifier(
            String property, Order direction, Path<T> path
    ) {
        PathBuilder<Object> pathBuilder = new PathBuilder<>(Object.class, path.getMetadata());
        return new OrderSpecifier(direction, pathBuilder.get(property));
    }
}