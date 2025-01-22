package com.fream.back.domain.weather.service.query;

import com.fream.back.domain.weather.dto.WeatherDataDto;

import java.util.List;
import java.util.Optional;

/**
 * 날씨 데이터 "조회" 관련 기능을 정의하는 Service 인터페이스.
 */
public interface WeatherQueryService {

    /**
     * 현재 시각과 가장 가까운 날씨 정보 1건을 가져온다.
     * @return WeatherDataDto (Optional)
     */
    Optional<WeatherDataDto> getClosestWeatherData();

    /**
     * "오늘"에 해당하는 모든 시간대의 날씨 데이터를 반환.
     * @return WeatherDataDto 리스트
     */
    List<WeatherDataDto> getTodayWeatherData();
}
