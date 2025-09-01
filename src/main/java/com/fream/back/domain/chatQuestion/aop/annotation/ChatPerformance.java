package com.fream.back.domain.chatQuestion.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ChatQuestion 도메인 성능 모니터링 어노테이션
 * GPT API 호출 시간, 응답 시간 등의 성능 모니터링을 설정
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatPerformance {

    /**
     * 성능 경고 임계값 (밀리초)
     * -1이면 기본값 사용 (레이어별 기본값)
     */
    long warningThreshold() default -1;

    /**
     * 성능 오류 임계값 (밀리초)
     * -1이면 기본값 사용 (레이어별 기본값)
     */
    long errorThreshold() default -1;

    /**
     * 성능 모니터링 활성화 여부
     */
    boolean enabled() default true;

    /**
     * 성능 통계 수집 여부
     */
    boolean collectStats() default true;

    /**
     * GPT API 응답 시간 모니터링 여부
     */
    boolean monitorGPTApiTime() default true;

    /**
     * 데이터베이스 쿼리 시간 모니터링 여부
     */
    boolean monitorDbQueryTime() default true;

    /**
     * 성능 리포트 생성 주기 (호출 횟수 기준)
     * 0이면 리포트 생성 안함
     */
    int reportInterval() default 50;

    /**
     * 메트릭스 태그 (모니터링 시스템 연동용)
     */
    String[] tags() default {};

    /**
     * 성능 모니터링 우선순위
     */
    Priority priority() default Priority.NORMAL;

    /**
     * 커스텀 메트릭 이름 (비어있으면 기본 이름 사용)
     */
    String metricName() default "";

    /**
     * 토큰 사용량 성능 임계값 (토큰 수)
     */
    int tokenUsageThreshold() default 2000;

    /**
     * API 호출 빈도 모니터링 여부
     */
    boolean monitorApiFrequency() default false;

    /**
     * 동시 처리 요청 수 모니터링 여부
     */
    boolean monitorConcurrentRequests() default false;

    /**
     * GPT 응답 품질 평가 여부
     */
    boolean evaluateResponseQuality() default false;

    /**
     * 우선순위 열거형
     */
    enum Priority {
        LOW,      // 낮은 우선순위 - 기본 모니터링
        NORMAL,   // 일반 우선순위 - 표준 모니터링
        HIGH,     // 높은 우선순위 - 상세 모니터링
        CRITICAL  // 중요 - 실시간 모니터링 및 알람
    }
}