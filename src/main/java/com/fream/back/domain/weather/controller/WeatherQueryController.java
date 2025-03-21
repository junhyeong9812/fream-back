package com.fream.back.domain.weather.controller;

import com.fream.back.domain.weather.dto.WeatherDataDto;
import com.fream.back.domain.weather.exception.WeatherDataException;
import com.fream.back.domain.weather.exception.WeatherErrorCode;
import com.fream.back.domain.weather.service.query.WeatherQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 날씨 "조회"와 관련된 HTTP API 컨트롤러
 */
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
     * @throws WeatherDataException 데이터 조회 실패 시
     */
    @GetMapping("/current")
    public ResponseEntity<WeatherDataDto> getClosestWeatherData() {
        return weatherQueryService.getClosestWeatherData()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new WeatherDataException(
                        WeatherErrorCode.WEATHER_DATA_NOT_FOUND,
                        "현재 시각과 가까운 날씨 데이터가 없습니다."));
    }

    /**
     * "오늘" 전체 시간대의 날씨 데이터 반환
     *
     * @return 오늘의 날씨 데이터 목록
     * @throws WeatherDataException 데이터 조회 실패 시
     */
    @GetMapping("/today")
    public ResponseEntity<List<WeatherDataDto>> getTodayWeatherData() {
        List<WeatherDataDto> weatherDataList = weatherQueryService.getTodayWeatherData();

        if (weatherDataList.isEmpty()) {
            throw new WeatherDataException(
                    WeatherErrorCode.WEATHER_DATA_NOT_FOUND,
                    "오늘의 날씨 데이터가 없습니다.");
        }

        return ResponseEntity.ok(weatherDataList);
    }
}