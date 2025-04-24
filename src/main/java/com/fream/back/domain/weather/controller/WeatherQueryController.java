package com.fream.back.domain.weather.controller;

import com.fream.back.domain.weather.dto.WeatherDataDto;
import com.fream.back.domain.weather.exception.WeatherException;
import com.fream.back.domain.weather.service.query.WeatherQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 날씨 "조회"와 관련된 HTTP API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/weather/query")
@RequiredArgsConstructor
public class WeatherQueryController {

    // 인터페이스 의존 -> 구현체(WeatherQueryServiceImpl)는 스프링이 주입
    private final WeatherQueryService weatherQueryService;

    /**
     * 현재 시점과 가장 가까운 날씨 데이터 반환
     *
     * @return 현재와 가장 가까운 시각의 날씨 데이터
     * @throws WeatherException 데이터 조회 실패 시
     */
    @GetMapping("/current")
    public ResponseEntity<?> getClosestWeatherData() {
        log.debug("현재 시각과 가장 가까운 날씨 데이터 API 요청 받음");

        return weatherQueryService.getClosestWeatherData()
                .map(ResponseEntity::ok)  // 데이터가 있으면 200 OK와 함께 반환
                .orElse(ResponseEntity.noContent().build());  // 데이터가 없으면 204 No Content
    }

    /**
     * "오늘" 전체 시간대의 날씨 데이터 반환
     *
     * @return 오늘의 날씨 데이터 목록
     * @throws WeatherException 데이터 조회 실패 시
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodayWeatherData() {
        log.debug("오늘의 날씨 데이터 API 요청 받음");

        List<WeatherDataDto> weatherDataList = weatherQueryService.getTodayWeatherData();

        if (weatherDataList.isEmpty()) {
            log.info("오늘의 날씨 데이터가 없음");
            return ResponseEntity.noContent().build();  // 204 No Content
        }

        return ResponseEntity.ok(weatherDataList);  // 200 OK
    }
}