package com.fream.back.domain.address.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Address 도메인 예외 처리 어노테이션
 * 메서드에 적용하여 예외 처리 방식과 옵션을 설정
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AddressExceptionHandling {

    /**
     * 예외 처리 전략
     */
    Strategy strategy() default Strategy.LOG_AND_RETHROW;

    /**
     * 처리할 예외 타입들 (비어있으면 모든 예외 처리)
     */
    Class<? extends Throwable>[] handleExceptions() default {};

    /**
     * 무시할 예외 타입들
     */
    Class<? extends Throwable>[] ignoreExceptions() default {};

    /**
     * 예외 발생 시 알림 여부
     */
    boolean sendAlert() default false;

    /**
     * 알림 레벨
     */
    AlertLevel alertLevel() default AlertLevel.ERROR;

    /**
     * 예외 발생 시 메트릭 수집 여부
     */
    boolean collectMetrics() default true;

    /**
     * 예외 발생 시 스택 트레이스 로깅 여부
     */
    boolean logStackTrace() default true;

    /**
     * 재시도 가능한 예외 여부
     */
    boolean retryable() default false;

    /**
     * 재시도 횟수 (retryable이 true인 경우)
     */
    int maxRetries() default 3;

    /**
     * 재시도 간격 (밀리초)
     */
    long retryDelay() default 1000;

    /**
     * 커스텀 예외 메시지
     */
    String customMessage() default "";

    /**
     * 예외 발생 시 사용자 정보 포함 여부
     */
    boolean includeUserInfo() default true;

    /**
     * 예외 발생 시 파라미터 정보 포함 여부
     */
    boolean includeParameters() default true;

    /**
     * 예외 컨텍스트 정보 수집 여부
     */
    boolean collectContext() default true;

    /**
     * 예외 처리 전략 열거형
     */
    enum Strategy {
        LOG_ONLY,           // 로그만 남기고 정상 처리
        LOG_AND_RETHROW,    // 로그 남기고 예외 재발생
        TRANSFORM,          // 다른 예외로 변환
        SUPPRESS,           // 예외 무시
        FALLBACK            // 폴백 처리
    }

    /**
     * 알림 레벨 열거형
     */
    enum AlertLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        CRITICAL
    }
}