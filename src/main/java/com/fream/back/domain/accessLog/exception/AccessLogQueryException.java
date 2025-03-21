package com.fream.back.domain.accessLog.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 접근 로그 조회 과정에서 발생하는 예외를 처리하는 클래스
 */
public class AccessLogQueryException extends AccessLogException {

    /**
     * AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR 기본 에러 코드로 예외 생성
     */
    public AccessLogQueryException() {
        super(AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public AccessLogQueryException(String message) {
        super(AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public AccessLogQueryException(Throwable cause) {
        super(AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR, cause);
    }

    /**
     * 사용자 정의 에러 코드와 함께 예외 생성
     *
     * @param errorCode 접근 로그 도메인 에러 코드
     */
    public AccessLogQueryException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 사용자 정의 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode 접근 로그 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public AccessLogQueryException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 사용자 정의 에러 코드와 원인 예외로 예외 생성
     *
     * @param errorCode 접근 로그 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public AccessLogQueryException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 사용자 정의 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 접근 로그 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public AccessLogQueryException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 기본 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public AccessLogQueryException(String message, Throwable cause) {
        super(AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR, message, cause);
    }
}