package com.fream.back.domain.weather.controller;

import com.fream.back.domain.weather.dto.WeatherDataDto;
import com.fream.back.domain.weather.service.query.WeatherQueryService; // 인터페이스
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 날씨 "조회"와 관련된 HTTP API 컨트롤러
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherQueryController {

    // 인터페이스 의존 -> 구현체(WeatherQueryServiceImpl)는 스프링이 주입
    private final WeatherQueryService weatherQueryService;

    /**
     * 현재 시점과 가장 가까운 날씨 데이터 반환
     */
    @GetMapping("/current")
    public ResponseEntity<WeatherDataDto> getClosestWeatherData() {
        return weatherQueryService.getClosestWeatherData()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * "오늘" 전체 시간대의 날씨 데이터 반환
     */
    @GetMapping("/today")
    public ResponseEntity<List<WeatherDataDto>> getTodayWeatherData() {
        List<WeatherDataDto> weatherDataList = weatherQueryService.getTodayWeatherData();
        return ResponseEntity.ok(weatherDataList);
    }
}
