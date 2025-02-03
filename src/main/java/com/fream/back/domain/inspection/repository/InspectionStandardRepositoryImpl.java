package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.entity.QInspectionStandard;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RequiredArgsConstructor
public class InspectionStandardRepositoryImpl implements InspectionStandardRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QInspectionStandard inspectionStandard=QInspectionStandard.inspectionStandard;
    @Override
    public Page<InspectionStandard> searchStandards(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isEmpty()) {
            builder.or(inspectionStandard.content.containsIgnoreCase(keyword))
                    .or(inspectionStandard.category.stringValue().containsIgnoreCase(keyword));
        }

        List<InspectionStandard> results = queryFactory.selectFrom(inspectionStandard)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory.select(inspectionStandard.count())
                .from(inspectionStandard)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }
}
