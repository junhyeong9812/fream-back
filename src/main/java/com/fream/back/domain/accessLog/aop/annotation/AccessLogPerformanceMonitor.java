package com.fream.back.domain.accessLog.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 성능 모니터링을 위한 AOP 어노테이션
 * 메서드 실행 시간과 시스템 리소스 사용량을 측정
 *
 * @Around 어드바이스와 연동하여 메서드 실행 전후의 성능 지표를 수집하고
 * 임계값을 초과하는 경우 경고를 발생시킵니다.
 *
 * 수집되는 성능 지표:
 * - 메서드 실행 시간 (밀리초)
 * - 메모리 사용량 변화 (선택적)
 * - 메서드별 호출 횟수 및 평균 실행 시간
 *
 * 사용 예시:
 * @AccessLogPerformanceMonitor(
 *     thresholdMs = 500L,
 *     measureMemory = true,
 *     collectMetrics = true
 * )
 * public List<Data> heavyProcessingMethod() { ... }
 */
@Target(ElementType.METHOD) // 메서드에만 적용
@Retention(RetentionPolicy.RUNTIME) // 런타임 유지 (AOP에서 접근 필요)
public @interface AccessLogPerformanceMonitor {

    /**
     * 성능 임계값 (밀리초)
     * 이 값을 초과하면 WARN 레벨로 성능 경고 로그 출력
     *
     * 권장 임계값:
     * - 단순 조회: 100-300ms
     * - 복잡한 조회: 500-1000ms
     * - 데이터 저장: 200-500ms
     * - 배치 처리: 2000-5000ms
     * - 외부 API 호출: 1000-3000ms
     *
     * @return 임계값 (기본값: 1000ms)
     */
    long thresholdMs() default 1000L;

    /**
     * 메모리 사용량 측정 여부
     * JVM의 힙 메모리 사용량 변화를 측정
     *
     * true: 메서드 실행 전후 메모리 사용량을 측정하여 로깅
     *       (가비지 컬렉션을 강제 실행하여 정확한 측정 시도)
     * false: 메모리 사용량 측정하지 않음 (성능상 유리)
     *
     * 주의사항:
     * - 메모리 측정 시 System.gc() 호출로 인한 성능 영향 있음
     * - 메모리 집약적인 작업이나 메모리 누수 의심 시에만 활성화 권장
     *
     * @return 메모리 측정 여부 (기본값: false)
     */
    boolean measureMemory() default false;

    /**
     * 메트릭 수집 여부 (향후 모니터링 시스템 연동용)
     * 메서드별 성능 통계를 내부 저장소에 누적
     *
     * true: 메서드별 실행 횟수, 총 실행 시간, 평균 실행 시간 수집
     *       100번 실행마다 통계 정보를 로그로 출력
     * false: 단순히 개별 실행 시간만 로깅
     *
     * 수집되는 메트릭:
     * - 총 실행 횟수
     * - 총 실행 시간
     * - 평균 실행 시간
     * - 최근 실행 시간 추이
     *
     * @return 메트릭 수집 여부 (기본값: true)
     */
    boolean collectMetrics() default true;

    /**
     * 성능 로그 출력 여부
     * 개별 메서드 실행마다 성능 정보 로깅 제어
     *
     * true: 매번 실행 시간을 INFO 레벨로 로깅
     * false: 임계값 초과 시에만 WARN 레벨로 로깅
     *
     * @return 성능 로그 출력 여부 (기본값: true)
     */
    boolean logPerformance() default true;

    /**
     * 슬로우 쿼리 임계값 (밀리초)
     * 이 값을 초과하면 슬로우 쿼리로 분류하여 별도 처리
     *
     * 일반 임계값(thresholdMs)보다 높은 값으로 설정
     * 슬로우 쿼리는 더 상세한 정보와 함께 로깅됨
     *
     * @return 슬로우 쿼리 임계값 (기본값: 5000ms)
     */
    long slowQueryThresholdMs() default 5000L;

    /**
     * 성능 등급 분류를 위한 구간 설정
     * 실행 시간에 따른 성능 등급 자동 분류
     *
     * EXCELLENT: thresholdMs의 25% 이하
     * GOOD: thresholdMs의 50% 이하
     * FAIR: thresholdMs의 75% 이하
     * POOR: thresholdMs 이하
     * CRITICAL: thresholdMs 초과
     *
     * @return 성능 등급 분류 사용 여부 (기본값: false)
     */
    boolean enablePerformanceGrading() default false;
}