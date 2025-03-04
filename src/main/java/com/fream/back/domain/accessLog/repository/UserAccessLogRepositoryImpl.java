package com.fream.back.domain.accessLog.repository;

import com.fream.back.domain.accessLog.dto.DailyAccessCountDto;
import com.fream.back.domain.accessLog.entity.QUserAccessLog;
import com.querydsl.core.types.Projections;
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

        // 날짜별 그룹핑
        // QueryDSL에서 날짜별로 그룹핑하려면 -> DB 특정 함수(예: DATE(access_time))가 필요
        // JPA / QueryDSL에서 사용 예: Expressions.stringTemplate("function('date_format', {0}, '%Y-%m-%d')", qLog.accessTime)
        // 데이터베이스에 따라 date_trunc(PSQL), DATE_FORMAT(MySQL) 등이 달라집니다.

        return queryFactory
                .select(Projections.constructor(
                        DailyAccessCountDto.class,
                        // 날짜(문자열로 변환 or LocalDate)
                        // MySQL 예시: date_format(access_time, '%Y-%m-%d') as dateStr
                        // 여기는 MySQL 기반 예제
                        com.querydsl.core.types.dsl.Expressions.stringTemplate(
                                "DATE_FORMAT({0}, '%Y-%m-%d')", qLog.accessTime
                        ).as("dateString"),
                        qLog.count() // count(*)
                ))
                .from(qLog)
                .where(qLog.accessTime.between(sevenDaysAgo, endOfToday))
                .groupBy(com.querydsl.core.types.dsl.Expressions.stringTemplate(
                        "DATE_FORMAT({0}, '%Y-%m-%d')", qLog.accessTime
                ))
                .orderBy(com.querydsl.core.types.dsl.Expressions.stringTemplate(
                        "DATE_FORMAT({0}, '%Y-%m-%d')", qLog.accessTime
                ).asc())
                .fetch();
    }
}
