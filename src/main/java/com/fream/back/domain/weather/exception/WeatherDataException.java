package com.fream.back.domain.weather.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 날씨 데이터 처리 중 발생하는 예외를 처리하는 클래스
 */
public class WeatherDataException extends WeatherException {

    /**
     * WeatherErrorCode.WEATHER_DATA_NOT_FOUND 기본 에러 코드로 예외 생성
     */
    public WeatherDataException() {
        super(WeatherErrorCode.WEATHER_DATA_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherDataException(String message) {
        super(WeatherErrorCode.WEATHER_DATA_NOT_FOUND, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WeatherDataException(Throwable cause) {
        super(WeatherErrorCode.WEATHER_DATA_NOT_FOUND, cause);
    }

    /**
     * 사용자 정의 에러 코드와 함께 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     */
    public WeatherDataException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 사용자 정의 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherDataException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 사용자 정의 에러 코드와 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public WeatherDataException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 사용자 정의 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherDataException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 기본 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherDataException(String message, Throwable cause) {
        super(WeatherErrorCode.WEATHER_DATA_NOT_FOUND, message, cause);
    }
}