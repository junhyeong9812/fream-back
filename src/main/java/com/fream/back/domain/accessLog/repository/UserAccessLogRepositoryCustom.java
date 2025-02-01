package com.fream.back.domain.accessLog.repository;

import com.fream.back.domain.accessLog.dto.DailyAccessCountDto;

import java.time.LocalDate;
import java.util.List;

public interface UserAccessLogRepositoryCustom {

    /**
     * 오늘 0시 ~ 현재까지 접속자 수
     */
    long countTodayAccesses();

    /**
     * 최근 7일(예: 오늘 포함 7일) 일자별 접속자 수
     * - 반환: DailyAccessCountDto(date, count)
     */
    List<DailyAccessCountDto> findRecent7DaysAccessCount();
}
