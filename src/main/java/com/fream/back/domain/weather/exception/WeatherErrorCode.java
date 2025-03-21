package com.fream.back.domain.weather.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 날씨 도메인에서 사용하는 에러 코드
 * 접두사 'W'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum WeatherErrorCode implements ErrorCode {

    // API 관련 에러
    /**
     * 외부 날씨 API 호출 실패 (500)
     * 외부 API 서버 연결 또는 응답 처리 중 오류 발생
     */
    WEATHER_API_ERROR("WE001", "날씨 API 호출 중 오류가 발생했습니다.", 500),

    /**
     * 날씨 API 응답 파싱 오류 (500)
     * API 응답을 객체로 변환하는 과정에서 발생하는 오류
     */
    WEATHER_API_PARSING_ERROR("WE002", "날씨 API 응답 처리 중 오류가 발생했습니다.", 500),

    // 데이터 관련 에러
    /**
     * 날씨 데이터 없음 (404)
     * 요청한 시간대의 날씨 데이터가 존재하지 않는 경우
     */
    WEATHER_DATA_NOT_FOUND("WE101", "요청한 시간대의 날씨 데이터를 찾을 수 없습니다.", 404),

    /**
     * 날씨 데이터 저장 실패 (500)
     * 날씨 데이터를 데이터베이스에 저장하는 과정에서 발생한 오류
     */
    WEATHER_DATA_SAVE_ERROR("WE102", "날씨 데이터 저장 중 오류가 발생했습니다.", 500),

    /**
     * 날씨 데이터 조회 실패 (500)
     * 데이터베이스에서 날씨 데이터를 조회하는 과정에서 발생한 오류
     */
    WEATHER_DATA_QUERY_ERROR("WE103", "날씨 데이터 조회 중 오류가 발생했습니다.", 500),

    // 파라미터 관련 에러
    /**
     * 유효하지 않은 날짜/시간 형식 (400)
     * 날짜/시간 파라미터가 올바른 형식이 아닌 경우
     */
    INVALID_DATETIME_FORMAT("WE201", "유효하지 않은 날짜/시간 형식입니다.", 400),

    /**
     * 유효하지 않은 조회 범위 (400)
     * 시작 시간이 종료 시간보다 늦은 경우 등 논리적으로 맞지 않는 조회 범위
     */
    INVALID_TIME_RANGE("WE202", "유효하지 않은 시간 범위입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}