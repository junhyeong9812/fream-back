package com.fream.back.domain.weather.api;

import com.fream.back.domain.weather.dto.WeatherApiResponse;

/**
 * 외부 날씨 API 호출을 위한 인터페이스
 * 구현체를 쉽게 대체할 수 있고 테스트 시 모킹이 용이함
 */
public interface WeatherApiClient {

    /**
     * 외부 날씨 API 호출
     *
     * @return 날씨 API 응답 데이터
     */
    WeatherApiResponse getWeatherData();
}