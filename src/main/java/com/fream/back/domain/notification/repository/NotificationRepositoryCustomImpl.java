package com.fream.back.domain.notification.repository;

import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.entity.QNotification;
import com.fream.back.domain.user.entity.QUser;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QNotification notification = QNotification.notification;
    private final QUser user = QUser.user;

    @Override
    public List<Notification> findByUserEmailAndCategory(String email, NotificationCategory category) {
        return queryFactory
                .selectFrom(notification)
                .join(notification.user, user).fetchJoin()
                .where(
                        userEmailEq(email),
                        categoryEq(category)
                )
                .orderBy(notification.createdDate.desc())
                .fetch();
    }

    @Override
    public List<Notification> findByUserEmailAndType(String email, NotificationType type) {
        return queryFactory
                .selectFrom(notification)
                .join(notification.user, user).fetchJoin()
                .where(
                        userEmailEq(email),
                        typeEq(type)
                )
                .orderBy(notification.createdDate.desc())
                .fetch();
    }

    @Override
    public Page<Notification> findByUserEmailAndIsRead(String email, boolean isRead, Pageable pageable) {
        List<Notification> content = queryFactory
                .selectFrom(notification)
                .join(notification.user, user).fetchJoin()
                .where(
                        userEmailEq(email),
                        isReadEq(isRead)
                )
                .orderBy(getOrderSpecifiers(pageable).toArray(OrderSpecifier[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(notification.count())
                .from(notification)
                .join(notification.user, user)
                .where(
                        userEmailEq(email),
                        isReadEq(isRead)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Notification> findByUserEmailAndCategoryAndIsRead(
            String email, NotificationCategory category, boolean isRead, Pageable pageable) {
        List<Notification> content = queryFactory
                .selectFrom(notification)
                .join(notification.user, user).fetchJoin()
                .where(
                        userEmailEq(email),
                        categoryEq(category),
                        isReadEq(isRead)
                )
                .orderBy(getOrderSpecifiers(pageable).toArray(OrderSpecifier[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(notification.count())
                .from(notification)
                .join(notification.user, user)
                .where(
                        userEmailEq(email),
                        categoryEq(category),
                        isReadEq(isRead)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Notification> findByUserEmailAndTypeAndIsRead(
            String email, NotificationType type, boolean isRead, Pageable pageable) {
        List<Notification> content = queryFactory
                .selectFrom(notification)
                .join(notification.user, user).fetchJoin()
                .where(
                        userEmailEq(email),
                        typeEq(type),
                        isReadEq(isRead)
                )
                .orderBy(getOrderSpecifiers(pageable).toArray(OrderSpecifier[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(notification.count())
                .from(notification)
                .join(notification.user, user)
                .where(
                        userEmailEq(email),
                        typeEq(type),
                        isReadEq(isRead)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Notification> findAllByUserEmail(String email) {
        return queryFactory
                .selectFrom(notification)
                .join(notification.user, user).fetchJoin()
                .where(userEmailEq(email))
                .orderBy(notification.createdDate.desc())
                .fetch();
    }

    /**
     * 동적 조건 - 사용자 이메일
     */
    private BooleanExpression userEmailEq(String email) {
        return email != null ? user.email.eq(email) : null;
    }

    /**
     * 동적 조건 - 알림 카테고리
     */
    private BooleanExpression categoryEq(NotificationCategory category) {
        return category != null ? notification.category.eq(category) : null;
    }

    /**
     * 동적 조건 - 알림 유형
     */
    private BooleanExpression typeEq(NotificationType type) {
        return type != null ? notification.type.eq(type) : null;
    }

    /**
     * 동적 조건 - 읽음 상태
     */
    private BooleanExpression isReadEq(Boolean isRead) {
        return isRead != null ? notification.isRead.eq(isRead) : null;
    }

    /**
     * Pageable 객체에서 정렬 조건 추출
     */
    private List<OrderSpecifier<?>> getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        // 기본 정렬은 생성일 내림차순
        if (pageable.getSort().isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, notification.createdDate));
            return orders;
        }

        // 페이지 정렬 조건이 있으면 그에 맞게 처리
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

            switch (order.getProperty()) {
                case "createdDate":
                    orders.add(new OrderSpecifier<>(direction, notification.createdDate));
                    break;
                case "category":
                    orders.add(new OrderSpecifier<>(direction, notification.category));
                    break;
                case "type":
                    orders.add(new OrderSpecifier<>(direction, notification.type));
                    break;
                case "isRead":
                    orders.add(new OrderSpecifier<>(direction, notification.isRead));
                    break;
                default:
                    orders.add(new OrderSpecifier<>(Order.DESC, notification.createdDate));
                    break;
            }
        }

        return orders;
    }
}