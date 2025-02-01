package com.fream.back.domain.accessLog.controller.query;

import com.fream.back.domain.accessLog.dto.DailyAccessCountDto;
import com.fream.back.domain.accessLog.service.query.UserAccessLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 접근 로그 조회를 위한 Query 전용 컨트롤러
 */
@RestController
@RequestMapping("/access-log/queries")
@RequiredArgsConstructor
public class UserAccessLogQueryController {

    private final UserAccessLogQueryService userAccessLogQueryService;

    /**
     * 오늘 접속자 수
     * - 예) GET /api/access-log/queries/today
     *   → 123
     */
    @GetMapping("/today")
    public long getTodayAccessCount() {
        return userAccessLogQueryService.getTodayAccessCount();
    }

    /**
     * 최근 7일 날짜별 접속자 수
     * - 예) GET /api/access-log/queries/week
     *   → [ { dateString: "2025-01-26", count: 10 },
     *        { dateString: "2025-01-27", count: 0 },
     *        ...
     *        { dateString: "2025-02-01", count: 20 } ]
     */
    @GetMapping("/week")
    public List<DailyAccessCountDto> getWeekAccessCount() {
        return userAccessLogQueryService.getRecent7DaysAccessCount();
    }
}
