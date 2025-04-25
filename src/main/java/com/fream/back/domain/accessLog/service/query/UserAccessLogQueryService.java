package com.fream.back.domain.accessLog.service.query;

import com.fream.back.domain.accessLog.dto.DailyAccessCountDto;
import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.AccessLogQueryException;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 접근 로그 조회(검색, 목록, 단건 등)를 담당하는
 * Query 전용 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccessLogQueryService {

    private final UserAccessLogRepository userAccessLogRepository;

    /**
     * 오늘 접속자 수
     *
     * @return 오늘 접속자 수
     * @throws AccessLogQueryException 조회 실패 시
     */
    public long getTodayAccessCount() {
        try {
            return userAccessLogRepository.countTodayAccesses();
        } catch (DataAccessException e) {
            log.error("오늘 접속자 수 조회 중 오류 발생", e);
            throw new AccessLogQueryException(AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR,
                    "오늘 접속자 수를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 오늘 고유 방문자 수 (IP 기준)
     *
     * @return 오늘 고유 방문자 수
     * @throws AccessLogQueryException 조회 실패 시
     */
    public long getTodayUniqueVisitorCount() {
        try {
            return userAccessLogRepository.countTodayUniqueVisitors();
        } catch (DataAccessException e) {
            log.error("오늘 고유 방문자 수 조회 중 오류 발생", e);
            throw new AccessLogQueryException(AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR,
                    "오늘 고유 방문자 수를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 최근 7일 일자별 접속자 수
     *
     * @return 최근 7일 일자별 접속자 수 목록
     * @throws AccessLogQueryException 조회 실패 시
     */
    public List<DailyAccessCountDto> getRecent7DaysAccessCount() {
        try {
            List<DailyAccessCountDto> result = userAccessLogRepository.findRecent7DaysAccessCount();
            return result != null ? result : Collections.emptyList();
        } catch (DataAccessException e) {
            log.error("최근 7일 접속자 수 조회 중 오류 발생", e);
            throw new AccessLogQueryException(AccessLogErrorCode.STATISTICS_QUERY_ERROR,
                    "최근 7일 접속 통계를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}