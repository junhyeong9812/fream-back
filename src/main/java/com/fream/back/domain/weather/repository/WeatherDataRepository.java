package com.fream.back.domain.weather.repository;

import com.fream.back.domain.weather.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    // 특정 시간대와 가장 가까운 데이터 찾기
    @Query("SELECT w FROM WeatherData w " +
            "WHERE w.timestamp BETWEEN :startTime AND :endTime " +
            "ORDER BY ABS(TIMESTAMPDIFF(SECOND, w.timestamp, :targetTime)) ASC")
    Optional<WeatherData> findClosestToTimeWithinRange(
            @Param("targetTime") LocalDateTime targetTime,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT w FROM WeatherData w " +
            "WHERE w.timestamp BETWEEN :startTime AND :endTime")
    List<WeatherData> findWithinTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );


    // 특정 날짜 범위 데이터 찾기
    List<WeatherData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    //중복검사
    boolean existsByTimestamp(LocalDateTime timestamp);

}
