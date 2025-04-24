package com.fream.back.domain.weather.service.query;

import com.fream.back.domain.weather.dto.WeatherDataDto;
import com.fream.back.domain.weather.entity.WeatherData;
import com.fream.back.domain.weather.exception.WeatherException;
import com.fream.back.domain.weather.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용
public class WeatherQueryServiceImpl implements WeatherQueryService {

    // DB 접근용 레포지토리
    private final WeatherDataRepository weatherDataRepository;

    /**
     * 현재 시각과 ±1시간 범위 내에서,
     * "가장 가까운 시각"의 WeatherData를 찾는다.
     *
     * @return 가장 가까운 시간의 날씨 데이터 (Optional)
     * @throws WeatherException 데이터 조회 실패 시
     */
    @Override
    public Optional<WeatherDataDto> getClosestWeatherData() {
        log.debug("현재 시각과 가장 가까운 날씨 데이터 조회 시작");
        try {
            // 현재 시각
            LocalDateTime now = LocalDateTime.now();
            // 검색 범위: [now-1h, now+1h]
            LocalDateTime startTime = now.minusHours(1);
            LocalDateTime endTime = now.plusHours(1);

            // 해당 범위의 날씨 데이터 목록 조회
            List<WeatherData> weatherDataList =
                    weatherDataRepository.findWithinTimeRange(startTime, endTime);

            if (weatherDataList.isEmpty()) {
                log.info("현재 시각({})과 가까운 범위에 날씨 데이터가 없습니다.", now);
                return Optional.empty();
            }

            // 최솟값(가장 가까운 시차)을 찾음
            WeatherData closest = weatherDataList.stream()
                    .min(Comparator.comparingLong(w ->
                            Math.abs(Duration.between(w.getTimestamp(), now).toMillis())))
                    .orElse(null);

            if (closest == null) {
                log.warn("가장 가까운 날씨 데이터를 찾을 수 없습니다.");
                return Optional.empty();
            }

            log.debug("현재 시각과 가장 가까운 날씨 데이터 찾음: timestamp={}", closest.getTimestamp());
            return Optional.of(WeatherDataDto.fromEntity(closest));
        } catch (Exception e) {
            log.error("현재 시각과 가장 가까운 날씨 데이터 조회 중 오류 발생", e);
            throw WeatherException.dataQueryError(
                    "현재 시각과 가장 가까운 날씨 데이터 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 오늘(0시 ~ 23:59:59) 사이의 모든 날씨 데이터 조회 후,
     * 시간 오름차순 정렬하여 반환
     *
     * @return 오늘의 날씨 데이터 목록
     * @throws WeatherException 데이터 조회 실패 시
     */
    @Override
    public List<WeatherDataDto> getTodayWeatherData() {
        log.debug("오늘의 날씨 데이터 조회 시작");
        try {
            // 오늘 0시 (startOfDay)
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            // 오늘 23:59:59 (endOfDay)
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            log.debug("조회 범위: {} ~ {}", startOfDay, endOfDay);

            // 범위 내 데이터 검색 후, 시간 순 정렬
            List<WeatherData> weatherDataList = weatherDataRepository.findByTimestampBetween(startOfDay, endOfDay);

            if (weatherDataList.isEmpty()) {
                log.info("오늘의 날씨 데이터가 없습니다.");
                return Collections.emptyList();
            }

            // 시간순 정렬 및 DTO 변환
            List<WeatherDataDto> result = weatherDataList.stream()
                    .sorted(Comparator.comparing(WeatherData::getTimestamp))
                    .map(WeatherDataDto::fromEntity)
                    .collect(Collectors.toList());

            log.debug("오늘의 날씨 데이터 조회 완료: {}개 항목", result.size());
            return result;
        } catch (Exception e) {
            log.error("오늘의 날씨 데이터 조회 중 오류 발생", e);
            throw WeatherException.dataQueryError(
                    "오늘의 날씨 데이터 조회 중 오류가 발생했습니다.", e);
        }
    }
}