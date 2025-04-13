package com.fream.back.global.exception;

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 * 리소스(데이터)가 존재하지 않는 경우 사용
 */
public class EntityNotFoundException extends GlobalException {

    /**
     * 기본 메시지와 함께 예외 생성
     */
    public EntityNotFoundException() {
        super(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public EntityNotFoundException(String message) {
        super(GlobalErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인 예외
     */
    public EntityNotFoundException(Throwable cause) {
        super(GlobalErrorCode.RESOURCE_NOT_FOUND, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인 예외
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(GlobalErrorCode.RESOURCE_NOT_FOUND, message, cause);
    }
}