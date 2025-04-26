package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.dto.log.GPTDailyUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTModelUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTRequestTypeUsageDto;
import com.fream.back.domain.chatQuestion.entity.QGPTUsageLog;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GPTUsageLogRepositoryCustom 인터페이스의 QueryDSL 구현체
 * 복잡한 쿼리를 QueryDSL을 사용하여 구현합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class GPTUsageLogRepositoryImpl implements GPTUsageLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QGPTUsageLog gptUsageLog = QGPTUsageLog.gPTUsageLog;

    @Override
    public Integer getTotalTokensUsedBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("특정 기간 토큰 사용량 집계 쿼리 실행: {} ~ {}", startDate, endDate);

        return queryFactory
                .select(gptUsageLog.totalTokens.sum())
                .from(gptUsageLog)
                .where(gptUsageLog.createdDate.between(startDate, endDate))
                .fetchOne();
    }

    @Override
    public List<GPTModelUsageDto> getTokenUsageByModelBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("모델별 토큰 사용량 집계 쿼리 실행: {} ~ {}", startDate, endDate);

        return queryFactory
                .select(Projections.constructor(GPTModelUsageDto.class,
                        gptUsageLog.modelName,
                        gptUsageLog.totalTokens.sum()))
                .from(gptUsageLog)
                .where(gptUsageLog.createdDate.between(startDate, endDate))
                .groupBy(gptUsageLog.modelName)
                .fetch();
    }

    @Override
    public List<GPTDailyUsageDto> getDailyTokenUsage(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("일별 토큰 사용량 집계 쿼리 실행: {} ~ {}", startDate, endDate);

        // MySQL, MariaDB
        String dateFunction = "DATE_FORMAT({0}, '%Y-%m-%d')";

        // PostgreSQL 또는 H2
        // String dateFunction = "TO_CHAR({0}, 'YYYY-MM-DD')";

        return queryFactory
                .select(Projections.constructor(GPTDailyUsageDto.class,
                        Expressions.stringTemplate(dateFunction, gptUsageLog.createdDate),
                        gptUsageLog.totalTokens.sum()))
                .from(gptUsageLog)
                .where(gptUsageLog.createdDate.between(startDate, endDate))
                .groupBy(Expressions.stringTemplate(dateFunction, gptUsageLog.createdDate))
                .orderBy(Expressions.stringTemplate(dateFunction, gptUsageLog.createdDate).asc())
                .fetch();
    }

    @Override
    public List<GPTRequestTypeUsageDto> getTokenUsageByRequestTypeBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("요청 유형별 토큰 사용량 집계 쿼리 실행: {} ~ {}", startDate, endDate);

        return queryFactory
                .select(Projections.constructor(GPTRequestTypeUsageDto.class,
                        gptUsageLog.requestType,
                        gptUsageLog.totalTokens.sum()))
                .from(gptUsageLog)
                .where(gptUsageLog.createdDate.between(startDate, endDate))
                .groupBy(gptUsageLog.requestType)
                .fetch();
    }

    @Override
    public Integer getTotalTokensUsedByUser(Long userId) {
        log.debug("사용자별 토큰 사용량 집계 쿼리 실행: userId={}", userId);

        return queryFactory
                .select(gptUsageLog.totalTokens.sum())
                .from(gptUsageLog)
                .where(gptUsageLog.user.id.eq(userId))
                .fetchOne();
    }
}