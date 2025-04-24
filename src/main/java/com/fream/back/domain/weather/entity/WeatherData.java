package com.fream.back.domain.weather.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = {
                @Index(name = "idx_weather_data_timestamp", columnList = "timestamp", unique = true)
        }
)
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDateTime timestamp; // 데이터의 시간대

    private double temperature; // 온도
    private double precipitationProbability; // 강수 확률
    private double precipitation; // 강수량
    private double rain; // 비
    private double snowfall; // 눈

    @Column(name = "retrieved_at")
    private LocalDateTime retrievedAt; // 데이터가 저장된 시간

    /**
     * 엔티티 생성용 Builder
     */
    @Builder
    private WeatherData(
            Long id,
            LocalDateTime timestamp,
            double temperature,
            double precipitationProbability,
            double precipitation,
            double rain,
            double snowfall,
            LocalDateTime retrievedAt
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.precipitationProbability = precipitationProbability;
        this.precipitation = precipitation;
        this.rain = rain;
        this.snowfall = snowfall;
        this.retrievedAt = retrievedAt;
    }

    /**
     * 업데이트용 비즈니스 메서드
     * - setter가 아닌, 필요한 필드만 업데이트
     */
    public void updateWeatherData(
            double temperature,
            double precipitationProbability,
            double precipitation,
            double rain,
            double snowfall,
            LocalDateTime retrievedAt
    ) {
        // timestamp는 고유 식별 개념이므로 변경 X (원한다면 로직 추가 가능)
        this.temperature = temperature;
        this.precipitationProbability = precipitationProbability;
        this.precipitation = precipitation;
        this.rain = rain;
        this.snowfall = snowfall;
        this.retrievedAt = retrievedAt;
    }
}