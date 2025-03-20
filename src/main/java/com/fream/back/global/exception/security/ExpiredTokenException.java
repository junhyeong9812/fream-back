package com.fream.back.global.exception.security;

/**
 * 만료된 토큰 예외
 * JWT 토큰의 유효 기간이 만료된 경우 발생
 */
public class ExpiredTokenException extends SecurityException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "만료된 토큰입니다."
     */
    public ExpiredTokenException() {
        super(SecurityErrorCode.EXPIRED_TOKEN);
    }
}
