package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 채팅 질문에 대한 기본 레포지토리
 */
public interface ChatQuestionRepository extends JpaRepository<ChatQuestion, Long>, ChatQuestionRepositoryCustom {

    /**
     * 사용자 ID로 채팅 기록을 조회합니다 (생성일 기준 내림차순)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 채팅 질문 페이지
     */
    Page<ChatQuestion> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자 ID로 채팅 기록 개수를 조회합니다
     *
     * @param userId 사용자 ID
     * @return 채팅 질문 개수
     */
    long countByUserId(Long userId);
}