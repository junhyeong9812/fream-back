package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.entity.QInspectionStandard;
import com.fream.back.domain.inspection.entity.QInspectionStandardImage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 검수 기준 리포지토리 QueryDSL 구현체
 * - N+1 문제 해결을 위한 조인 쿼리 적용
 * - 다양한 검색 조건 지원
 */
@RequiredArgsConstructor
public class InspectionStandardRepositoryImpl implements InspectionStandardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QInspectionStandard inspectionStandard = QInspectionStandard.inspectionStandard;
    private static final QInspectionStandardImage inspectionStandardImage = QInspectionStandardImage.inspectionStandardImage;

    /**
     * 키워드로 검수 기준 검색
     * - 내용 및 카테고리 이름에서 검색
     */
    @Override
    public Page<InspectionStandard> searchStandards(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.or(inspectionStandard.content.containsIgnoreCase(keyword))
                    .or(inspectionStandard.category.stringValue().containsIgnoreCase(keyword));
        }

        JPQLQuery<InspectionStandard> query = queryFactory
                .selectDistinct(inspectionStandard)
                .from(inspectionStandard)
                .leftJoin(inspectionStandardImage)
                .on(inspectionStandardImage.inspectionStandard.id.eq(inspectionStandard.id))
                .where(builder);

        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (order.getProperty().equals("id")) {
                    if (order.isAscending()) {
                        query.orderBy(inspectionStandard.id.asc());
                    } else {
                        query.orderBy(inspectionStandard.id.desc());
                    }
                } else if (order.getProperty().equals("createdDate")) {
                    if (order.isAscending()) {
                        query.orderBy(inspectionStandard.createdDate.asc());
                    } else {
                        query.orderBy(inspectionStandard.createdDate.desc());
                    }
                } else if (order.getProperty().equals("modifiedDate")) {
                    if (order.isAscending()) {
                        query.orderBy(inspectionStandard.modifiedDate.asc());
                    } else {
                        query.orderBy(inspectionStandard.modifiedDate.desc());
                    }
                }
            });
        } else {
            // 기본 정렬: ID 기준 내림차순
            query.orderBy(inspectionStandard.id.desc());
        }

        // 페이징 적용
        long total = query.fetchCount();
        List<InspectionStandard> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 카테고리 및 키워드로 검수 기준 검색
     */
    @Override
    public Page<InspectionStandard> searchStandardsByCategoryAndKeyword(
            InspectionCategory category, String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // 카테고리 조건 추가
        if (category != null) {
            builder.and(inspectionStandard.category.eq(category));
        }

        // 키워드 조건 추가
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(inspectionStandard.content.containsIgnoreCase(keyword));
        }

        JPQLQuery<InspectionStandard> query = queryFactory
                .selectDistinct(inspectionStandard)
                .from(inspectionStandard)
                .leftJoin(inspectionStandardImage)
                .on(inspectionStandardImage.inspectionStandard.id.eq(inspectionStandard.id))
                .where(builder);

        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (order.getProperty().equals("id")) {
                    if (order.isAscending()) {
                        query.orderBy(inspectionStandard.id.asc());
                    } else {
                        query.orderBy(inspectionStandard.id.desc());
                    }
                } else if (order.getProperty().equals("createdDate")) {
                    if (order.isAscending()) {
                        query.orderBy(inspectionStandard.createdDate.asc());
                    } else {
                        query.orderBy(inspectionStandard.createdDate.desc());
                    }
                } else if (order.getProperty().equals("modifiedDate")) {
                    if (order.isAscending()) {
                        query.orderBy(inspectionStandard.modifiedDate.asc());
                    } else {
                        query.orderBy(inspectionStandard.modifiedDate.desc());
                    }
                }
            });
        } else {
            // 기본 정렬: ID 기준 내림차순
            query.orderBy(inspectionStandard.id.desc());
        }

        // 페이징 적용
        long total = query.fetchCount();
        List<InspectionStandard> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }
}