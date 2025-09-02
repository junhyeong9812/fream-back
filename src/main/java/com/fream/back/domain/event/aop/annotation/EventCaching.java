package com.fream.back.domain.event.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event 도메인 캐싱 어노테이션
 * 이벤트 조회 성능 최적화를 위한 캐싱 제어
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventCaching {

    /**
     * 캐시 키 생성 전략
     */
    CacheKeyStrategy keyStrategy() default CacheKeyStrategy.DEFAULT;

    /**
     * 캐시 TTL (Time To Live) - 초 단위
     */
    int ttlSeconds() default 300; // 5분

    /**
     * 캐시 활성화 여부
     */
    boolean enabled() default true;

    /**
     * 이벤트 상태 변경 시 캐시 무효화 여부
     */
    boolean invalidateOnStatusChange() default true;

    /**
     * 브랜드별 캐시 분리 여부
     */
    boolean separateByBrand() default false;

    /**
     * 사용자별 캐시 분리 여부 (관리자/일반 사용자)
     */
    boolean separateByUserRole() default false;

    /**
     * 캐시 워밍업 여부
     */
    boolean warmUp() default false;

    /**
     * 캐시 히트/미스 메트릭 수집 여부
     */
    boolean collectMetrics() default true;

    /**
     * 조건부 캐싱 활성화 여부
     */
    boolean conditionalCaching() default false;

    /**
     * 캐시 조건 (conditionalCaching이 true일 때)
     */
    String condition() default "";

    /**
     * 캐시 키 생성 전략
     */
    enum CacheKeyStrategy {
        DEFAULT,           // 기본 키 생성 (메서드명 + 파라미터)
        EVENT_ID,         // 이벤트 ID 기반
        BRAND_ID,         // 브랜드 ID 기반
        USER_SPECIFIC,    // 사용자별
        STATUS_BASED,     // 상태별
        TIME_BASED,       // 시간 기반
        COMPOSITE         // 복합 키
    }
}