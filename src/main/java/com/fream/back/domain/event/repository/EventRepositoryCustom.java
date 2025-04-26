package com.fream.back.domain.event.repository;

import com.fream.back.domain.event.dto.EventSearchCondition;
import com.fream.back.domain.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Event 리포지토리의 커스텀 메소드 정의 인터페이스
 * QueryDSL을 활용한 복잡한 쿼리와 동적 쿼리를 위한 인터페이스
 */
public interface EventRepositoryCustom {

    /**
     * 검색 조건에 따른 이벤트 페이징 조회
     * @param condition 검색 조건 (키워드, 브랜드ID, 활성상태, 시작일, 종료일, 상태)
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    Page<Event> searchEvents(EventSearchCondition condition, Pageable pageable);
}