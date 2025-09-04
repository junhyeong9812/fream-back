package com.fream.back.domain.faq.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FAQ 캐싱 어노테이션
 * FAQ는 자주 조회되지만 변경이 적은 특성을 활용한 적극적 캐싱 전략 적용
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FAQCache {

    /**
     * 캐시 활성화 여부
     */
    boolean enabled() default true;

    /**
     * 캐시 이름
     */
    String cacheName() default "faqCache";

    /**
     * 캐시 키 생성 전략
     */
    CacheKeyStrategy keyStrategy() default CacheKeyStrategy.DEFAULT;

    /**
     * TTL (Time To Live) - 초 단위
     */
    int ttlSeconds() default 3600; // 1시간

    /**
     * 카테고리별 캐시 분리 사용 여부
     */
    boolean useMultipleCaches() default false;

    /**
     * 캐시 통계 로깅 여부
     */
    boolean logStatistics() default true;

    /**
     * 조회수 추적 여부
     */
    boolean trackHitCount() default true;

    /**
     * 캐시 워밍업 여부
     */
    boolean warmUp() default false;

    /**
     * 캐시 무효화 전파 여부
     */
    boolean propagateInvalidation() default true;

    /**
     * 압축 사용 여부 (대용량 컨텐츠)
     */
    boolean useCompression() default false;

    /**
     * 캐시 키 생성 전략
     */
    enum CacheKeyStrategy {
        DEFAULT,           // 메서드명 + 파라미터
        ID_BASED,         // FAQ ID 기반
        CATEGORY_BASED,   // 카테고리 기반
        SEARCH_BASED,     // 검색어 기반
        COMPOSITE,        // 복합 키
        CUSTOM            // 커스텀 키 생성
    }

    /**
     * 캐시 무효화 정책
     */
    enum InvalidationPolicy {
        ON_UPDATE,        // 업데이트 시
        ON_DELETE,        // 삭제 시
        SCHEDULED,        // 스케줄 기반
        MANUAL,           // 수동
        TTL_ONLY         // TTL만 사용
    }

    /**
     * 캐시 무효화 정책
     */
    InvalidationPolicy[] invalidationPolicies() default {InvalidationPolicy.ON_UPDATE, InvalidationPolicy.ON_DELETE};
}