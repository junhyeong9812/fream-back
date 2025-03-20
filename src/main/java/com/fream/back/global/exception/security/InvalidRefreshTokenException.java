package com.fream.back.global.exception.security;

/**
 * 유효하지 않은 리프레시 토큰 예외
 * 리프레시 토큰이 유효하지 않거나 Redis에 저장된 값과 일치하지 않는 경우 발생
 */
public class InvalidRefreshTokenException extends SecurityException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "유효하지 않은 리프레시 토큰입니다."
     */
    public InvalidRefreshTokenException() {
        super(SecurityErrorCode.INVALID_REFRESH_TOKEN);
    }
}
