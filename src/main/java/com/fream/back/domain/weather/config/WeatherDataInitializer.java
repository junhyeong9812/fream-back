package com.fream.back.domain.weather.config;

import com.fream.back.domain.weather.service.command.WeatherDataCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시, 24시간 날씨 데이터를 한 번 가져오기 위한 초기화 로직
 */
@Component
@RequiredArgsConstructor
public class WeatherDataInitializer {

    // Command 인터페이스
    private final WeatherDataCommandService weatherDataCommandService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeWeatherData() {
        // 서버가 올라오면 1회 실행
        weatherDataCommandService.fetchAndStore24HourWeatherData();
    }
}
