package com.fream.back.global.exception.security;

/**
 * 토큰 찾을 수 없음 예외
 * 요청에 토큰이 포함되지 않은 경우 발생
 */
public class TokenNotFoundException extends SecurityException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "토큰을 찾을 수 없습니다."
     */
    public TokenNotFoundException() {
        super(SecurityErrorCode.TOKEN_NOT_FOUND);
    }
}