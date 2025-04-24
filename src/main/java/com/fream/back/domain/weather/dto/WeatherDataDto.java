package com.fream.back.domain.weather.dto;

import com.fream.back.domain.weather.entity.WeatherData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * WeatherData 엔티티로부터 DTO를 생성하는 정적 팩토리 메서드
     *
     * @param entity 변환할 WeatherData 엔티티
     * @return 생성된 DTO
     */
    public static WeatherDataDto fromEntity(WeatherData entity) {
        return WeatherDataDto.builder()
                .timestamp(entity.getTimestamp())
                .temperature(entity.getTemperature())
                .precipitationProbability(entity.getPrecipitationProbability())
                .precipitation(entity.getPrecipitation())
                .rain(entity.getRain())
                .snowfall(entity.getSnowfall())
                .build();
    }

    /**
     * WeatherData 엔티티 리스트를 DTO 리스트로 변환하는 유틸리티 메서드
     *
     * @param entities 변환할 엔티티 리스트
     * @return 변환된 DTO 리스트
     */
    public static List<WeatherDataDto> fromEntities(List<WeatherData> entities) {
        return entities.stream()
                .map(WeatherDataDto::fromEntity)
                .collect(Collectors.toList());
    }
}