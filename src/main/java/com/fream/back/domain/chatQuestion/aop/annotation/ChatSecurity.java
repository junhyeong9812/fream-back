package com.fream.back.domain.chatQuestion.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ChatQuestion 도메인 보안 어노테이션
 * GPT API 키 보호, 사용량 제한, 악용 방지 등의 보안 제어
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatSecurity {

    /**
     * 필요한 권한들
     */
    String[] requiredRoles() default {};

    /**
     * 인증된 사용자만 접근 가능 여부
     */
    boolean requireAuthentication() default true;

    /**
     * 관리자 권한 필요 여부
     */
    boolean requireAdminRole() default false;

    /**
     * 보안 검증 타입
     */
    SecurityCheck[] checks() default {SecurityCheck.AUTHENTICATION, SecurityCheck.RATE_LIMIT};

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
    boolean enableRateLimit() default true;

    /**
     * 속도 제한 - 시간 윈도우 (초)
     */
    int rateLimitWindow() default 60;

    /**
     * 속도 제한 - 최대 요청 수
     */
    int maxRequestsPerWindow() default 10;

    /**
     * 일일 사용량 제한 (토큰 수)
     */
    int dailyTokenLimit() default 10000;

    /**
     * 월간 사용량 제한 (토큰 수)
     */
    int monthlyTokenLimit() default 100000;

    /**
     * 질문 길이 제한 (문자 수)
     */
    int maxQuestionLength() default 2000;

    /**
     * 스팸 질문 감지 여부
     */
    boolean detectSpam() default true;

    /**
     * 반복 질문 제한 활성화 여부
     */
    boolean limitRepeatedQuestions() default true;

    /**
     * 악성 콘텐츠 필터링 여부
     */
    boolean filterMaliciousContent() default true;

    /**
     * GPT API 키 보호 모드 활성화 여부
     */
    boolean protectApiKey() default true;

    /**
     * 민감한 정보 요청 차단 여부
     */
    boolean blockSensitiveRequests() default true;

    /**
     * 관리자 기능 접근 시 추가 인증 여부
     */
    boolean requireAdditionalAuth() default false;

    /**
     * 보안 검증 타입 열거형
     */
    enum SecurityCheck {
        AUTHENTICATION,          // 인증 확인
        AUTHORIZATION,          // 인가 확인
        RATE_LIMIT,            // 속도 제한 확인
        IP_RESTRICTION,        // IP 제한 확인
        TIME_RESTRICTION,      // 시간 제한 확인
        TOKEN_LIMIT,          // 토큰 사용량 제한
        CONTENT_FILTER,       // 콘텐츠 필터링
        SPAM_DETECTION,       // 스팸 감지
        REPEATED_QUESTION,    // 반복 질문 제한
        API_KEY_PROTECTION,   // API 키 보호
        SENSITIVE_DATA_BLOCK  // 민감한 데이터 요청 차단
    }

    /**
     * 보안 위반 처리 방식 열거형
     */
    enum ViolationAction {
        THROW_EXCEPTION,     // 예외 발생
        LOG_AND_DENY,       // 로그 남기고 접근 거부
        LOG_AND_THROTTLE,   // 로그 남기고 요청 지연
        SILENT_DENY,        // 조용히 접근 거부
        FALLBACK_RESPONSE,  // 기본 응답 반환
        BLACKLIST_USER      // 사용자 블랙리스트 추가
    }
}