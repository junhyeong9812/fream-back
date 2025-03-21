package com.fream.back.domain.weather.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 외부 날씨 API 호출 중 발생하는 예외를 처리하는 클래스
 */
public class WeatherApiException extends WeatherException {

    /**
     * WeatherErrorCode.WEATHER_API_ERROR 기본 에러 코드로 예외 생성
     */
    public WeatherApiException() {
        super(WeatherErrorCode.WEATHER_API_ERROR);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherApiException(String message) {
        super(WeatherErrorCode.WEATHER_API_ERROR, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WeatherApiException(Throwable cause) {
        super(WeatherErrorCode.WEATHER_API_ERROR, cause);
    }

    /**
     * 사용자 정의 에러 코드와 함께 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     */
    public WeatherApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 사용자 정의 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 사용자 정의 에러 코드와 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public WeatherApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 사용자 정의 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 기본 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherApiException(String message, Throwable cause) {
        super(WeatherErrorCode.WEATHER_API_ERROR, message, cause);
    }
}