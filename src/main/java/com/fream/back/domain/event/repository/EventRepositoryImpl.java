package com.fream.back.domain.event.repository;

import com.fream.back.domain.event.dto.EventSearchCondition;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.EventStatus;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.fream.back.domain.event.entity.QEvent.event;
import static com.fream.back.domain.product.entity.QBrand.brand;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 검색 조건에 따른 이벤트 페이징 조회
     */
    @Override
    public Page<Event> searchEvents(EventSearchCondition condition, Pageable pageable) {
        log.debug("이벤트 검색 수행: condition={}", condition);

        // 조건에 맞는 결과 조회
        JPAQuery<Event> query = queryFactory
                .selectFrom(event)
                .join(event.brand, brand).fetchJoin()
                .where(
                        keywordContains(condition.getKeyword()),
                        brandIdEquals(condition.getBrandId()),
                        isActiveEquals(condition.getIsActive()),
                        startDateAfter(condition.getStartDate()),
                        endDateBefore(condition.getEndDate()),
                        statusEquals(condition.getStatus())
                );

        // 카운트 쿼리 최적화를 위한 별도 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(event.count())
                .from(event)
                .join(event.brand, brand)
                .where(
                        keywordContains(condition.getKeyword()),
                        brandIdEquals(condition.getBrandId()),
                        isActiveEquals(condition.getIsActive()),
                        startDateAfter(condition.getStartDate()),
                        endDateBefore(condition.getEndDate()),
                        statusEquals(condition.getStatus())
                );

        // 정렬 및 페이징 처리
        OrderSpecifier<?>[] orderSpecifiers = getOrderSpecifier(pageable.getSort(), event);
        List<Event> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifiers)
                .fetch();

        // 결과가 없으면 빈 페이지 반환 (최적화)
        if (content.isEmpty()) {
            return Page.empty(pageable);
        }

        // 조회 결과 로깅
        log.debug("이벤트 검색 결과: 개수={}", content.size());

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
     * 이벤트 상태 조건 (계산된 필드이므로 시작일/종료일로 변환하여 조회)
     */
    private BooleanExpression statusEquals(EventStatus status) {
        if (status == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        switch (status) {
            case UPCOMING:
                // 시작일이 현재보다 미래인 이벤트
                return event.startDate.gt(now);
            case ACTIVE:
                // 현재 진행 중인 이벤트 (시작일 <= 현재 < 종료일)
                return event.startDate.loe(now).and(event.endDate.gt(now));
            case ENDED:
                // 종료된 이벤트 (종료일 <= 현재)
                return event.endDate.loe(now);
            default:
                return null;
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
            // 기본 정렬 (ID 내림차순)
            return new OrderSpecifier[]{
                    new OrderSpecifier(Order.DESC, new PathBuilder(path.getType(), path.getMetadata()).get("id"))
            };
        }

        PathBuilder<Object> pathBuilder = new PathBuilder<>(Object.class, path.getMetadata());

        return sort.stream()
                .map(order -> {
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                    return new OrderSpecifier(direction, pathBuilder.get(order.getProperty()));
                })
                .toArray(OrderSpecifier[]::new);
    }
}