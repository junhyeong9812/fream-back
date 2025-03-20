package com.fream.back.global.exception.security;
/**
 * 보안 관련 사용자 찾을 수 없음 예외
 * 토큰에 포함된 사용자 식별자로 사용자를 찾을 수 없는 경우 발생
 */
public class SecurityUserNotFoundException extends SecurityException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "사용자를 찾을 수 없습니다."
     */
    public SecurityUserNotFoundException() {
        super(SecurityErrorCode.USER_NOT_FOUND);
    }
}