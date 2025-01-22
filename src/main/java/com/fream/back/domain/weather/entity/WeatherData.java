package com.fream.back.domain.weather.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp; // 데이터의 시간대

    private double temperature; // 온도
    private double precipitationProbability; // 강수 확률
    private double precipitation; // 강수량
    private double rain; // 비
    private double snowfall; // 눈

    @Column(name = "retrieved_at")
    private LocalDateTime retrievedAt; // 데이터가 저장된 시간
}
