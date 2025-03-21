package com.fream.back.domain.weather.service.command;

import com.fream.back.domain.weather.dto.WeatherApiResponse;
import com.fream.back.domain.weather.entity.WeatherData;
import com.fream.back.domain.weather.exception.WeatherApiException;
import com.fream.back.domain.weather.exception.WeatherDataException;
import com.fream.back.domain.weather.exception.WeatherErrorCode;
import com.fream.back.domain.weather.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WeatherDataCommandServiceImpl implements WeatherDataCommandService {

    // Repository: DB 저장/조회 담당
    private final WeatherDataRepository weatherDataRepository;

    // RestTemplate: 외부 날씨 API 호출
    private final RestTemplate restTemplate;

    // 외부 날씨 API 주소 (Query 파라미터 포함)
    private static final String WEATHER_API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=36.5&longitude=127.75&hourly=" +
                    "temperature_2m,precipitation_probability,precipitation,rain,snowfall&timezone=auto";

    /**
     * 외부 API로부터 24시간 날씨 정보를 받아와,
     * DB에 저장하는 메서드
     */
    @Override
    public void fetchAndStore24HourWeatherData() {
        try {
            // 1) API 호출
            WeatherApiResponse response = callWeatherApi();
            if (response == null || response.getHourly() == null) {
                throw new WeatherApiException(WeatherErrorCode.WEATHER_API_ERROR, "날씨 API 응답이 null입니다.");
            }

            // 예: 48개 처리
            int hoursCount = 48;
            if (response.getHourly().getTime().size() < hoursCount) {
                hoursCount = response.getHourly().getTime().size();
            }

            // 2) timestamp 리스트 구성 및 정렬
            List<LocalDateTime> timestamps = parseTimestamps(response, hoursCount);

            // 정렬
            timestamps.sort(LocalDateTime::compareTo);
            LocalDateTime startTime = timestamps.get(0);
            LocalDateTime endTime = timestamps.get(timestamps.size() - 1);

            // 3) DB에서 [startTime, endTime] 범위 조회
            List<WeatherData> existingList =
                    weatherDataRepository.findByTimestampBetween(startTime, endTime);

            // 4) 기존 레코드를 Map으로 변환: timestamp -> WeatherData
            Map<LocalDateTime, WeatherData> existingMap = new HashMap<>();
            for (WeatherData wd : existingList) {
                existingMap.put(wd.getTimestamp(), wd);
            }

            // 5) upsert 로직
            saveOrUpdateWeatherData(response, timestamps, existingMap, hoursCount);

        } catch (RestClientException e) {
            // API 호출 실패 처리
            throw new WeatherApiException(WeatherErrorCode.WEATHER_API_ERROR, e);
        } catch (DateTimeParseException e) {
            // 날짜/시간 파싱 오류 처리
            throw new WeatherApiException(WeatherErrorCode.WEATHER_API_PARSING_ERROR,
                    "날짜/시간 파싱 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            // 기타 모든 예외 처리
            if (e instanceof WeatherApiException) {
                throw e;
            } else {
                throw new WeatherDataException(WeatherErrorCode.WEATHER_DATA_SAVE_ERROR, e);
            }
        }
    }

    /**
     * 외부 날씨 API 호출 메서드
     *
     * @return API 응답 객체
     * @throws WeatherApiException API 호출 실패 시
     */
    private WeatherApiResponse callWeatherApi() {
        try {
            return restTemplate.getForObject(WEATHER_API_URL, WeatherApiResponse.class);
        } catch (RestClientException e) {
            throw new WeatherApiException(WeatherErrorCode.WEATHER_API_ERROR,
                    "날씨 API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * API 응답에서 타임스탬프 목록 파싱
     *
     * @param response API 응답 객체
     * @param hoursCount 처리할 시간 수
     * @return 타임스탬프 목록
     * @throws WeatherApiException 파싱 실패 시
     */
    private List<LocalDateTime> parseTimestamps(WeatherApiResponse response, int hoursCount) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        try {
            for (int i = 0; i < hoursCount; i++) {
                LocalDateTime ts = LocalDateTime.parse(
                        response.getHourly().getTime().get(i),
                        DateTimeFormatter.ISO_DATE_TIME
                );
                timestamps.add(ts);
            }
            return timestamps;
        } catch (DateTimeParseException e) {
            throw new WeatherApiException(WeatherErrorCode.WEATHER_API_PARSING_ERROR,
                    "날짜/시간 파싱 중 오류가 발생했습니다.", e);
        } catch (IndexOutOfBoundsException e) {
            throw new WeatherApiException(WeatherErrorCode.WEATHER_API_PARSING_ERROR,
                    "API 응답 데이터 형식이 올바르지 않습니다.", e);
        }
    }

    /**
     * 날씨 데이터 저장 또는 업데이트
     *
     * @param response API 응답 객체
     * @param timestamps 타임스탬프 목록
     * @param existingMap 기존 데이터 맵
     * @param hoursCount 처리할 시간 수
     * @throws WeatherDataException 데이터 저장 실패 시
     */
    private void saveOrUpdateWeatherData(WeatherApiResponse response, List<LocalDateTime> timestamps,
                                         Map<LocalDateTime, WeatherData> existingMap, int hoursCount) {
        try {
            for (int i = 0; i < hoursCount; i++) {
                LocalDateTime timestamp = timestamps.get(i);

                double temperature = response.getHourly().getTemperature_2m().get(i);
                double precipitationProbability = response.getHourly().getPrecipitation_probability().get(i);
                double precipitation = response.getHourly().getPrecipitation().get(i);
                double rain = response.getHourly().getRain().get(i);
                double snowfall = response.getHourly().getSnowfall().get(i);

                WeatherData existing = existingMap.get(timestamp);
                if (existing != null) {
                    // 존재 -> update
                    existing.updateWeatherData(
                            temperature,
                            precipitationProbability,
                            precipitation,
                            rain,
                            snowfall,
                            LocalDateTime.now()
                    );
                    weatherDataRepository.save(existing);
                } else {
                    // 없으면 insert
                    WeatherData newData = WeatherData.builder()
                            .timestamp(timestamp)
                            .temperature(temperature)
                            .precipitationProbability(precipitationProbability)
                            .precipitation(precipitation)
                            .rain(rain)
                            .snowfall(snowfall)
                            .retrievedAt(LocalDateTime.now())
                            .build();
                    weatherDataRepository.save(newData);
                }
            }
        } catch (Exception e) {
            throw new WeatherDataException(WeatherErrorCode.WEATHER_DATA_SAVE_ERROR,
                    "날씨 데이터 저장 중 오류가 발생했습니다.", e);
        }
    }
//    @Override
//    public void fetchAndStore24HourWeatherData() {
//        // 1) 외부 API 호출 -> WeatherApiResponse로 매핑
//        WeatherApiResponse response = restTemplate.getForObject(WEATHER_API_URL, WeatherApiResponse.class);
//
//        // 2) 응답이 있고, hourly 데이터가 있을 경우에만 처리
//        if (response != null && response.getHourly() != null) {
//            // 24시간 데이터를 순회하며 저장
//            for (int i = 0; i <= 48; i++) {
//                // 시간 문자열 -> LocalDateTime 파싱
//                LocalDateTime timestamp = LocalDateTime.parse(
//                        response.getHourly().getTime().get(i),
//                        DateTimeFormatter.ISO_DATE_TIME
//                );
//                // 2) DB에서 timestamp로 레코드 조회
//                Optional<WeatherData> optionalData = weatherDataRepository.findByTimestamp(timestamp);
//
//                double temperature = response.getHourly().getTemperature_2m().get(i);
//                double precipitationProbability = response.getHourly().getPrecipitation_probability().get(i);
//                double precipitation = response.getHourly().getPrecipitation().get(i);
//                double rain = response.getHourly().getRain().get(i);
//                double snowfall = response.getHourly().getSnowfall().get(i);
//
//                if (optionalData.isPresent()) {
//                    // 3) 존재하면 update
//                    WeatherData existing = optionalData.get();
//                    existing.updateWeatherData(
//                            temperature,
//                            precipitationProbability,
//                            precipitation,
//                            rain,
//                            snowfall,
//                            // updatedAt
//                            LocalDateTime.now()
//                    );
//                    // JPA save 시, 영속성 컨텍스트 내에 업데이트가 반영됨
//                    weatherDataRepository.save(existing);
//
//                } else {
//                    // 4) 없으면 insert
//                    WeatherData newData = WeatherData.builder()
//                            .timestamp(timestamp)
//                            .temperature(temperature)
//                            .precipitationProbability(precipitationProbability)
//                            .precipitation(precipitation)
//                            .rain(rain)
//                            .snowfall(snowfall)
//                            .retrievedAt(LocalDateTime.now())
//                            .build();
//
//                    weatherDataRepository.save(newData);
//                }
//
//
//                // 이미 해당 시각의 데이터가 DB에 있는지 검사
////                if (!weatherDataRepository.existsByTimestamp(timestamp)) {
////                    // 없으면 WeatherData 엔티티를 생성
////                    WeatherData weatherData = WeatherData.builder()
////                            .timestamp(timestamp)
////                            // 온도/강수확률/강수량/비/눈 등을 리스트에서 i번째 값으로 세팅
////                            .temperature(response.getHourly().getTemperature_2m().get(i))
////                            .precipitationProbability(response.getHourly().getPrecipitation_probability().get(i))
////                            .precipitation(response.getHourly().getPrecipitation().get(i))
////                            .rain(response.getHourly().getRain().get(i))
////                            .snowfall(response.getHourly().getSnowfall().get(i))
////                            // 데이터가 저장된 시점 기록
////                            .retrievedAt(LocalDateTime.now())
////                            .build();
////
////                    // JPA save 호출 -> DB에 insert
////                    weatherDataRepository.save(weatherData);
////                }
//            }
//        }
//    }
}
