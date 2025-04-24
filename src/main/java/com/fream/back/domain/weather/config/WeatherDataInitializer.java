package com.fream.back.domain.weather.config;

import com.fream.back.domain.weather.service.command.WeatherDataCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시, 24시간 날씨 데이터를 한 번 가져오기 위한 초기화 로직
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDataInitializer {

    // Command 인터페이스
    private final WeatherDataCommandService weatherDataCommandService;

    /**
     * 서버가 완전히 시작된 후 날씨 데이터 초기화
     * ApplicationReadyEvent는 모든 빈이 초기화되고 준비된 후 발생
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeWeatherData() {
        log.info("서버 시작 후 날씨 데이터 초기화 시작");
        try {
            // 서버가 올라오면 1회 실행
            weatherDataCommandService.fetchAndStore24HourWeatherData();
            log.info("서버 시작 후 날씨 데이터 초기화 완료");
        } catch (Exception e) {
            // 시작 시 실패해도 서버 실행은 계속되어야 함
            log.error("서버 시작 후 날씨 데이터 초기화 중 오류 발생", e);
        }
    }
}