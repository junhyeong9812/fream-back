package com.fream.back.global.config;

import com.fream.back.domain.style.service.scheduler.StyleViewLogBufferManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationShutdownConfig {

    private final StyleViewLogBufferManager bufferManager;

    /**
     * 애플리케이션 종료 시 실행할 작업 등록
     */
    @Bean
    public ApplicationListener<ContextClosedEvent> shutdownHook() {
        return event -> {
            log.info("Application shutdown hook triggered");

            // 버퍼에 남은 로그 데이터 저장
            bufferManager.flushBufferOnShutdown();

            // 추가 종료 작업이 필요한 경우 여기에 추가

            log.info("Application shutdown tasks completed");
        };
    }
}