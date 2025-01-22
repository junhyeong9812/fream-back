package com.fream.back.domain.weather.config;

import com.fream.back.domain.weather.service.command.WeatherDataCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기적으로 날씨 데이터를 업데이트하기 위한 스케줄러
 */
@Component
@RequiredArgsConstructor
public class WeatherDataScheduler {

    // Command 인터페이스
    private final WeatherDataCommandService weatherDataCommandService;

    /**
     * 24시간마다 한 번씩 날씨 데이터를 갱신
     */
    @Scheduled(fixedRate = 86400000) // 밀리초 단위 (24시간)
    public void scheduleWeatherDataFetch() {
        weatherDataCommandService.fetchAndStore24HourWeatherData();
    }
}
