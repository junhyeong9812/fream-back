package com.fream.back.domain.weather.exception;

import com.fream.back.global.exception.ErrorCode;
import com.fream.back.global.exception.GlobalException;

/**
 * 날씨 도메인에서 발생하는 모든 예외의 기본 클래스
 * 팩토리 메서드 패턴을 사용하여 다양한 예외 상황을 단일 클래스로 처리
 */
public class WeatherException extends GlobalException {

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     */
    public WeatherException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public WeatherException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * ErrorCode, 사용자 정의 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    // ======= API 관련 예외를 위한 팩토리 메서드 =======

    /**
     * API 호출 오류 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException apiError(String message) {
        return new WeatherException(WeatherErrorCode.WEATHER_API_ERROR, message);
    }

    /**
     * API 호출 오류 예외 생성
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     * @return WeatherException 인스턴스
     */
    public static WeatherException apiError(String message, Throwable cause) {
        return new WeatherException(WeatherErrorCode.WEATHER_API_ERROR, message, cause);
    }

    /**
     * API 파싱 오류 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException apiParsingError(String message) {
        return new WeatherException(WeatherErrorCode.WEATHER_API_PARSING_ERROR, message);
    }

    /**
     * API 파싱 오류 예외 생성
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     * @return WeatherException 인스턴스
     */
    public static WeatherException apiParsingError(String message, Throwable cause) {
        return new WeatherException(WeatherErrorCode.WEATHER_API_PARSING_ERROR, message, cause);
    }

    // ======= 데이터 관련 예외를 위한 팩토리 메서드 =======

    /**
     * 데이터 없음 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException dataNotFound(String message) {
        return new WeatherException(WeatherErrorCode.WEATHER_DATA_NOT_FOUND, message);
    }

    /**
     * 데이터 저장 오류 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException dataSaveError(String message) {
        return new WeatherException(WeatherErrorCode.WEATHER_DATA_SAVE_ERROR, message);
    }

    /**
     * 데이터 저장 오류 예외 생성
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     * @return WeatherException 인스턴스
     */
    public static WeatherException dataSaveError(String message, Throwable cause) {
        return new WeatherException(WeatherErrorCode.WEATHER_DATA_SAVE_ERROR, message, cause);
    }

    /**
     * 데이터 조회 오류 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException dataQueryError(String message) {
        return new WeatherException(WeatherErrorCode.WEATHER_DATA_QUERY_ERROR, message);
    }

    /**
     * 데이터 조회 오류 예외 생성
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     * @return WeatherException 인스턴스
     */
    public static WeatherException dataQueryError(String message, Throwable cause) {
        return new WeatherException(WeatherErrorCode.WEATHER_DATA_QUERY_ERROR, message, cause);
    }

    // ======= 파라미터 관련 예외를 위한 팩토리 메서드 =======

    /**
     * 유효하지 않은 날짜/시간 형식 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException invalidDateTimeFormat(String message) {
        return new WeatherException(WeatherErrorCode.INVALID_DATETIME_FORMAT, message);
    }

    /**
     * 유효하지 않은 시간 범위 예외 생성
     *
     * @param message 에러 메시지
     * @return WeatherException 인스턴스
     */
    public static WeatherException invalidTimeRange(String message) {
        return new WeatherException(WeatherErrorCode.INVALID_TIME_RANGE, message);
    }
}