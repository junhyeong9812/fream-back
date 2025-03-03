package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatQuestionRepository extends JpaRepository<ChatQuestion, Long> {

    // 사용자별 채팅 기록 조회
    @Query("SELECT c FROM ChatQuestion c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<ChatQuestion> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // IP별 채팅 기록 조회 (비회원)
    @Query("SELECT c FROM ChatQuestion c WHERE c.clientIp = :clientIp AND c.user IS NULL ORDER BY c.createdAt DESC")
    Page<ChatQuestion> findByClientIpOrderByCreatedAtDesc(@Param("clientIp") String clientIp, Pageable pageable);

    // 최근 질문 조회 (사용자)
    @Query("SELECT c FROM ChatQuestion c WHERE c.user.id = :userId ORDER BY c.createdAt DESC LIMIT 10")
    List<ChatQuestion> findRecentQuestionsByUserId(@Param("userId") Long userId);

    // 최근 질문 조회 (IP)
    @Query("SELECT c FROM ChatQuestion c WHERE c.clientIp = :clientIp AND c.user IS NULL ORDER BY c.createdAt DESC LIMIT 10")
    List<ChatQuestion> findRecentQuestionsByClientIp(@Param("clientIp") String clientIp);

    // 사용자별 채팅 기록 개수 조회
    long countByUserId(Long userId);
}