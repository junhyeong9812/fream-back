package com.fream.back.domain.address.aop;

import com.fream.back.domain.address.aop.annotation.AddressPerformance;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Address 도메인 성능 모니터링 AOP
 * @AddressPerformance 어노테이션을 기반으로 성능 모니터링 수행
 */
@Aspect
@Component
@Slf4j
public class AddressPerformanceAspect {

    // 성능 임계값 상수 (밀리초)
    private static final long CONTROLLER_WARNING_THRESHOLD = 3000L;  // 3초
    private static final long CONTROLLER_ERROR_THRESHOLD = 5000L;    // 5초
    private static final long SERVICE_WARNING_THRESHOLD = 2000L;     // 2초
    private static final long SERVICE_ERROR_THRESHOLD = 3000L;       // 3초
    private static final long REPOSITORY_WARNING_THRESHOLD = 1000L;  // 1초
    private static final long REPOSITORY_ERROR_THRESHOLD = 2000L;    // 2초

    // 성능 통계 저장소
    private final ConcurrentHashMap<String, MethodPerformanceStats> performanceStats = new ConcurrentHashMap<>();

    // 동시성 모니터링을 위한 카운터
    private final ConcurrentHashMap<String, AtomicLong> concurrencyCounters = new ConcurrentHashMap<>();

    /**
     * @AddressPerformance 어노테이션이 붙은 메서드의 성능 모니터링
     *
     * @param proceedingJoinPoint 조인포인트
     * @param addressPerformance 성능 모니터링 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("@annotation(addressPerformance)")
    public Object monitorAnnotatedPerformance(ProceedingJoinPoint proceedingJoinPoint,
                                              AddressPerformance addressPerformance) throws Throwable {
        if (!addressPerformance.enabled()) {
            return proceedingJoinPoint.proceed();
        }

        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        String userEmail = extractUserEmailSafely();

        // 어노테이션에서 임계값 설정 확인
        long warningThreshold = addressPerformance.warningThreshold() != -1
                ? addressPerformance.warningThreshold()
                : getDefaultWarningThreshold(className);
        long errorThreshold = addressPerformance.errorThreshold() != -1
                ? addressPerformance.errorThreshold()
                : getDefaultErrorThreshold(className);

        return executePerformanceMonitoring(proceedingJoinPoint, addressPerformance,
                fullMethodName, userEmail, warningThreshold, errorThreshold);
    }

    /**
     * Address Controller 메서드 성능 모니터링
     */
    @Around("execution(* com.fream.back.domain.address.controller..*(..))")
    public Object monitorControllerPerformance(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return monitorMethodPerformance(
                proceedingJoinPoint,
                "CONTROLLER",
                CONTROLLER_WARNING_THRESHOLD,
                CONTROLLER_ERROR_THRESHOLD
        );
    }

    /**
     * Address Service 메서드 성능 모니터링
     */
    @Around("execution(* com.fream.back.domain.address.service..*(..))")
    public Object monitorServicePerformance(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return monitorMethodPerformance(
                proceedingJoinPoint,
                "SERVICE",
                SERVICE_WARNING_THRESHOLD,
                SERVICE_ERROR_THRESHOLD
        );
    }

    /**
     * Address Repository 메서드 성능 모니터링
     */
    @Around("execution(* com.fream.back.domain.address.repository..*(..))")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return monitorMethodPerformance(
                proceedingJoinPoint,
                "REPOSITORY",
                REPOSITORY_WARNING_THRESHOLD,
                REPOSITORY_ERROR_THRESHOLD
        );
    }

    /**
     * 어노테이션 기반 성능 모니터링 실행
     */
    private Object executePerformanceMonitoring(ProceedingJoinPoint proceedingJoinPoint,
                                                AddressPerformance addressPerformance,
                                                String fullMethodName, String userEmail,
                                                long warningThreshold, long errorThreshold) throws Throwable {

        // 동시성 모니터링
        AtomicLong concurrencyCounter = null;
        if (addressPerformance.monitorConcurrency()) {
            concurrencyCounter = concurrencyCounters.computeIfAbsent(fullMethodName, k -> new AtomicLong(0));
            concurrencyCounter.incrementAndGet();
        }

        // 메모리 및 CPU 모니터링 준비
        MemoryMXBean memoryBean = addressPerformance.monitorMemory() ? ManagementFactory.getMemoryMXBean() : null;
        ThreadMXBean threadBean = addressPerformance.monitorCpu() ? ManagementFactory.getThreadMXBean() : null;

        long startMemory = memoryBean != null ? memoryBean.getHeapMemoryUsage().getUsed() : 0;
        long startCpuTime = threadBean != null ? threadBean.getCurrentThreadCpuTime() : 0;

        long startTime = System.currentTimeMillis();
        long startNanoTime = System.nanoTime();

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long executionTime = endTime - startTime;
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 추가 메트릭 수집
            long endMemory = memoryBean != null ? memoryBean.getHeapMemoryUsage().getUsed() : 0;
            long endCpuTime = threadBean != null ? threadBean.getCurrentThreadCpuTime() : 0;
            long memoryUsed = endMemory - startMemory;
            long cpuTimeUsed = endCpuTime - startCpuTime;

            // 성능 통계 업데이트
            if (addressPerformance.collectStats()) {
                updatePerformanceStats(fullMethodName, preciseExecutionTime, true);
            }

            // 성능 로깅
            logAdvancedPerformanceMetrics(addressPerformance, fullMethodName, userEmail, preciseExecutionTime,
                    warningThreshold, errorThreshold, memoryUsed, cpuTimeUsed,
                    concurrencyCounter != null ? concurrencyCounter.get() : 0, true);

            // 주기적 성능 리포트
            if (addressPerformance.reportInterval() > 0 &&
                    shouldGeneratePerformanceReport(fullMethodName, addressPerformance.reportInterval())) {
                generatePerformanceReport(fullMethodName);
            }

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long executionTime = endTime - startTime;
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 실패한 경우도 성능 통계에 반영
            if (addressPerformance.collectStats()) {
                updatePerformanceStats(fullMethodName, preciseExecutionTime, false);
            }

            logAdvancedPerformanceMetrics(addressPerformance, fullMethodName, userEmail, preciseExecutionTime,
                    warningThreshold, errorThreshold, 0, 0,
                    concurrencyCounter != null ? concurrencyCounter.get() : 0, false);

            throw e;
        } finally {
            // 동시성 카운터 감소
            if (concurrencyCounter != null) {
                concurrencyCounter.decrementAndGet();
            }
        }
    }

    /**
     * 메서드 성능 모니터링 공통 로직
     */
    private Object monitorMethodPerformance(
            ProceedingJoinPoint proceedingJoinPoint,
            String layer,
            long warningThreshold,
            long errorThreshold) throws Throwable {

        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        String userEmail = extractUserEmailSafely();

        long startTime = System.currentTimeMillis();
        long startNanoTime = System.nanoTime();

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long executionTime = endTime - startTime;
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 성능 통계 업데이트
            updatePerformanceStats(fullMethodName, preciseExecutionTime, true);

            // 성능 임계값 검사 및 로깅
            logPerformanceMetrics(layer, className, methodName, userEmail, preciseExecutionTime,
                    warningThreshold, errorThreshold, true);

            // 주기적 성능 리포트
            if (shouldGeneratePerformanceReport(fullMethodName, 100)) {
                generatePerformanceReport(fullMethodName);
            }

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long executionTime = endTime - startTime;
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 실패한 경우도 성능 통계에 반영
            updatePerformanceStats(fullMethodName, preciseExecutionTime, false);

            logPerformanceMetrics(layer, className, methodName, userEmail, preciseExecutionTime,
                    warningThreshold, errorThreshold, false);

            throw e;
        }
    }

    /**
     * 고급 성능 메트릭스 로깅
     */
    private void logAdvancedPerformanceMetrics(AddressPerformance addressPerformance, String fullMethodName,
                                               String userEmail, long executionTime,
                                               long warningThreshold, long errorThreshold,
                                               long memoryUsed, long cpuTimeUsed, long concurrentCount, boolean success) {

        String status = success ? "SUCCESS" : "FAILED";
        String priority = addressPerformance.priority().toString();
        String metricName = addressPerformance.metricName().isEmpty() ? fullMethodName : addressPerformance.metricName();

        if (executionTime >= errorThreshold) {
            log.error("PERFORMANCE_CRITICAL - Address Method - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Memory: {}KB, CPU: {}ns, Concurrent: {}",
                    metricName, userEmail, executionTime, priority, status,
                    memoryUsed / 1024, cpuTimeUsed, concurrentCount);
        } else if (executionTime >= warningThreshold) {
            log.warn("PERFORMANCE_WARNING - Address Method - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Memory: {}KB, CPU: {}ns, Concurrent: {}",
                    metricName, userEmail, executionTime, priority, status,
                    memoryUsed / 1024, cpuTimeUsed, concurrentCount);
        } else if (addressPerformance.priority() == AddressPerformance.Priority.HIGH ||
                addressPerformance.priority() == AddressPerformance.Priority.CRITICAL) {
            log.info("PERFORMANCE_NORMAL - Address Method - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Memory: {}KB, CPU: {}ns, Concurrent: {}",
                    metricName, userEmail, executionTime, priority, status,
                    memoryUsed / 1024, cpuTimeUsed, concurrentCount);
        } else {
            log.debug("PERFORMANCE_NORMAL - Address Method - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Memory: {}KB, CPU: {}ns, Concurrent: {}",
                    metricName, userEmail, executionTime, priority, status,
                    memoryUsed / 1024, cpuTimeUsed, concurrentCount);
        }

        // 메트릭스 시스템 연동을 위한 구조화된 로깅
        StringBuilder metricsLog = new StringBuilder();
        metricsLog.append(String.format("METRICS: performance.address.%s execution_time=%d success=%b user=%s",
                metricName.toLowerCase(), executionTime, success, userEmail));

        if (addressPerformance.tags().length > 0) {
            metricsLog.append(" tags=").append(Arrays.toString(addressPerformance.tags()));
        }

        if (addressPerformance.monitorMemory()) {
            metricsLog.append(" memory_used=").append(memoryUsed);
        }

        if (addressPerformance.monitorCpu()) {
            metricsLog.append(" cpu_time=").append(cpuTimeUsed);
        }

        if (addressPerformance.monitorConcurrency()) {
            metricsLog.append(" concurrent_count=").append(concurrentCount);
        }

        log.info(metricsLog.toString());
    }

    /**
     * 성능 메트릭스 로깅
     */
    private void logPerformanceMetrics(String layer, String className, String methodName,
                                       String userEmail, long executionTime,
                                       long warningThreshold, long errorThreshold, boolean success) {

        String status = success ? "SUCCESS" : "FAILED";

        if (executionTime >= errorThreshold) {
            log.error("PERFORMANCE_CRITICAL - Address {} - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Status: {}",
                    layer, className, methodName, userEmail, executionTime, status);
        } else if (executionTime >= warningThreshold) {
            log.warn("PERFORMANCE_WARNING - Address {} - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Status: {}",
                    layer, className, methodName, userEmail, executionTime, status);
        } else {
            log.debug("PERFORMANCE_NORMAL - Address {} - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Status: {}",
                    layer, className, methodName, userEmail, executionTime, status);
        }

        // 메트릭스 시스템 연동을 위한 구조화된 로깅
        log.info("METRICS: performance.address.{}.{}.{} execution_time={} success={} user={}",
                layer.toLowerCase(), className.toLowerCase(), methodName.toLowerCase(),
                executionTime, success, userEmail);
    }

    /**
     * 성능 통계 업데이트
     */
    private void updatePerformanceStats(String methodName, long executionTime, boolean success) {
        performanceStats.computeIfAbsent(methodName, k -> new MethodPerformanceStats())
                .updateStats(executionTime, success);
    }

    /**
     * 성능 리포트 생성 여부 확인
     */
    private boolean shouldGeneratePerformanceReport(String methodName, int interval) {
        MethodPerformanceStats stats = performanceStats.get(methodName);
        if (stats == null || interval <= 0) {
            return false;
        }

        return stats.getTotalCalls() % interval == 0;
    }

    /**
     * 성능 리포트 생성
     */
    private void generatePerformanceReport(String methodName) {
        MethodPerformanceStats stats = performanceStats.get(methodName);
        if (stats == null) {
            return;
        }

        log.info("PERFORMANCE_REPORT - Method: {}, TotalCalls: {}, SuccessRate: {:.2f}%, " +
                        "AvgExecutionTime: {:.2f}ms, MinExecutionTime: {}ms, MaxExecutionTime: {}ms, " +
                        "TotalExecutionTime: {}ms",
                methodName,
                stats.getTotalCalls(),
                stats.getSuccessRate(),
                stats.getAverageExecutionTime(),
                stats.getMinExecutionTime(),
                stats.getMaxExecutionTime(),
                stats.getTotalExecutionTime());
    }

    /**
     * 레이어별 기본 경고 임계값 반환
     */
    private long getDefaultWarningThreshold(String className) {
        if (className.toLowerCase().contains("controller")) {
            return CONTROLLER_WARNING_THRESHOLD;
        } else if (className.toLowerCase().contains("service")) {
            return SERVICE_WARNING_THRESHOLD;
        } else if (className.toLowerCase().contains("repository")) {
            return REPOSITORY_WARNING_THRESHOLD;
        }
        return SERVICE_WARNING_THRESHOLD; // 기본값
    }

    /**
     * 레이어별 기본 오류 임계값 반환
     */
    private long getDefaultErrorThreshold(String className) {
        if (className.toLowerCase().contains("controller")) {
            return CONTROLLER_ERROR_THRESHOLD;
        } else if (className.toLowerCase().contains("service")) {
            return SERVICE_ERROR_THRESHOLD;
        } else if (className.toLowerCase().contains("repository")) {
            return REPOSITORY_ERROR_THRESHOLD;
        }
        return SERVICE_ERROR_THRESHOLD; // 기본값
    }

    /**
     * 안전하게 사용자 이메일 추출
     */
    private String extractUserEmailSafely() {
        try {
            return SecurityUtils.extractEmailOrAnonymous();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    /**
     * 메서드별 성능 통계를 저장하는 내부 클래스
     */
    private static class MethodPerformanceStats {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = Long.MIN_VALUE;

        public synchronized void updateStats(long executionTime, boolean success) {
            totalCalls.incrementAndGet();
            if (success) {
                successCalls.incrementAndGet();
            }

            totalExecutionTime.addAndGet(executionTime);

            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            }

            if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
        }

        public long getTotalCalls() {
            return totalCalls.get();
        }

        public double getSuccessRate() {
            long total = totalCalls.get();
            if (total == 0) return 0.0;
            return (successCalls.get() * 100.0) / total;
        }

        public double getAverageExecutionTime() {
            long total = totalCalls.get();
            if (total == 0) return 0.0;
            return (double) totalExecutionTime.get() / total;
        }

        public long getMinExecutionTime() {
            return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime;
        }

        public long getMaxExecutionTime() {
            return maxExecutionTime == Long.MIN_VALUE ? 0 : maxExecutionTime;
        }

        public long getTotalExecutionTime() {
            return totalExecutionTime.get();
        }
    }
}