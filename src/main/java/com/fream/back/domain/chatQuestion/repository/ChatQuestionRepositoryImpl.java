package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.domain.chatQuestion.entity.QChatQuestion;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * ChatQuestionRepositoryCustom 인터페이스의 QueryDSL 구현체
 * 복잡한 쿼리를 QueryDSL을 사용하여 구현합니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatQuestionRepositoryImpl implements ChatQuestionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QChatQuestion chatQuestion = QChatQuestion.chatQuestion;

    @Override
    public List<ChatQuestion> findRecentQuestionsByUserId(Long userId, int limit) {
        log.debug("사용자의 최근 질문 조회 쿼리 실행: userId={}, limit={}", userId, limit);

        return queryFactory
                .selectFrom(chatQuestion)
                .where(chatQuestion.user.id.eq(userId))
                .orderBy(chatQuestion.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<ChatQuestion> findRecentQuestionsByClientIp(String clientIp, int limit) {
        log.debug("IP 주소별 최근 질문 조회 쿼리 실행: clientIp={}, limit={}", clientIp, limit);

        return queryFactory
                .selectFrom(chatQuestion)
                .where(
                        chatQuestion.clientIp.eq(clientIp)
                                .and(chatQuestion.user.isNull())
                )
                .orderBy(chatQuestion.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<ChatQuestion> searchQuestionsByKeyword(String keyword, Pageable pageable) {
        log.debug("키워드로 질문 검색 쿼리 실행: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        BooleanBuilder builder = new BooleanBuilder();

        // 키워드가 있는 경우에만 검색 조건 추가
        if (StringUtils.hasText(keyword)) {
            builder.and(
                    chatQuestion.question.containsIgnoreCase(keyword)
                            .or(chatQuestion.answer.containsIgnoreCase(keyword))
            );
        }

        // 전체 개수 쿼리
        long total = queryFactory
                .selectFrom(chatQuestion)
                .where(builder)
                .fetchCount();

        // 데이터 쿼리
        List<ChatQuestion> results = queryFactory
                .selectFrom(chatQuestion)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(chatQuestion.createdAt.desc())
                .fetch();

        return new PageImpl<>(results, pageable, total);
    }
}