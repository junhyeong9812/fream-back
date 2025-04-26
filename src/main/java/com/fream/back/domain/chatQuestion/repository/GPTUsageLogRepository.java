package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.entity.GPTUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * GPT 사용량 로그에 대한 기본 레포지토리
 */
public interface GPTUsageLogRepository extends JpaRepository<GPTUsageLog, Long>, GPTUsageLogRepositoryCustom {

    /**
     * 모든 GPT 사용량 로그를 생성일 기준 내림차순으로 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return GPT 사용량 로그 페이지
     */
    Page<GPTUsageLog> findAllByOrderByCreatedDateDesc(Pageable pageable);
}