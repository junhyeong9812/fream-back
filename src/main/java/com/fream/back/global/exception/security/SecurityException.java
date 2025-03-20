package com.fream.back.global.exception.security;

import com.fream.back.global.exception.GlobalException;

/**
 * 보안(Security) 관련 예외의 기본 클래스
 * 모든 보안 관련 예외는 이 클래스를 상속받음
 */
public abstract class SecurityException extends GlobalException {
    /**
     * SecurityErrorCode로 예외 생성
     *
     * @param errorCode 보안 관련 에러 코드
     */
    public SecurityException(SecurityErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * SecurityErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 보안 관련 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public SecurityException(SecurityErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * SecurityErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 보안 관련 에러 코드
     * @param cause 원인이 되는 예외
     */
    public SecurityException(SecurityErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}