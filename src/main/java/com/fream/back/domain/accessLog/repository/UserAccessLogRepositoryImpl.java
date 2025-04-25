package com.fream.back.domain.accessLog.repository;

import com.fream.back.domain.accessLog.dto.DailyAccessCountDto;
import com.fream.back.domain.accessLog.entity.QUserAccessLog;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class UserAccessLogRepositoryImpl implements UserAccessLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countTodayAccesses() {
        QUserAccessLog qLog = QUserAccessLog.userAccessLog;

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay(); // 오늘 00:00
        LocalDateTime now = LocalDateTime.now();                     // 현재

        return queryFactory
                .select(qLog.count())
                .from(qLog)
                .where(qLog.accessTime.between(startOfToday, now))
                .fetchOne();
    }

    /**
     * ip기반 접속자 수
     */
    @Override
    public long countTodayUniqueVisitors() {
        QUserAccessLog qLog = QUserAccessLog.userAccessLog;

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay(); // 오늘 00:00
        LocalDateTime now = LocalDateTime.now();                     // 현재

        return queryFactory
                .select(qLog.ipAddress.countDistinct())
                .from(qLog)
                .where(qLog.accessTime.between(startOfToday, now))
                .fetchOne();
    }

    @Override
    public List<DailyAccessCountDto> findRecent7DaysAccessCount() {
        QUserAccessLog qLog = QUserAccessLog.userAccessLog;

        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        // 예: 오늘이 2/1이면, 1/26 00:00부터 2/1 23:59:59까지
        LocalDateTime endOfToday = LocalDate.now().plusDays(1).atStartOfDay().minusNanos(1);

        // DB에 맞는 날짜 추출 함수 (DB 종속적이지 않게)
        StringTemplate dateFormat = Expressions.stringTemplate(
                "function('DATE_FORMAT', {0}, {1})",
                qLog.accessTime,
                "%Y-%m-%d"
        );

        return queryFactory
                .select(Projections.constructor(
                        DailyAccessCountDto.class,
                        dateFormat.as("dateString"),
                        qLog.count()
                ))
                .from(qLog)
                .where(qLog.accessTime.between(sevenDaysAgo, endOfToday))
                .groupBy(dateFormat)
                .orderBy(dateFormat.asc())
                .fetch();
    }
}