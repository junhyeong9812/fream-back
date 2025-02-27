package com.fream.back.domain.user.scheduler;

import com.fream.back.domain.user.service.point.PointCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpirationScheduler {

    private final PointCommandService pointCommandService;

    /**
     * 1시간마다 만료된 포인트 확인 및 처리
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 0분 0초에 실행
    public void checkExpiredPoints() {
        log.info("포인트 만료 체크 스케줄러 실행");
        int expiredCount = pointCommandService.expirePoints();
        log.info("포인트 만료 처리 완료: {} 개의 포인트가 만료됨", expiredCount);
    }
}