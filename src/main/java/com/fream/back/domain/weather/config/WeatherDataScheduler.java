package com.fream.back.domain.weather.config;

import com.fream.back.domain.weather.service.command.WeatherDataCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기적으로 날씨 데이터를 업데이트하기 위한 스케줄러
 */
@Slf4j
@Component
@EnableScheduling  // 스케줄 기능 활성화
@RequiredArgsConstructor
public class WeatherDataScheduler {

    // Command 인터페이스
    private final WeatherDataCommandService weatherDataCommandService;

    // 스케줄링 간격 (application.yml에서 설정)
    @Value("${weather.scheduler.interval:86400000}")
    private long schedulerInterval;

    /**
     * 정해진 간격으로 날씨 데이터를 갱신
     * fixedRateString 값은 application.yml에서 설정
     * initialDelayString 값을 지정하여 서버 시작 후 바로 실행되지 않도록 함
     */
    @Scheduled(
            fixedRateString = "${weather.scheduler.interval:86400000}",
            initialDelayString = "${weather.scheduler.initial-delay:10000}"
    )
    public void scheduleWeatherDataFetch() {
        log.info("스케줄러에 의한 날씨 데이터 갱신 시작 (간격: {}ms)", schedulerInterval);
        try {
            weatherDataCommandService.fetchAndStore24HourWeatherData();
            log.info("스케줄러에 의한 날씨 데이터 갱신 완료");
        } catch (Exception e) {
            log.error("스케줄러에 의한 날씨 데이터 갱신 중 오류 발생", e);
            // 스케줄러에서 발생한 예외는 상위로 전파하지 않고 로깅만 수행
            // 다음 스케줄 실행에 영향을 주지 않도록 함
        }
    }
}