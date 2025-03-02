package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.entity.GPTUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GPTUsageLogRepository extends JpaRepository<GPTUsageLog, Long> {

    // 특정 기간 동안의 총 토큰 사용량 집계
    @Query("SELECT SUM(g.totalTokens) FROM GPTUsageLog g WHERE g.createdDate BETWEEN :startDate AND :endDate")
    Integer getTotalTokensUsedBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 특정 기간 동안의 모델별 토큰 사용량 집계
    @Query("SELECT g.modelName, SUM(g.totalTokens) FROM GPTUsageLog g " +
            "WHERE g.createdDate BETWEEN :startDate AND :endDate GROUP BY g.modelName")
    List<Object[]> getTokenUsageByModelBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 특정 사용자의 토큰 사용량 조회
    @Query("SELECT SUM(g.totalTokens) FROM GPTUsageLog g WHERE g.user.id = :userId")
    Integer getTotalTokensUsedByUser(@Param("userId") Long userId);

    // 일자별 토큰 사용량 집계
    @Query("SELECT FUNCTION('DATE', g.createdDate) as date, SUM(g.totalTokens) as tokens " +
            "FROM GPTUsageLog g " +
            "WHERE g.createdDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', g.createdDate) " +
            "ORDER BY date ASC")
    List<Object[]> getDailyTokenUsage(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 관리자용 사용량 로그 페이징 조회
    Page<GPTUsageLog> findAllByOrderByCreatedDateDesc(Pageable pageable);

    // 요청 유형별 토큰 사용량 집계
    @Query("SELECT g.requestType, SUM(g.totalTokens) FROM GPTUsageLog g " +
            "WHERE g.createdDate BETWEEN :startDate AND :endDate GROUP BY g.requestType")
    List<Object[]> getTokenUsageByRequestTypeBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}