package com.fream.back.global.exception.security;
/**
 * 유효하지 않은 토큰 예외
 * JWT 토큰의 서명이 유효하지 않거나 형식이 잘못된 경우 발생
 */
public class InvalidTokenException extends SecurityException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "유효하지 않은 토큰입니다."
     */
    public InvalidTokenException() {
        super(SecurityErrorCode.INVALID_TOKEN);
    }

    /**
     * 사용자 정의 메시지로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidTokenException(String message) {
        super(SecurityErrorCode.INVALID_TOKEN, message);
    }
}