package com.fream.back.domain.faq.repository;

import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.QFAQ;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RequiredArgsConstructor
public class FAQRepositoryImpl implements FAQRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QFAQ fAQ = QFAQ.fAQ;
    @Override
    public Page<FAQ> searchFAQs(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isEmpty()) {
            // 대소문자 무시와 한글 검색을 위한 like 사용
            builder.or(fAQ.question.like("%" + keyword + "%"))
                    .or(fAQ.answer.like("%" + keyword + "%"));
        }

        List<FAQ> results = queryFactory.selectFrom(fAQ)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory.select(fAQ.count())
                .from(fAQ)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }
}
