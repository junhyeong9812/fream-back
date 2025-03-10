package com.fream.back.domain.style.service.scheduler;

import com.fream.back.domain.style.service.kafka.StyleViewEventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StyleViewLogBufferManager {

    private final StyleViewEventConsumer styleViewEventConsumer;

    /**
     * 10초마다 버퍼에 남아있는 뷰 로그들을 DB에 저장
     * 트래픽이 적은 경우에도 로그가 적시에 저장되도록 보장
     */
    @Scheduled(fixedDelay = 10000) // 10초마다 실행
    public void flushBufferPeriodically() {
        log.debug("스타일 뷰 로그 버퍼 주기적 플러시 실행");
        styleViewEventConsumer.flushBuffer();
    }

    /**
     * 애플리케이션 종료 시 버퍼에 남아있는 로그 저장
     * (이 메서드는 애플리케이션 종료 이벤트에 연결해야 함)
     */
    public void flushBufferOnShutdown() {
        log.info("애플리케이션 종료 - 버퍼 플러시 실행");
        styleViewEventConsumer.flushBuffer();
    }
}