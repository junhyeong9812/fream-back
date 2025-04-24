package com.fream.back.domain.weather.service.command;

import com.fream.back.domain.weather.api.WeatherApiClient;
import com.fream.back.domain.weather.dto.WeatherApiResponse;
import com.fream.back.domain.weather.entity.WeatherData;
import com.fream.back.domain.weather.exception.WeatherException;
import com.fream.back.domain.weather.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataCommandServiceImpl implements WeatherDataCommandService {

    // Repository: DB 저장/조회 담당
    private final WeatherDataRepository weatherDataRepository;

    // 날씨 API 클라이언트
    private final WeatherApiClient weatherApiClient;

    @Value("${weather.data.hours-count:48}")
    private int hoursCount;

    /**
     * 외부 API로부터 24시간 날씨 정보를 받아와,
     * DB에 저장하는 메서드
     */
    @Override
    @Transactional
    public void fetchAndStore24HourWeatherData() {
        log.info("날씨 데이터 가져오기 및 저장 시작");
        try {
            // 1) API 호출
            WeatherApiResponse response = weatherApiClient.getWeatherData();
            if (response == null || response.getHourly() == null) {
                throw WeatherException.apiError("날씨 API 응답이 null입니다.");
            }

            // 처리할 시간 수 결정
            int actualHoursCount = Math.min(hoursCount, response.getHourly().getTime().size());
            log.debug("처리할 시간 수: {}", actualHoursCount);

            // 2) timestamp 리스트 구성 및 정렬
            List<LocalDateTime> timestamps = parseTimestamps(response, actualHoursCount);

            // 정렬
            timestamps.sort(LocalDateTime::compareTo);
            LocalDateTime startTime = timestamps.get(0);
            LocalDateTime endTime = timestamps.get(timestamps.size() - 1);
            log.debug("범위: {} ~ {}", startTime, endTime);

            // 3) DB에서 [startTime, endTime] 범위 조회
            List<WeatherData> existingList =
                    weatherDataRepository.findByTimestampBetween(startTime, endTime);

            // 4) 기존 레코드를 Map으로 변환: timestamp -> WeatherData
            Map<LocalDateTime, WeatherData> existingMap = new HashMap<>();
            for (WeatherData wd : existingList) {
                existingMap.put(wd.getTimestamp(), wd);
            }
            log.debug("기존 데이터 수: {}", existingMap.size());

            // 5) upsert 로직 - 일괄 처리
            List<WeatherData> toSave = saveOrUpdateWeatherData(response, timestamps, existingMap, actualHoursCount);

            // 6) 일괄 저장
            weatherDataRepository.saveAll(toSave);
            log.info("날씨 데이터 가져오기 및 저장 완료. 처리된 레코드 수: {}", toSave.size());

        } catch (Exception e) {
            log.error("날씨 데이터 처리 중 예상치 못한 오류 발생", e);
            if (e instanceof WeatherException) {
                throw e;
            } else {
                throw WeatherException.dataSaveError("날씨 데이터 저장 중 오류가 발생했습니다.", e);
            }
        }
    }

    /**
     * API 응답에서 타임스탬프 목록 파싱
     *
     * @param response API 응답 객체
     * @param hoursCount 처리할 시간 수
     * @return 타임스탬프 목록
     * @throws WeatherException 파싱 실패 시
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
            log.error("날짜/시간 파싱 중 오류 발생: {}", e.getMessage());
            throw WeatherException.apiParsingError("날짜/시간 파싱 중 오류가 발생했습니다.", e);
        } catch (IndexOutOfBoundsException e) {
            log.error("API 응답 데이터 형식이 올바르지 않음: {}", e.getMessage());
            throw WeatherException.apiParsingError("API 응답 데이터 형식이 올바르지 않습니다.", e);
        }
    }

    /**
     * 날씨 데이터 저장 또는 업데이트
     * 일괄 처리를 위해 수정된 엔티티 리스트 반환
     *
     * @param response API 응답 객체
     * @param timestamps 타임스탬프 목록
     * @param existingMap 기존 데이터 맵
     * @param hoursCount 처리할 시간 수
     * @return 저장할 엔티티 리스트
     * @throws WeatherException 데이터 처리 실패 시
     */
    private List<WeatherData> saveOrUpdateWeatherData(
            WeatherApiResponse response,
            List<LocalDateTime> timestamps,
            Map<LocalDateTime, WeatherData> existingMap,
            int hoursCount) {

        List<WeatherData> toSave = new ArrayList<>();
        try {
            for (int i = 0; i < hoursCount; i++) {
                LocalDateTime timestamp = timestamps.get(i);

                double temperature = response.getHourly().getTemperature_2m().get(i);
                double precipitationProbability = response.getHourly().getPrecipitation_probability().get(i);
                double precipitation = response.getHourly().getPrecipitation().get(i);
                double rain = response.getHourly().getRain().get(i);
                double snowfall = response.getHourly().getSnowfall().get(i);

                LocalDateTime now = LocalDateTime.now();

                WeatherData entity = existingMap.get(timestamp);
                if (entity != null) {
                    // 존재 -> update
                    entity.updateWeatherData(
                            temperature,
                            precipitationProbability,
                            precipitation,
                            rain,
                            snowfall,
                            now
                    );
                    toSave.add(entity);
                    log.debug("날씨 데이터 업데이트: timestamp={}", timestamp);
                } else {
                    // 없으면 insert
                    WeatherData newData = WeatherData.builder()
                            .timestamp(timestamp)
                            .temperature(temperature)
                            .precipitationProbability(precipitationProbability)
                            .precipitation(precipitation)
                            .rain(rain)
                            .snowfall(snowfall)
                            .retrievedAt(now)
                            .build();
                    toSave.add(newData);
                    log.debug("새 날씨 데이터 추가: timestamp={}", timestamp);
                }
            }
            return toSave;
        } catch (Exception e) {
            log.error("날씨 데이터 처리 중 오류 발생: {}", e.getMessage());
            throw WeatherException.dataSaveError("날씨 데이터 저장 중 오류가 발생했습니다.", e);
        }
    }
}