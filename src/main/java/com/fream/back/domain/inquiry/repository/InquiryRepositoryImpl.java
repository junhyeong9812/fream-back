package com.fream.back.domain.inquiry.repository;

import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.entity.QInquiry;
import com.fream.back.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 1대1 문의 커스텀 리포지토리 구현체
 * QueryDSL을 활용한 복잡한 쿼리 구현
 */
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<InquirySearchResultDto> searchInquiries(InquirySearchCondition condition, Pageable pageable) {
        QInquiry inquiry = QInquiry.inquiry;
        QUser user = QUser.user;

        // 검색 조건 빌더
        BooleanBuilder builder = new BooleanBuilder();

        // 카테고리 조건
        if (condition.getCategory() != null) {
            builder.and(inquiry.category.eq(condition.getCategory()));
        }

        // 상태 조건
        if (condition.getStatus() != null) {
            builder.and(inquiry.status.eq(condition.getStatus()));
        }

        // 사용자 ID 조건
        if (condition.getUserId() != null) {
            builder.and(inquiry.user.id.eq(condition.getUserId()));
        }

        // 키워드 검색 (제목 또는 내용)
        if (StringUtils.hasText(condition.getKeyword())) {
            builder.and(
                    inquiry.title.containsIgnoreCase(condition.getKeyword())
                            .or(inquiry.content.containsIgnoreCase(condition.getKeyword()))
            );
        }

        // 날짜 범위 검색
        if (condition.getStartDate() != null) {
            builder.and(inquiry.createdDate.goe(condition.getStartDate()));
        }

        if (condition.getEndDate() != null) {
            LocalDateTime endOfDay = condition.getEndDate().plusDays(1).minusNanos(1);
            builder.and(inquiry.createdDate.loe(endOfDay));
        }

        // 비공개 문의 필터링 (관리자가 아닌 경우)
        if (!condition.isAdmin()) {
            builder.and(inquiry.isPrivate.eq(false).or(inquiry.user.id.eq(condition.getUserId())));
        }

        // 쿼리 실행: 결과 카운트 조회
        Long total = queryFactory
                .select(inquiry.count())
                .from(inquiry)
                .leftJoin(inquiry.user, user)
                .where(builder)
                .fetchOne();

        // 결과가 없는 경우 빈 페이지 반환
        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 쿼리 실행: 결과 데이터 조회
        List<InquirySearchResultDto> content = queryFactory
                .select(Projections.constructor(
                        InquirySearchResultDto.class,
                        inquiry.id,
                        inquiry.title,
                        inquiry.content,
                        inquiry.answer,
                        inquiry.status,
                        inquiry.category,
                        inquiry.isPrivate,
                        inquiry.createdDate,
                        inquiry.modifiedDate,
                        user.id.as("userId"),
                        user.email,
                        user.profile.profileName,
                        user.profile.Name
                ))
                .from(inquiry)
                .leftJoin(inquiry.user, user)
                .leftJoin(user.profile)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inquiry.createdDate.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public InquirySearchResultDto findInquiryWithUserDetails(Long inquiryId) {
        QInquiry inquiry = QInquiry.inquiry;
        QUser user = QUser.user;

        return queryFactory
                .select(Projections.constructor(
                        InquirySearchResultDto.class,
                        inquiry.id,
                        inquiry.title,
                        inquiry.content,
                        inquiry.answer,
                        inquiry.status,
                        inquiry.category,
                        inquiry.isPrivate,
                        inquiry.createdDate,
                        inquiry.modifiedDate,
                        user.id.as("userId"),
                        user.email,
                        user.profile.profileName,
                        user.profile.Name
                ))
                .from(inquiry)
                .leftJoin(inquiry.user, user)
                .leftJoin(user.profile)
                .where(inquiry.id.eq(inquiryId))
                .fetchOne();
    }

    @Override
    public Page<InquirySearchResultDto> findUnansweredInquiriesOrderByOldest(Pageable pageable) {
        QInquiry inquiry = QInquiry.inquiry;
        QUser user = QUser.user;

        // 답변되지 않은 문의 조건
        BooleanExpression condition = inquiry.status.ne(InquiryStatus.ANSWERED);

        // 총 개수 조회
        Long total = queryFactory
                .select(inquiry.count())
                .from(inquiry)
                .where(condition)
                .fetchOne();

        // 결과가 없는 경우 빈 페이지 반환
        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 데이터 조회
        List<InquirySearchResultDto> content = queryFactory
                .select(Projections.constructor(
                        InquirySearchResultDto.class,
                        inquiry.id,
                        inquiry.title,
                        inquiry.content,
                        inquiry.answer,
                        inquiry.status,
                        inquiry.category,
                        inquiry.isPrivate,
                        inquiry.createdDate,
                        inquiry.modifiedDate,
                        user.id.as("userId"),
                        user.email,
                        user.profile.profileName,
                        user.profile.Name
                ))
                .from(inquiry)
                .leftJoin(inquiry.user, user)
                .leftJoin(user.profile)
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inquiry.createdDate.asc()) // 오래된 순
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<InquirySearchResultDto> findByTitleOrContentContaining(String keyword, Pageable pageable) {
        QInquiry inquiry = QInquiry.inquiry;
        QUser user = QUser.user;

        // 검색 조건
        BooleanExpression condition = inquiry.title.containsIgnoreCase(keyword)
                .or(inquiry.content.containsIgnoreCase(keyword));

        // 총 개수 조회
        Long total = queryFactory
                .select(inquiry.count())
                .from(inquiry)
                .where(condition)
                .fetchOne();

        // 결과가 없는 경우 빈 페이지 반환
        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 데이터 조회
        List<InquirySearchResultDto> content = queryFactory
                .select(Projections.constructor(
                        InquirySearchResultDto.class,
                        inquiry.id,
                        inquiry.title,
                        inquiry.content,
                        inquiry.answer,
                        inquiry.status,
                        inquiry.category,
                        inquiry.isPrivate,
                        inquiry.createdDate,
                        inquiry.modifiedDate,
                        user.id.as("userId"),
                        user.email,
                        user.profile.profileName,
                        user.profile.Name
                ))
                .from(inquiry)
                .leftJoin(inquiry.user, user)
                .leftJoin(user.profile)
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inquiry.createdDate.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Object getInquiryStatistics() {
        QInquiry inquiry = QInquiry.inquiry;
        Map<String, Object> statistics = new HashMap<>();

        // 상태별 문의 수
        List<Tuple> statusStats = queryFactory
                .select(inquiry.status, inquiry.count())
                .from(inquiry)
                .groupBy(inquiry.status)
                .fetch();

        // 카테고리별 문의 수
        List<Tuple> categoryStats = queryFactory
                .select(inquiry.category, inquiry.count())
                .from(inquiry)
                .groupBy(inquiry.category)
                .fetch();

        // 최근 30일간 일별 문의 수
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Tuple> dailyStats = queryFactory
                .select(inquiry.createdDate.yearMonth(), inquiry.count())
                .from(inquiry)
                .where(inquiry.createdDate.after(thirtyDaysAgo))
                .groupBy(inquiry.createdDate.yearMonth())
                .fetch();

        // 각 통계 결과 맵에 추가
        Map<String, Long> statusCounts = statusStats.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, InquiryStatus.class).name(),
                        tuple -> tuple.get(1, Long.class)
                ));

        Map<String, Long> categoryCounts = categoryStats.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, InquiryCategory.class).name(),
                        tuple -> tuple.get(1, Long.class)
                ));

        Map<String, Long> dailyCounts = dailyStats.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, YearMonth.class).toString(),
                        tuple -> tuple.get(1, Long.class)
                ));

        statistics.put("statusCounts", statusCounts);
        statistics.put("categoryCounts", categoryCounts);
        statistics.put("dailyCounts", dailyCounts);

        return statistics;
    }
}