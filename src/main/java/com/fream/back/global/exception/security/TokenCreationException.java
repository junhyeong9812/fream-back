package com.fream.back.global.exception.security;

/**
 * 토큰 생성 실패 예외
 * JWT 토큰 생성 과정에서 오류가 발생한 경우 발생
 */
public class TokenCreationException extends SecurityException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "토큰 생성에 실패했습니다."
     */
    public TokenCreationException() {
        super(SecurityErrorCode.TOKEN_CREATION_ERROR);
    }

    /**
     * 원인 예외로 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public TokenCreationException(Throwable cause) {
        super(SecurityErrorCode.TOKEN_CREATION_ERROR, cause);
    }
}