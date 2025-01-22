package com.fream.back.domain.weather.service.query;

import com.fream.back.domain.weather.dto.WeatherDataDto;                  // 조회 결과 DTO
import com.fream.back.domain.weather.entity.WeatherData;                 // 날씨 엔티티
import com.fream.back.domain.weather.repository.WeatherDataRepository;   // JPA Repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeatherQueryServiceImpl implements WeatherQueryService {

    // DB 접근용 레포지토리
    private final WeatherDataRepository weatherDataRepository;

    /**
     * 현재 시각과 ±1시간 범위 내에서,
     * "가장 가까운 시각"의 WeatherData를 찾는다.
     */
    @Override
    public Optional<WeatherDataDto> getClosestWeatherData() {
        // 현재 시각
        LocalDateTime now = LocalDateTime.now();
        // 검색 범위: [now-1h, now+1h]
        LocalDateTime startTime = now.minusHours(1);
        LocalDateTime endTime = now.plusHours(1);

        // 해당 범위의 날씨 데이터 목록 조회
        List<WeatherData> weatherDataList =
                weatherDataRepository.findWithinTimeRange(startTime, endTime);

        // 최솟값(가장 가까운 시차)을 찾음
        return weatherDataList.stream()
                .min(Comparator.comparingLong(w ->
                        Math.abs(Duration.between(w.getTimestamp(), now).toMillis()))
                )
                .map(this::convertToDto);  // 엔티티 -> DTO 변환
    }

    /**
     * 오늘(0시 ~ 23:59:59) 사이의 모든 날씨 데이터 조회 후,
     * 시간 오름차순 정렬하여 반환
     */
    @Override
    public List<WeatherDataDto> getTodayWeatherData() {
        // 오늘 0시 (startOfDay)
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        // 오늘 23:59:59 (endOfDay)
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        // 범위 내 데이터 검색 후, 시간 순 정렬
        return weatherDataRepository.findByTimestampBetween(startOfDay, endOfDay).stream()
                .sorted(Comparator.comparing(WeatherData::getTimestamp))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 엔티티(WeatherData) -> DTO(WeatherDataDto) 변환 메서드
     */
    private WeatherDataDto convertToDto(WeatherData weatherData) {
        return WeatherDataDto.builder()
                .timestamp(weatherData.getTimestamp())
                .temperature(weatherData.getTemperature())
                .precipitationProbability(weatherData.getPrecipitationProbability())
                .precipitation(weatherData.getPrecipitation())
                .rain(weatherData.getRain())
                .snowfall(weatherData.getSnowfall())
                .build();
    }
}
