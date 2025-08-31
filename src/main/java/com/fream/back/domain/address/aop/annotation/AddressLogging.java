package com.fream.back.domain.address.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Address 도메인 로깅 어노테이션
 * 메서드에 적용하여 로깅 레벨과 옵션을 세밀하게 제어
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AddressLogging {

    /**
     * 로깅 레벨 설정
     */
    LogLevel level() default LogLevel.DEBUG;

    /**
     * 로깅할 정보 타입들
     */
    LogType[] types() default {LogType.EXECUTION_TIME, LogType.PARAMETERS, LogType.RESULT};

    /**
     * 메서드 실행 전 로깅 여부
     */
    boolean logBefore() default true;

    /**
     * 메서드 실행 후 로깅 여부
     */
    boolean logAfter() default true;

    /**
     * 예외 발생 시 로깅 여부
     */
    boolean logException() default true;

    /**
     * 파라미터 마스킹 여부 (개인정보 보호)
     */
    boolean maskSensitiveData() default true;

    /**
     * 결과값 로깅 여부
     */
    boolean logResult() default false;

    /**
     * 커스텀 로그 메시지 (비어있으면 기본 메시지 사용)
     */
    String message() default "";

    /**
     * 로깅할 파라미터 인덱스들 (비어있으면 모든 파라미터 로깅)
     */
    int[] includeParams() default {};

    /**
     * 로깅에서 제외할 파라미터 인덱스들
     */
    int[] excludeParams() default {};

    /**
     * 로깅 레벨 열거형
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /**
     * 로깅 타입 열거형
     */
    enum LogType {
        EXECUTION_TIME,    // 실행 시간
        PARAMETERS,        // 메서드 파라미터
        RESULT,           // 반환값
        USER_INFO,        // 사용자 정보
        REQUEST_ID        // 요청 ID
    }
}