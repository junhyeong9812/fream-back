package com.fream.back.domain.chatQuestion.repository;

import com.fream.back.domain.chatQuestion.dto.log.GPTDailyUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTModelUsageDto;
import com.fream.back.domain.chatQuestion.dto.log.GPTRequestTypeUsageDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GPT 사용량 로그에 대한 커스텀 쿼리를 위한 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리를 정의합니다.
 */
public interface GPTUsageLogRepositoryCustom {

    /**
     * 특정 기간 동안의 총 토큰 사용량 집계
     *
     * @param startDate 시작 날짜/시간
     * @param endDate 종료 날짜/시간
     * @return 총 토큰 사용량, 데이터가 없는 경우 null
     */
    Integer getTotalTokensUsedBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 기간 동안의 모델별 토큰 사용량 집계
     *
     * @param startDate 시작 날짜/시간
     * @param endDate 종료 날짜/시간
     * @return 모델별 토큰 사용량 DTO 리스트
     */
    List<GPTModelUsageDto> getTokenUsageByModelBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 일자별 토큰 사용량 집계
     *
     * @param startDate 시작 날짜/시간
     * @param endDate 종료 날짜/시간
     * @return 일자별 토큰 사용량 DTO 리스트
     */
    List<GPTDailyUsageDto> getDailyTokenUsage(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 요청 유형별 토큰 사용량 집계
     *
     * @param startDate 시작 날짜/시간
     * @param endDate 종료 날짜/시간
     * @return 요청 유형별 토큰 사용량 DTO 리스트
     */
    List<GPTRequestTypeUsageDto> getTokenUsageByRequestTypeBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 사용자의 누적 토큰 사용량 조회
     *
     * @param userId 사용자 ID
     * @return 사용자의 총 토큰 사용량, 데이터가 없는 경우 null
     */
    Integer getTotalTokensUsedByUser(Long userId);
}