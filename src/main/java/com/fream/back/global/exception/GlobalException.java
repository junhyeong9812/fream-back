package com.fream.back.global.exception;

import lombok.Getter;

/**
 * 모든 커스텀 예외의 기본이 되는 추상 클래스
 * ErrorCode를 포함하여 일관된 예외 처리를 가능하게 함
 */
@Getter
public abstract class GlobalException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * ErrorCode만으로 예외 생성
     * 기본 에러 메시지는 ErrorCode에서 제공하는 메시지를 사용
     *
     * @param errorCode 에러 코드 enum 값
     */
    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 사용자 정의 메시지로 예외 생성
     * ErrorCode의 메시지 대신 사용자 정의 메시지를 사용
     *
     * @param errorCode 에러 코드 enum 값
     * @param message 사용자 정의 에러 메시지
     */
    public GlobalException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     * 다른 예외를 래핑할 때 사용
     *
     * @param errorCode 에러 코드 enum 값
     * @param cause 원인이 되는 예외
     */
    public GlobalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}