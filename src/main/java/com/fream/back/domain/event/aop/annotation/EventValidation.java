package com.fream.back.domain.event.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event 도메인 비즈니스 유효성 검사 어노테이션
 * 이벤트 날짜, 상태, 브랜드 관련 비즈니스 규칙 검증
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventValidation {

    /**
     * 수행할 유효성 검사 타입들
     */
    ValidationType[] validations() default {
            ValidationType.DATE_RANGE,
            ValidationType.BUSINESS_RULE
    };

    /**
     * 이벤트 최소 지속 시간 (시간 단위)
     */
    int minDurationHours() default 1;

    /**
     * 이벤트 최대 지속 시간 (일 단위)
     */
    int maxDurationDays() default 365;

    /**
     * 과거 날짜 허용 여부
     */
    boolean allowPastDates() default false;

    /**
     * 미래 날짜 제한 (일 단위)
     */
    int maxFutureDays() default 365;

    /**
     * 동일한 브랜드의 중복 이벤트 허용 여부
     */
    boolean allowDuplicateEvents() default true;

    /**
     * 브랜드 활성 상태 확인 여부
     */
    boolean checkBrandStatus() default true;

    /**
     * 이벤트 제목 중복 확인 여부
     */
    boolean checkTitleDuplicate() default false;

    /**
     * 이벤트 용량 제한 (심플 이미지 + 썸네일)
     */
    long maxTotalFileSize() default 52428800; // 50MB

    /**
     * 이벤트 상태 변경 유효성 검사 여부
     */
    boolean validateStatusTransition() default true;

    /**
     * 관리자 권한 검증 여부
     */
    boolean requireAdminPermission() default true;

    /**
     * 유효성 검사 실패 시 처리 방식
     */
    ValidationFailureAction failureAction() default ValidationFailureAction.THROW_EXCEPTION;

    /**
     * 커스텀 유효성 검사 메시지
     */
    String customMessage() default "";

    /**
     * 유효성 검사 타입
     */
    enum ValidationType {
        DATE_RANGE,           // 날짜 범위 검증
        BUSINESS_RULE,        // 비즈니스 규칙 검증
        BRAND_VALIDATION,     // 브랜드 유효성 검증
        FILE_VALIDATION,      // 파일 유효성 검증
        STATUS_VALIDATION,    // 상태 전환 유효성 검증
        PERMISSION_CHECK,     // 권한 검증
        DUPLICATE_CHECK,      // 중복 검증
        CAPACITY_CHECK        // 용량 검증
    }

    /**
     * 유효성 검사 실패 처리 방식
     */
    enum ValidationFailureAction {
        THROW_EXCEPTION,    // 예외 발생
        LOG_AND_CONTINUE,   // 로그 남기고 계속 진행
        RETURN_DEFAULT,     // 기본값 반환
        PROMPT_USER         // 사용자에게 확인 요청
    }
}