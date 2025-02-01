package com.fream.back.domain.accessLog.service.query;

import com.fream.back.domain.accessLog.dto.DailyAccessCountDto;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 접근 로그 조회(검색, 목록, 단건 등)를 담당하는
 * Query 전용 서비스
 */
@Service
@RequiredArgsConstructor
public class UserAccessLogQueryService {

    private final UserAccessLogRepository userAccessLogRepository;

    /**
     * 오늘 접속자 수
     */
    public long getTodayAccessCount() {
        return userAccessLogRepository.countTodayAccesses();
    }

    /**
     * 최근 7일 일자별 접속자 수
     */
    public List<DailyAccessCountDto> getRecent7DaysAccessCount() {
        // 1/26 ~ 2/1 식으로 데이터를 가져옴
        return userAccessLogRepository.findRecent7DaysAccessCount();
    }

}
