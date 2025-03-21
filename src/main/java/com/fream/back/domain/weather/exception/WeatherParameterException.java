package com.fream.back.domain.weather.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 날씨 API 파라미터 관련 예외를 처리하는 클래스
 */
public class WeatherParameterException extends WeatherException {

    /**
     * WeatherErrorCode.INVALID_DATETIME_FORMAT 기본 에러 코드로 예외 생성
     */
    public WeatherParameterException() {
        super(WeatherErrorCode.INVALID_DATETIME_FORMAT);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherParameterException(String message) {
        super(WeatherErrorCode.INVALID_DATETIME_FORMAT, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WeatherParameterException(Throwable cause) {
        super(WeatherErrorCode.INVALID_DATETIME_FORMAT, cause);
    }

    /**
     * 사용자 정의 에러 코드와 함께 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     */
    public WeatherParameterException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 사용자 정의 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public WeatherParameterException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 사용자 정의 에러 코드와 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public WeatherParameterException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 사용자 정의 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 날씨 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherParameterException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 기본 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WeatherParameterException(String message, Throwable cause) {
        super(WeatherErrorCode.INVALID_DATETIME_FORMAT, message, cause);
    }
}