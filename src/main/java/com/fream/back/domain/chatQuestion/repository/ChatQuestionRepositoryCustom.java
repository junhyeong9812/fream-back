package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 채팅 질문에 대한 커스텀 쿼리를 위한 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리를 정의합니다.
 */
public interface ChatQuestionRepositoryCustom {

    /**
     * 사용자의 최근 질문을 조회합니다
     *
     * @param userId 사용자 ID
     * @param limit 최대 항목 수
     * @return 최근 채팅 질문 목록
     */
    List<ChatQuestion> findRecentQuestionsByUserId(Long userId, int limit);

    /**
     * IP 주소로 최근 질문을 조회합니다 (비회원용)
     *
     * @param clientIp 클라이언트 IP 주소
     * @param limit 최대 항목 수
     * @return 최근 채팅 질문 목록
     */
    List<ChatQuestion> findRecentQuestionsByClientIp(String clientIp, int limit);

    /**
     * 특정 키워드가 포함된 질문들을 검색합니다 (관리자용)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 키워드가 포함된 채팅 질문 페이지
     */
    Page<ChatQuestion> searchQuestionsByKeyword(String keyword, Pageable pageable);
}