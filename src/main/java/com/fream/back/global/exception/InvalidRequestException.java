package com.fream.back.global.exception;

/**
 * 유효하지 않은 요청 예외
 * 클라이언트의 요청이 유효하지 않은 경우 사용
 */
public class InvalidRequestException extends GlobalException {

    /**
     * 기본 메시지와 함께 예외 생성
     */
    public InvalidRequestException() {
        super(GlobalErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidRequestException(String message) {
        super(GlobalErrorCode.INVALID_INPUT_VALUE, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인 예외
     */
    public InvalidRequestException(Throwable cause) {
        super(GlobalErrorCode.INVALID_INPUT_VALUE, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidRequestException(String message, Throwable cause) {
        super(GlobalErrorCode.INVALID_INPUT_VALUE, message, cause);
    }
}