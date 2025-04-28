package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

import static com.fream.back.domain.notice.entity.QNotice.notice;

/**
 * 공지사항 레포지토리 커스텀 구현
 */
@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Notice> searchNotices(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // 키워드 검색 조건 추가
        if (StringUtils.hasText(keyword)) {
            builder.and(containsKeyword(keyword));
        }

        // 결과 조회
        List<Notice> results = queryFactory.selectFrom(notice)
                .where(builder)
                .orderBy(notice.createdDate.desc()) // 최신순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 결과 수 조회
        Long total = queryFactory.select(notice.count())
                .from(notice)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(results, pageable, Objects.requireNonNullElse(total, 0L));
    }

    @Override
    public Page<Notice> searchNoticesByCategoryAndKeyword(NoticeCategory category, String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // 카테고리 검색 조건 추가
        if (category != null) {
            builder.and(notice.category.eq(category));
        }

        // 키워드 검색 조건 추가
        if (StringUtils.hasText(keyword)) {
            builder.and(containsKeyword(keyword));
        }

        // 결과 조회
        List<Notice> results = queryFactory.selectFrom(notice)
                .where(builder)
                .orderBy(notice.createdDate.desc()) // 최신순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 결과 수 조회
        Long total = queryFactory.select(notice.count())
                .from(notice)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(results, pageable, Objects.requireNonNullElse(total, 0L));
    }

    /**
     * 키워드 검색 조건 (제목 또는 내용에 포함)
     *
     * @param keyword 검색 키워드
     * @return 검색 조건
     */
    private BooleanExpression containsKeyword(String keyword) {
        return notice.title.containsIgnoreCase(keyword)
                .or(notice.content.containsIgnoreCase(keyword));
    }
}