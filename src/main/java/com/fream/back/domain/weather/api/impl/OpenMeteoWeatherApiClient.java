package com.fream.back.domain.weather.api.impl;

import com.fream.back.domain.weather.api.WeatherApiClient;
import com.fream.back.domain.weather.dto.WeatherApiResponse;
import com.fream.back.domain.weather.exception.WeatherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Open-Meteo API를 호출하는 클라이언트 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenMeteoWeatherApiClient implements WeatherApiClient {

    private final RestTemplate restTemplate;

    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${weather.api.params.latitude}")
    private double latitude;

    @Value("${weather.api.params.longitude}")
    private double longitude;

    @Value("${weather.api.params.hourly}")
    private String hourly;

    @Value("${weather.api.params.timezone}")
    private String timezone;

    /**
     * Open-Meteo API 호출
     *
     * @return 날씨 API 응답 데이터
     * @throws WeatherException API 호출 실패 시
     */
    @Override
    public WeatherApiResponse getWeatherData() {
        try {
            // UriComponentsBuilder를 사용하여 URL 구성
            String fullUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("hourly", hourly)
                    .queryParam("timezone", timezone)
                    .toUriString();

            log.debug("OpenMeteo API 호출: {}", fullUrl);
            return restTemplate.getForObject(fullUrl, WeatherApiResponse.class);
        } catch (RestClientException e) {
            log.error("OpenMeteo API 호출 중 오류 발생: {}", e.getMessage());
            throw WeatherException.apiError("OpenMeteo API 호출 중 오류가 발생했습니다.", e);
        }
    }
}