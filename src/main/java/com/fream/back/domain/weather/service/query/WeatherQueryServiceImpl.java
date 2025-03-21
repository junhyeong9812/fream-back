package com.fream.back.domain.weather.service.query;

import com.fream.back.domain.weather.dto.WeatherDataDto;
import com.fream.back.domain.weather.entity.WeatherData;
import com.fream.back.domain.weather.exception.WeatherDataException;
import com.fream.back.domain.weather.exception.WeatherErrorCode;
import com.fream.back.domain.weather.repository.WeatherDataRepository;
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
     *
     * @return 가장 가까운 시간의 날씨 데이터 (Optional)
     * @throws WeatherDataException 데이터 조회 실패 시
     */
    @Override
    public Optional<WeatherDataDto> getClosestWeatherData() {
        try {
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
        } catch (Exception e) {
            throw new WeatherDataException(WeatherErrorCode.WEATHER_DATA_QUERY_ERROR,
                    "현재 시각과 가장 가까운 날씨 데이터 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 오늘(0시 ~ 23:59:59) 사이의 모든 날씨 데이터 조회 후,
     * 시간 오름차순 정렬하여 반환
     *
     * @return 오늘의 날씨 데이터 목록
     * @throws WeatherDataException 데이터 조회 실패 시
     */
    @Override
    public List<WeatherDataDto> getTodayWeatherData() {
        try {
            // 오늘 0시 (startOfDay)
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            // 오늘 23:59:59 (endOfDay)
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            // 범위 내 데이터 검색 후, 시간 순 정렬
            List<WeatherData> weatherDataList = weatherDataRepository.findByTimestampBetween(startOfDay, endOfDay);

            if (weatherDataList.isEmpty()) {
                // 데이터가 없는 경우 빈 목록 반환 (또는 필요시 예외 발생)
                return Collections.emptyList();
                // 또는: throw new WeatherDataException(WeatherErrorCode.WEATHER_DATA_NOT_FOUND, "오늘의 날씨 데이터가 없습니다.");
            }

            return weatherDataList.stream()
                    .sorted(Comparator.comparing(WeatherData::getTimestamp))
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new WeatherDataException(WeatherErrorCode.WEATHER_DATA_QUERY_ERROR,
                    "오늘의 날씨 데이터 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 엔티티(WeatherData) -> DTO(WeatherDataDto) 변환 메서드
     *
     * @param weatherData 변환할 엔티티
     * @return 변환된 DTO
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