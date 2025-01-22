package com.fream.back.domain.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeatherDataDto {
    private LocalDateTime timestamp; // 데이터의 시간대
    private double temperature; // 온도
    private double precipitationProbability; // 강수 확률
    private double precipitation; // 강수량
    private double rain; // 비
    private double snowfall; // 눈
}
