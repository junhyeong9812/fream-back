package com.fream.back.domain.event.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event 도메인 모니터링 어노테이션
 * 이벤트 생성/수정/삭제, 상태 변경, 파일 작업 등의 모니터링
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventMonitoring {

    /**
     * 모니터링할 메트릭 타입들
     */
    MetricType[] metrics() default {
            MetricType.EXECUTION_TIME,
            MetricType.EVENT_LIFECYCLE,
            MetricType.FILE_OPERATIONS
    };

    /**
     * 이벤트 라이프사이클 추적 여부
     */
    boolean trackLifecycle() default true;

    /**
     * 파일 작업 모니터링 여부
     */
    boolean monitorFileOperations() default true;

    /**
     * 사용자 활동 추적 여부
     */
    boolean trackUserActivity() default true;

    /**
     * 이벤트 상태 변경 알림 여부
     */
    boolean alertOnStatusChange() default true;

    /**
     * 대용량 파일 업로드 알림 여부
     */
    boolean alertOnLargeFileUpload() default true;

    /**
     * 이벤트 생성 빈도 모니터링 여부
     */
    boolean monitorCreationFrequency() default true;

    /**
     * 브랜드별 이벤트 통계 수집 여부
     */
    boolean collectBrandStatistics() default true;

    /**
     * 이벤트 참여도 메트릭 수집 여부
     */
    boolean collectEngagementMetrics() default false;

    /**
     * 스케줄러 작업 모니터링 여부
     */
    boolean monitorSchedulerJobs() default true;

    /**
     * 캐시 성능 모니터링 여부
     */
    boolean monitorCachePerformance() default true;

    /**
     * 메트릭 수집 간격 (초)
     */
    int collectionIntervalSeconds() default 60;

    /**
     * 알림 임계값들
     */
    long fileUploadThresholdMB() default 5;
    int eventCreationThresholdPerHour() default 10;
    double cacheHitRateThreshold() default 0.8;

    /**
     * 모니터링 메트릭 타입
     */
    enum MetricType {
        EXECUTION_TIME,         // 실행 시간
        EVENT_LIFECYCLE,        // 이벤트 라이프사이클
        FILE_OPERATIONS,        // 파일 작업
        USER_ACTIVITY,          // 사용자 활동
        CACHE_PERFORMANCE,      // 캐시 성능
        DATABASE_QUERIES,       // 데이터베이스 쿼리
        BRAND_STATISTICS,       // 브랜드 통계
        SCHEDULER_PERFORMANCE,  // 스케줄러 성능
        ENGAGEMENT_METRICS,     // 참여도 메트릭
        SYSTEM_RESOURCES        // 시스템 리소스
    }
}