package com.fream.back.domain.weather.dto;

import lombok.Data;

import java.util.List;

@Data
public class WeatherApiResponse {
    private double latitude; // 위도
    private double longitude; // 경도
    private Hourly hourly; // 시간별 데이터

    @Data
    public static class Hourly {
        private List<String> time; // 시간 (ISO 8601 형식)
        private List<Double> temperature_2m; // 온도 (°C)
        private List<Double> precipitation_probability; // 강수 확률 (%)
        private List<Double> precipitation; // 강수량 (mm)
        private List<Double> rain; // 비 (mm)
        private List<Double> snowfall; // 눈 (cm)
    }
}
