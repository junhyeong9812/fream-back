package com.fream.back.domain.accessLog.aop.aspect;

import com.fream.back.domain.accessLog.aop.annotation.AccessLogPerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 접근 로그 도메인의 성능 모니터링을 전담하는 AOP Aspect
 *
 * 주요 기능:
 * - 메서드 실행 시간 정밀 측정
 * - 메모리 사용량 변화 추적
 * - 성능 임계값 기반 경고 시스템
 * - 메서드별 성능 통계 수집
 * - 슬로우 쿼리 감지 및 알림
 * - 성능 등급 자동 분류
 *
 * Order(3): 로깅 다음 순서로 실행
 * 성능 측정이 가장 내부에서 이루어져야 정확한 측정 가능
 */
@Aspect // AspectJ Aspect 클래스 선언
@Component // Spring Bean으로 등록
@Order(3) // 로깅 Aspect 다음 순서
@Slf4j // Lombok 로거 자동 생성
public class AccessLogPerformanceAspect {

    // 성능 통계를 위한 Thread-Safe 컬렉션들
    private final ConcurrentHashMap<String, AtomicLong> methodExecutionCount = new ConcurrentHashMap<>(); // 메서드별 실행 횟수
    private final ConcurrentHashMap<String, AtomicLong> methodTotalExecutionTime = new ConcurrentHashMap<>(); // 메서드별 총 실행 시간
    private final ConcurrentHashMap<String, AtomicLong> methodMaxExecutionTime = new ConcurrentHashMap<>(); // 메서드별 최대 실행 시간
    private final ConcurrentHashMap<String, AtomicLong> methodMinExecutionTime = new ConcurrentHashMap<>(); // 메서드별 최소 실행 시간
    private final ConcurrentHashMap<String, AtomicLong> slowQueryCount = new ConcurrentHashMap<>(); // 메서드별 슬로우 쿼리 횟수

    // 전역 성능 통계
    private final AtomicLong totalPerformanceChecks = new AtomicLong(0); // 총 성능 체크 횟수
    private final AtomicLong totalThresholdExceeded = new AtomicLong(0); // 임계값 초과 횟수
    private final AtomicLong totalSlowQueries = new AtomicLong(0); // 총 슬로우 쿼리 횟수
    private final AtomicLong totalMemoryMeasurements = new AtomicLong(0); // 총 메모리 측정 횟수

    // 시간 포맷터 (로깅용)
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * @AccessLogPerformanceMonitor 어노테이션이 적용된 메서드의 성능 모니터링
     *
     * @Around 어드바이스: 메서드 실행 전후를 모두 제어하여 성능 지표 수집
     * 메서드 실행 시간, 메모리 사용량, 성능 등급 등을 종합적으로 모니터링
     *
     * @param joinPoint 실행될 메서드의 정보와 제어권
     * @param monitor 성능 모니터링 어노테이션 설정
     * @return 원래 메서드의 반환값
     * @throws Throwable 원래 메서드에서 발생할 수 있는 모든 예외
     */
    @Around("@annotation(monitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, AccessLogPerformanceMonitor monitor) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString(); // 메서드 시그니처
        String className = joinPoint.getTarget().getClass().getSimpleName(); // 클래스명
        LocalDateTime startTime = LocalDateTime.now(); // 시작 시간

        // 성능 체크 횟수 증가
        totalPerformanceChecks.incrementAndGet();
        methodExecutionCount.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();

        // 실행 시간 측정을 위한 고정밀 StopWatch 사용
        StopWatch stopWatch = new StopWatch(methodName);

        // 메모리 측정 변수들
        long beforeMemory = 0;
        long afterMemory = 0;
        boolean memoryMeasured = false;

        // 메모리 측정이 활성화된 경우 실행 전 메모리 상태 측정
        if (monitor.measureMemory()) {
            beforeMemory = measureCurrentMemoryUsage();
            memoryMeasured = true;
            totalMemoryMeasurements.incrementAndGet();
        }

        // 성능 측정 시작
        stopWatch.start();

        try {
            // 실제 메서드 실행
            Object result = joinPoint.proceed();

            // 성능 측정 종료
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            // 메모리 측정 완료
            if (memoryMeasured) {
                afterMemory = measureCurrentMemoryUsage();
            }

            // 성공적인 실행에 대한 성능 분석 및 로깅
            analyzeAndLogPerformance(monitor, methodName, className, executionTime,
                    beforeMemory, afterMemory, memoryMeasured, startTime, false);

            return result; // 정상 결과 반환

        } catch (Throwable throwable) {
            // 예외 발생 시에도 성능 측정 완료
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            // 메모리 측정 완료 (예외 상황에서도)
            if (memoryMeasured) {
                afterMemory = measureCurrentMemoryUsage();
            }

            // 예외 발생 실행에 대한 성능 분석 및 로깅
            analyzeAndLogPerformance(monitor, methodName, className, executionTime,
                    beforeMemory, afterMemory, memoryMeasured, startTime, true);

            // 예외를 다시 던져서 상위로 전파
            throw throwable;
        }
    }

    /**
     * 성능 분석 및 로깅 수행
     *
     * @param monitor 성능 모니터링 설정
     * @param methodName 메서드명
     * @param className 클래스명
     * @param executionTime 실행 시간
     * @param beforeMemory 실행 전 메모리 사용량
     * @param afterMemory 실행 후 메모리 사용량
     * @param memoryMeasured 메모리 측정 여부
     * @param startTime 시작 시간
     * @param hasException 예외 발생 여부
     */
    private void analyzeAndLogPerformance(AccessLogPerformanceMonitor monitor, String methodName, String className,
                                          long executionTime, long beforeMemory, long afterMemory, boolean memoryMeasured,
                                          LocalDateTime startTime, boolean hasException) {

        // 1. 메서드별 실행 시간 통계 업데이트
        updateExecutionTimeStatistics(methodName, executionTime);

        // 2. 성능 등급 계산 (활성화된 경우)
        PerformanceGrade grade = null;
        if (monitor.enablePerformanceGrading()) {
            grade = calculatePerformanceGrade(executionTime, monitor.thresholdMs());
        }

        // 3. 슬로우 쿼리 감지
        boolean isSlowQuery = executionTime > monitor.slowQueryThresholdMs();
        if (isSlowQuery) {
            totalSlowQueries.incrementAndGet();
            slowQueryCount.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();
        }

        // 4. 임계값 초과 감지
        boolean thresholdExceeded = executionTime > monitor.thresholdMs();
        if (thresholdExceeded) {
            totalThresholdExceeded.incrementAndGet();
        }

        // 5. 로깅 수행
        if (monitor.logPerformance() || thresholdExceeded || isSlowQuery) {
            logPerformanceResults(monitor, methodName, className, executionTime, beforeMemory, afterMemory,
                    memoryMeasured, startTime, hasException, grade, isSlowQuery, thresholdExceeded);
        }

        // 6. 메트릭 수집 (활성화된 경우)
        if (monitor.collectMetrics()) {
            collectPerformanceMetrics(methodName, executionTime);
        }
    }

    /**
     * 현재 메모리 사용량 측정
     *
     * @return 현재 힙 메모리 사용량 (바이트)
     */
    private long measureCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime(); // JVM 런타임 정보 접근

        // 가비지 컬렉션 실행으로 정확한 메모리 측정 시도
        // 주의: 성능에 영향을 줄 수 있으므로 필요시에만 사용
        runtime.gc();

        // 총 메모리에서 여유 메모리를 뺀 값이 현재 사용 중인 메모리
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 메서드별 실행 시간 통계 업데이트
     *
     * @param methodName 메서드명
     * @param executionTime 실행 시간
     */
    private void updateExecutionTimeStatistics(String methodName, long executionTime) {
        // 총 실행 시간 누적
        methodTotalExecutionTime.computeIfAbsent(methodName, k -> new AtomicLong(0))
                .addAndGet(executionTime);

        // 최대 실행 시간 업데이트
        methodMaxExecutionTime.compute(methodName, (k, v) -> {
            if (v == null) {
                return new AtomicLong(executionTime);
            } else {
                long currentMax = v.get();
                if (executionTime > currentMax) {
                    v.set(executionTime);
                }
                return v;
            }
        });

        // 최소 실행 시간 업데이트
        methodMinExecutionTime.compute(methodName, (k, v) -> {
            if (v == null) {
                return new AtomicLong(executionTime);
            } else {
                long currentMin = v.get();
                if (executionTime < currentMin) {
                    v.set(executionTime);
                }
                return v;
            }
        });
    }

    /**
     * 실행 시간 기반 성능 등급 계산
     *
     * @param executionTime 실행 시간
     * @param thresholdMs 기준 임계값
     * @return 성능 등급
     */
    private PerformanceGrade calculatePerformanceGrade(long executionTime, long thresholdMs) {
        if (executionTime > thresholdMs) {
            return PerformanceGrade.CRITICAL; // 임계값 초과
        } else if (executionTime > thresholdMs * 0.75) {
            return PerformanceGrade.POOR; // 임계값의 75% 초과
        } else if (executionTime > thresholdMs * 0.5) {
            return PerformanceGrade.FAIR; // 임계값의 50% 초과
        } else if (executionTime > thresholdMs * 0.25) {
            return PerformanceGrade.GOOD; // 임계값의 25% 초과
        } else {
            return PerformanceGrade.EXCELLENT; // 임계값의 25% 이하
        }
    }

    /**
     * 성능 결과 로깅
     *
     * @param monitor 모니터링 설정
     * @param methodName 메서드명
     * @param className 클래스명
     * @param executionTime 실행 시간
     * @param beforeMemory 실행 전 메모리
     * @param afterMemory 실행 후 메모리
     * @param memoryMeasured 메모리 측정 여부
     * @param startTime 시작 시간
     * @param hasException 예외 발생 여부
     * @param grade 성능 등급
     * @param isSlowQuery 슬로우 쿼리 여부
     * @param thresholdExceeded 임계값 초과 여부
     */
    private void logPerformanceResults(AccessLogPerformanceMonitor monitor, String methodName, String className,
                                       long executionTime, long beforeMemory, long afterMemory, boolean memoryMeasured,
                                       LocalDateTime startTime, boolean hasException, PerformanceGrade grade,
                                       boolean isSlowQuery, boolean thresholdExceeded) {

        // 기본 성능 정보 구성
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[성능] ").append(methodName);
        logMessage.append(" | 실행시간: ").append(executionTime).append("ms");
        logMessage.append(" | 시작: ").append(startTime.format(TIME_FORMATTER));

        // 상태 정보 추가
        if (hasException) {
            logMessage.append(" | 상태: 예외발생");
        } else {
            logMessage.append(" | 상태: 정상완료");
        }

        // 메모리 정보 추가 (측정된 경우)
        if (memoryMeasured) {
            long memoryDiff = afterMemory - beforeMemory;
            logMessage.append(" | 메모리변화: ");
            if (memoryDiff >= 0) {
                logMessage.append("+").append(formatBytes(memoryDiff));
            } else {
                logMessage.append(formatBytes(memoryDiff));
            }
            logMessage.append(" (").append(formatBytes(afterMemory)).append(")");
        }

        // 성능 등급 정보 추가 (활성화된 경우)
        if (grade != null) {
            logMessage.append(" | 등급: ").append(grade.name());
        }

        // 로그 레벨 결정 및 출력
        if (isSlowQuery) {
            // 슬로우 쿼리는 ERROR 레벨
            log.error("🐌 [SLOW QUERY] {}", logMessage.toString());
            logSlowQueryDetails(methodName, executionTime, monitor.slowQueryThresholdMs());
        } else if (thresholdExceeded) {
            // 임계값 초과는 WARN 레벨
            log.warn("⚠️ [성능 경고] {} | 임계값({}ms) 초과", logMessage.toString(), monitor.thresholdMs());
        } else {
            // 정상 범위는 INFO 레벨
            log.info("✅ {}", logMessage.toString());
        }
    }

    /**
     * 슬로우 쿼리 상세 정보 로깅
     *
     * @param methodName 메서드명
     * @param executionTime 실행 시간
     * @param slowThreshold 슬로우 쿼리 임계값
     */
    private void logSlowQueryDetails(String methodName, long executionTime, long slowThreshold) {
        long currentSlowCount = slowQueryCount.getOrDefault(methodName, new AtomicLong(0)).get();
        double slowRatio = executionTime / (double) slowThreshold;

        log.error("[슬로우 쿼리 상세] {} | 임계값 대비: {:.2f}배 | 누적 슬로우 쿼리: {}회",
                methodName, slowRatio, currentSlowCount);
    }

    /**
     * 성능 메트릭 수집
     *
     * @param methodName 메서드명
     * @param executionTime 실행 시간
     */
    private void collectPerformanceMetrics(String methodName, long executionTime) {
        // 현재 통계 정보 계산
        long totalCount = methodExecutionCount.get(methodName).get();
        long totalTime = methodTotalExecutionTime.get(methodName).get();
        long avgTime = totalTime / totalCount;
        long maxTime = methodMaxExecutionTime.getOrDefault(methodName, new AtomicLong(0)).get();
        long minTime = methodMinExecutionTime.getOrDefault(methodName, new AtomicLong(0)).get();

        // 주기적으로 통계 정보 로깅 (100번 실행마다)
        if (totalCount % 100 == 0) {
            log.info("📊 [메트릭] {} | 호출: {}회 | 평균: {}ms | 최대: {}ms | 최소: {}ms",
                    methodName, totalCount, avgTime, maxTime, minTime);
        }

        // 성능 이상 감지 (평균의 3배를 초과하는 경우)
        if (avgTime > 0 && executionTime > avgTime * 3) {
            log.warn("🔍 [성능 이상] {} | 현재: {}ms | 평균: {}ms | 평균 대비: {:.1f}배",
                    methodName, executionTime, avgTime, (double) executionTime / avgTime);
        }
    }

    /**
     * 바이트 단위를 읽기 쉬운 형태로 포맷팅
     *
     * @param bytes 바이트 수
     * @return 포맷팅된 문자열
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 전체 성능 통계 정보 조회
     * 모니터링 대시보드나 관리 도구에서 사용
     *
     * @return 성능 통계 정보
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
                totalPerformanceChecks.get(),
                totalThresholdExceeded.get(),
                totalSlowQueries.get(),
                totalMemoryMeasurements.get(),
                methodExecutionCount.size()
        );
    }

    /**
     * 특정 메서드의 성능 통계 조회
     *
     * @param methodName 조회할 메서드명
     * @return 메서드별 성능 통계
     */
    public MethodPerformanceStats getMethodStats(String methodName) {
        long count = methodExecutionCount.getOrDefault(methodName, new AtomicLong(0)).get();
        long totalTime = methodTotalExecutionTime.getOrDefault(methodName, new AtomicLong(0)).get();
        long maxTime = methodMaxExecutionTime.getOrDefault(methodName, new AtomicLong(0)).get();
        long minTime = methodMinExecutionTime.getOrDefault(methodName, new AtomicLong(0)).get();
        long slowCount = slowQueryCount.getOrDefault(methodName, new AtomicLong(0)).get();

        return new MethodPerformanceStats(count, totalTime, maxTime, minTime, slowCount);
    }

    /**
     * 성능 등급 열거형
     */
    public enum PerformanceGrade {
        EXCELLENT("우수"),    // 임계값의 25% 이하
        GOOD("양호"),         // 임계값의 50% 이하
        FAIR("보통"),         // 임계값의 75% 이하
        POOR("나쁨"),         // 임계값 이하
        CRITICAL("심각");     // 임계값 초과

        private final String description;

        PerformanceGrade(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 전체 성능 통계 정보 클래스
     */
    public static class PerformanceStats {
        private final long totalChecks;           // 총 성능 체크 횟수
        private final long thresholdExceeded;     // 임계값 초과 횟수
        private final long slowQueries;           // 슬로우 쿼리 횟수
        private final long memoryMeasurements;    // 메모리 측정 횟수
        private final int uniqueMethods;          // 고유 메서드 수

        public PerformanceStats(long totalChecks, long thresholdExceeded, long slowQueries,
                                long memoryMeasurements, int uniqueMethods) {
            this.totalChecks = totalChecks;
            this.thresholdExceeded = thresholdExceeded;
            this.slowQueries = slowQueries;
            this.memoryMeasurements = memoryMeasurements;
            this.uniqueMethods = uniqueMethods;
        }

        // Getter 메서드들
        public long getTotalChecks() { return totalChecks; }
        public long getThresholdExceeded() { return thresholdExceeded; }
        public long getSlowQueries() { return slowQueries; }
        public long getMemoryMeasurements() { return memoryMeasurements; }
        public int getUniqueMethods() { return uniqueMethods; }

        public double getThresholdExceededRate() {
            return totalChecks > 0 ? (double) thresholdExceeded / totalChecks * 100 : 0;
        }

        public double getSlowQueryRate() {
            return totalChecks > 0 ? (double) slowQueries / totalChecks * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("PerformanceStats{checks=%d, exceeded=%d(%.2f%%), slow=%d(%.2f%%), memory=%d, methods=%d}",
                    totalChecks, thresholdExceeded, getThresholdExceededRate(),
                    slowQueries, getSlowQueryRate(), memoryMeasurements, uniqueMethods);
        }
    }

    /**
     * 메서드별 성능 통계 정보 클래스
     */
    public static class MethodPerformanceStats {
        private final long executionCount;   // 실행 횟수
        private final long totalTime;        // 총 실행 시간
        private final long maxTime;          // 최대 실행 시간
        private final long minTime;          // 최소 실행 시간
        private final long slowQueryCount;   // 슬로우 쿼리 횟수

        public MethodPerformanceStats(long executionCount, long totalTime, long maxTime,
                                      long minTime, long slowQueryCount) {
            this.executionCount = executionCount;
            this.totalTime = totalTime;
            this.maxTime = maxTime;
            this.minTime = minTime;
            this.slowQueryCount = slowQueryCount;
        }

        // Getter 메서드들
        public long getExecutionCount() { return executionCount; }
        public long getTotalTime() { return totalTime; }
        public long getMaxTime() { return maxTime; }
        public long getMinTime() { return minTime; }
        public long getSlowQueryCount() { return slowQueryCount; }

        public long getAverageTime() {
            return executionCount > 0 ? totalTime / executionCount : 0;
        }

        public double getSlowQueryRate() {
            return executionCount > 0 ? (double) slowQueryCount / executionCount * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("MethodStats{count=%d, avg=%dms, max=%dms, min=%dms, slow=%d(%.2f%%)}",
                    executionCount, getAverageTime(), maxTime, minTime, slowQueryCount, getSlowQueryRate());
        }
    }
}