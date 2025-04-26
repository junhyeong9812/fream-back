package com.fream.back.domain.event.scheduler;

import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 상태 자동 업데이트 스케줄러
 * - UPCOMING -> ACTIVE, ACTIVE -> ENDED 상태 변환을 주기적으로 체크
 */
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling  // 스케줄링 기능 활성화
public class EventStatusScheduler {

    private final EventRepository eventRepository;

    /**
     * 1시간마다 실행되는 이벤트 상태 체크 스케줄러
     * - UPCOMING 상태의 이벤트 중 시작 시간이 지난 이벤트를 체크
     * - ACTIVE 상태의 이벤트 중 종료 시간이 지난 이벤트를 체크
     *
     * cron = "0 0 * * * *": 매시 정각마다 실행 (초 분 시 일 월 요일)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkEventStatus() {
        log.info("이벤트 상태 체크 스케줄러 실행 시작: {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        // 1. 예정된 이벤트(UPCOMING) 중 시작 시간이 지난 이벤트를 ACTIVE로 업데이트
        checkUpcomingToActiveEvents(now);

        // 2. 활성 이벤트(ACTIVE) 중 종료 시간이 지난 이벤트를 ENDED 상태로 처리
        checkActiveToEndedEvents(now);

        log.info("이벤트 상태 체크 스케줄러 실행 완료: {}", LocalDateTime.now());
    }

    /**
     * UPCOMING -> ACTIVE 상태 변경 체크
     */
    private void checkUpcomingToActiveEvents(LocalDateTime now) {
        try {
            // 예정된 이벤트 중 시작 시간이 현재 이전인 이벤트 조회
            // (시작일 <= 현재) AND (종료일 > 현재)의 조건으로 시작 시간이 지났으나 종료되지 않은 이벤트 조회
            List<Event> startedEvents = eventRepository.findByStartDateBeforeAndEndDateAfter(now, now);

            int count = 0;
            for (Event event : startedEvents) {
                if (event.isUpcoming()) {  // 현재 UPCOMING 상태인 이벤트만 처리
                    // 이벤트 엔티티에 상태 변경 처리 로직이 있다면 호출
                    // (현재는 isActive(), isUpcoming() 등의 메서드만 있으므로 로그만 남김)
                    log.info("이벤트 상태 변경: UPCOMING -> ACTIVE, eventId={}, title={}",
                            event.getId(), event.getTitle());
                    count++;
                }
            }

            if (count > 0) {
                log.info("UPCOMING -> ACTIVE 상태로 변경된 이벤트 수: {}", count);
            } else {
                log.debug("UPCOMING -> ACTIVE 상태로 변경된 이벤트 없음");
            }
        } catch (Exception e) {
            log.error("UPCOMING -> ACTIVE 상태 변경 처리 중 오류 발생", e);
        }
    }

    /**
     * ACTIVE -> ENDED 상태 변경 체크
     */
    private void checkActiveToEndedEvents(LocalDateTime now) {
        try {
            // 종료 시간이 지난 이벤트 조회 (현재 >= 종료일)
            List<Event> endedEvents = eventRepository.findByEndDateLessThanEqual(now);

            int count = 0;
            for (Event event : endedEvents) {
                if (event.isActive()) {  // 현재 ACTIVE 상태인 이벤트만 처리
                    // 이벤트 엔티티에 상태 변경 처리 로직이 있다면 호출
                    // (현재는 isEnded() 등의 메서드만 있으므로 로그만 남김)
                    log.info("이벤트 상태 변경: ACTIVE -> ENDED, eventId={}, title={}",
                            event.getId(), event.getTitle());
                    count++;
                }
            }

            if (count > 0) {
                log.info("ACTIVE -> ENDED 상태로 변경된 이벤트 수: {}", count);
            } else {
                log.debug("ACTIVE -> ENDED 상태로 변경된 이벤트 없음");
            }
        } catch (Exception e) {
            log.error("ACTIVE -> ENDED 상태 변경 처리 중 오류 발생", e);
        }
    }
}