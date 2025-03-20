package com.fream.back.global.exception.security;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보안(Security) 관련 에러 코드
 * 접두사 'S'로 시작하는 코드를 사용
 */
@Getter
@RequiredArgsConstructor
public enum SecurityErrorCode implements ErrorCode {
    /**
     * 유효하지 않은 토큰 (401)
     * JWT 토큰의 서명이 유효하지 않거나 형식이 잘못된 경우
     */
    INVALID_TOKEN("S001", "유효하지 않은 토큰입니다.", 401),

    /**
     * 만료된 토큰 (401)
     * JWT 토큰의 유효 기간이 만료된 경우
     */
    EXPIRED_TOKEN("S002", "만료된 토큰입니다.", 401),

    /**
     * 토큰 찾을 수 없음 (401)
     * 요청에 토큰이 포함되지 않은 경우
     */
    TOKEN_NOT_FOUND("S003", "토큰을 찾을 수 없습니다.", 401),

    /**
     * 유효하지 않은 리프레시 토큰 (401)
     * 리프레시 토큰이 유효하지 않거나 Redis에 저장된 값과 일치하지 않는 경우
     */
    INVALID_REFRESH_TOKEN("S004", "유효하지 않은 리프레시 토큰입니다.", 401),

    /**
     * 사용자 찾을 수 없음 (404)
     * 토큰에 포함된 사용자 식별자로 사용자를 찾을 수 없는 경우
     */
    USER_NOT_FOUND("S005", "사용자를 찾을 수 없습니다.", 404),

    /**
     * 토큰 생성 오류 (500)
     * JWT 토큰 생성 과정에서 오류가 발생한 경우
     * 비밀키 문제나 JWT 라이브러리 관련 오류가 포함될 수 있음
     */
    TOKEN_CREATION_ERROR("S006", "토큰 생성에 실패했습니다.", 500);


    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}