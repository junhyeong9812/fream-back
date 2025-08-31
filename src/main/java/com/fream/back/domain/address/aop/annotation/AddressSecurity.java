package com.fream.back.domain.address.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Address 도메인 보안 어노테이션
 * 보안이 중요한 메서드에 적용하여 접근 제어 및 보안 로깅을 수행
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AddressSecurity {

    /**
     * 필요한 권한들
     */
    String[] requiredRoles() default {};

    /**
     * 자원 소유자만 접근 가능 여부
     */
    boolean ownerOnly() default true;

    /**
     * 소유자 확인을 위한 파라미터 인덱스
     * (이메일이나 사용자 ID가 들어있는 파라미터 위치)
     */
    int ownerParamIndex() default 0;

    /**
     * 보안 검증 타입
     */
    SecurityCheck[] checks() default {SecurityCheck.AUTHENTICATION, SecurityCheck.AUTHORIZATION};

    /**
     * 보안 위반 시 처리 방식
     */
    ViolationAction violationAction() default ViolationAction.THROW_EXCEPTION;

    /**
     * 보안 감사 로그 활성화 여부
     */
    boolean enableAuditLog() default true;

    /**
     * IP 주소 기반 접근 제한 여부
     */
    boolean checkIpRestriction() default false;

    /**
     * 허용된 IP 주소 패턴들
     */
    String[] allowedIpPatterns() default {};

    /**
     * 시간 기반 접근 제한 여부
     */
    boolean checkTimeRestriction() default false;

    /**
     * 접근 허용 시간 (24시간 형식, 예: "09:00-18:00")
     */
    String allowedTimeRange() default "";

    /**
     * 속도 제한 활성화 여부
     */
    boolean enableRateLimit() default false;

    /**
     * 속도 제한 - 시간 윈도우 (초)
     */
    int rateLimitWindow() default 60;

    /**
     * 속도 제한 - 최대 요청 수
     */
    int maxRequestsPerWindow() default 100;

    /**
     * 개인정보 접근 여부 (GDPR, 개인정보보호법 준수)
     */
    boolean accessesPersonalData() default true;

    /**
     * 데이터 처리 목적 (개인정보보호법 준수)
     */
    String[] processingPurposes() default {};

    /**
     * 암호화된 데이터 접근 여부
     */
    boolean accessesEncryptedData() default false;

    /**
     * 보안 검증 타입 열거형
     */
    enum SecurityCheck {
        AUTHENTICATION,      // 인증 확인
        AUTHORIZATION,       // 인가 확인
        OWNERSHIP,          // 소유권 확인
        IP_RESTRICTION,     // IP 제한 확인
        TIME_RESTRICTION,   // 시간 제한 확인
        RATE_LIMIT,        // 속도 제한 확인
        DATA_ENCRYPTION,   // 데이터 암호화 확인
        AUDIT_TRAIL       // 감사 추적 확인
    }

    /**
     * 보안 위반 처리 방식 열거형
     */
    enum ViolationAction {
        THROW_EXCEPTION,    // 예외 발생
        LOG_AND_DENY,      // 로그 남기고 접근 거부
        LOG_AND_ALLOW,     // 로그 남기고 접근 허용 (경고용)
        SILENT_DENY,       // 조용히 접근 거부
        REDIRECT          // 다른 페이지로 리다이렉트
    }
}