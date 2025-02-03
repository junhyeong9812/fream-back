package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.Notice;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.fream.back.domain.notice.entity.QNotice.notice;

@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Notice> searchNotices(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isEmpty()) {
            // 대소문자 무시와 한글 검색을 위한 like 사용
            builder.or(notice.title.like("%" + keyword + "%"))
                    .or(notice.content.like("%" + keyword + "%"));
        }

        List<Notice> results = queryFactory.selectFrom(notice)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory.select(notice.count())
                .from(notice)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }
}
