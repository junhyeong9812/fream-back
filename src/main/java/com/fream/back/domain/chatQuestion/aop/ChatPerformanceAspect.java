package com.fream.back.domain.chatQuestion.aop;

import com.fream.back.domain.chatQuestion.aop.annotation.ChatPerformance;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
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
 * ChatQuestion 도메인 성능 모니터링 AOP
 * @ChatPerformance 어노테이션을 기반으로 성능 모니터링 수행
 */
@Aspect
@Component
@Slf4j
public class ChatPerformanceAspect {

    // 성능 임계값 상수 (밀리초)
    private static final long CONTROLLER_WARNING_THRESHOLD = 5000L;   // 5초
    private static final long CONTROLLER_ERROR_THRESHOLD = 10000L;    // 10초
    private static final long SERVICE_WARNING_THRESHOLD = 3000L;      // 3초
    private static final long SERVICE_ERROR_THRESHOLD = 8000L;        // 8초 (GPT API 호출 포함)
    private static final long GPT_API_WARNING_THRESHOLD = 5000L;      // 5초
    private static final long GPT_API_ERROR_THRESHOLD = 15000L;       // 15초

    // 성능 통계 저장소
    private final ConcurrentHashMap<String, MethodPerformanceStats> performanceStats = new ConcurrentHashMap<>();

    // 동시성 모니터링을 위한 카운터
    private final ConcurrentHashMap<String, AtomicLong> concurrencyCounters = new ConcurrentHashMap<>();

    // API 호출 빈도 추적
    private final ConcurrentHashMap<String, ApiFrequencyTracker> frequencyTrackers = new ConcurrentHashMap<>();

    /**
     * @ChatPerformance 어노테이션이 붙은 메서드의 성능 모니터링
     */
    @Around("@annotation(chatPerformance)")
    public Object monitorAnnotatedPerformance(ProceedingJoinPoint proceedingJoinPoint,
                                              ChatPerformance chatPerformance) throws Throwable {
        if (!chatPerformance.enabled()) {
            return proceedingJoinPoint.proceed();
        }

        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        String userEmail = extractUserEmailSafely();

        // 어노테이션에서 임계값 설정 확인
        long warningThreshold = chatPerformance.warningThreshold() != -1
                ? chatPerformance.warningThreshold()
                : getDefaultWarningThreshold(className);
        long errorThreshold = chatPerformance.errorThreshold() != -1
                ? chatPerformance.errorThreshold()
                : getDefaultErrorThreshold(className);

        return executeAdvancedPerformanceMonitoring(proceedingJoinPoint, chatPerformance,
                fullMethodName, userEmail, warningThreshold, errorThreshold);
    }

    /**
     * Chat Controller 메서드 성능 모니터링
     */
    @Around("execution(* com.fream.back.domain.chatQuestion.controller..*(..))")
    public Object monitorControllerPerformance(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return monitorMethodPerformance(
                proceedingJoinPoint,
                "CHAT_CONTROLLER",
                CONTROLLER_WARNING_THRESHOLD,
                CONTROLLER_ERROR_THRESHOLD
        );
    }

    /**
     * Chat Service 메서드 성능 모니터링
     */
    @Around("execution(* com.fream.back.domain.chatQuestion.service..*(..))")
    public Object monitorServicePerformance(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();

        // GPT API 호출 메서드는 다른 임계값 적용
        if (methodName.contains("GPT") || methodName.contains("Api")) {
            return monitorMethodPerformance(
                    proceedingJoinPoint,
                    "GPT_API_SERVICE",
                    GPT_API_WARNING_THRESHOLD,
                    GPT_API_ERROR_THRESHOLD
            );
        }

        return monitorMethodPerformance(
                proceedingJoinPoint,
                "CHAT_SERVICE",
                SERVICE_WARNING_THRESHOLD,
                SERVICE_ERROR_THRESHOLD
        );
    }

    /**
     * 고급 성능 모니터링 실행
     */
    private Object executeAdvancedPerformanceMonitoring(ProceedingJoinPoint proceedingJoinPoint,
                                                        ChatPerformance chatPerformance,
                                                        String fullMethodName, String userEmail,
                                                        long warningThreshold, long errorThreshold) throws Throwable {

        // 동시성 모니터링
        AtomicLong concurrencyCounter = null;
        if (chatPerformance.monitorConcurrentRequests()) {
            concurrencyCounter = concurrencyCounters.computeIfAbsent(fullMethodName, k -> new AtomicLong(0));
            long currentConcurrency = concurrencyCounter.incrementAndGet();
            if (currentConcurrency > 10) { // 임계값
                log.warn("HIGH_CONCURRENCY - Method: {}, ConcurrentRequests: {}, User: {}",
                        fullMethodName, currentConcurrency, userEmail);
            }
        }

        // API 호출 빈도 모니터링
        if (chatPerformance.monitorApiFrequency()) {
            ApiFrequencyTracker tracker = frequencyTrackers.computeIfAbsent(
                    userEmail + ":" + fullMethodName, k -> new ApiFrequencyTracker());
            if (tracker.isExceedingLimit()) {
                log.warn("HIGH_API_FREQUENCY - Method: {}, User: {}, CallsPerMinute: {}",
                        fullMethodName, userEmail, tracker.getCallsPerMinute());
            }
            tracker.recordCall();
        }

        // 메모리 및 CPU 모니터링 준비
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        long startMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long startCpuTime = threadBean.getCurrentThreadCpuTime();

        long startTime = System.currentTimeMillis();
        long startNanoTime = System.nanoTime();

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long executionTime = endTime - startTime;
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 추가 메트릭 수집
            long endMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long endCpuTime = threadBean.getCurrentThreadCpuTime();
            long memoryUsed = endMemory - startMemory;
            long cpuTimeUsed = endCpuTime - startCpuTime;

            // 토큰 사용량 분석
            int tokensUsed = extractTokenUsage(result);
            if (tokensUsed > chatPerformance.tokenUsageThreshold()) {
                log.warn("HIGH_TOKEN_USAGE - Method: {}, User: {}, TokensUsed: {}, Threshold: {}",
                        fullMethodName, userEmail, tokensUsed, chatPerformance.tokenUsageThreshold());
            }

            // 응답 품질 평가
            if (chatPerformance.evaluateResponseQuality()) {
                evaluateResponseQuality(result, fullMethodName, userEmail);
            }

            // 성능 통계 업데이트
            if (chatPerformance.collectStats()) {
                updatePerformanceStats(fullMethodName, preciseExecutionTime, tokensUsed, true);
            }

            // 성능 로깅
            logAdvancedPerformanceMetrics(chatPerformance, fullMethodName, userEmail, preciseExecutionTime,
                    warningThreshold, errorThreshold, memoryUsed, cpuTimeUsed,
                    concurrencyCounter != null ? concurrencyCounter.get() : 0,
                    tokensUsed, true);

            // 주기적 성능 리포트
            if (chatPerformance.reportInterval() > 0 &&
                    shouldGeneratePerformanceReport(fullMethodName, chatPerformance.reportInterval())) {
                generatePerformanceReport(fullMethodName);
            }

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 실패한 경우도 성능 통계에 반영
            if (chatPerformance.collectStats()) {
                updatePerformanceStats(fullMethodName, preciseExecutionTime, 0, false);
            }

            logAdvancedPerformanceMetrics(chatPerformance, fullMethodName, userEmail, preciseExecutionTime,
                    warningThreshold, errorThreshold, 0, 0,
                    concurrencyCounter != null ? concurrencyCounter.get() : 0, 0, false);

            throw e;
        } finally {
            // 동시성 카운터 감소
            if (concurrencyCounter != null) {
                concurrencyCounter.decrementAndGet();
            }
        }
    }

    /**
     * 일반 메서드 성능 모니터링
     */
    private Object monitorMethodPerformance(ProceedingJoinPoint proceedingJoinPoint,
                                            String layer, long warningThreshold, long errorThreshold) throws Throwable {

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
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            // 토큰 사용량 추출
            int tokensUsed = extractTokenUsage(result);

            // 성능 통계 업데이트
            updatePerformanceStats(fullMethodName, preciseExecutionTime, tokensUsed, true);

            // 성능 로깅
            logPerformanceMetrics(layer, className, methodName, userEmail, preciseExecutionTime,
                    tokensUsed, warningThreshold, errorThreshold, true);

            // 주기적 성능 리포트
            if (shouldGeneratePerformanceReport(fullMethodName, 50)) {
                generatePerformanceReport(fullMethodName);
            }

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long endNanoTime = System.nanoTime();
            long preciseExecutionTime = (endNanoTime - startNanoTime) / 1_000_000;

            updatePerformanceStats(fullMethodName, preciseExecutionTime, 0, false);

            logPerformanceMetrics(layer, className, methodName, userEmail, preciseExecutionTime,
                    0, warningThreshold, errorThreshold, false);

            throw e;
        }
    }

    /**
     * 토큰 사용량 추출
     */
    private int extractTokenUsage(Object result) {
        if (result instanceof GPTResponseDto) {
            GPTResponseDto gptResponse = (GPTResponseDto) result;
            return gptResponse.getUsage() != null ? gptResponse.getUsage().getTotal_tokens() : 0;
        }
        return 0;
    }

    /**
     * 응답 품질 평가
     */
    private void evaluateResponseQuality(Object result, String fullMethodName, String userEmail) {
        if (result instanceof GPTResponseDto) {
            GPTResponseDto gptResponse = (GPTResponseDto) result;
            String answer = gptResponse.getAnswer();

            // 간단한 품질 평가 지표
            if (answer != null) {
                int answerLength = answer.length();
                boolean hasApology = answer.toLowerCase().contains("죄송") ||
                        answer.toLowerCase().contains("미안") ||
                        answer.toLowerCase().contains("sorry");
                boolean hasHelpfulContent = answerLength > 50 && !hasApology;

                if (!hasHelpfulContent) {
                    log.warn("LOW_RESPONSE_QUALITY - Method: {}, User: {}, AnswerLength: {}, HasApology: {}",
                            fullMethodName, userEmail, answerLength, hasApology);
                }
            }
        }
    }

    /**
     * 고급 성능 메트릭스 로깅
     */
    private void logAdvancedPerformanceMetrics(ChatPerformance chatPerformance, String fullMethodName,
                                               String userEmail, long executionTime,
                                               long warningThreshold, long errorThreshold,
                                               long memoryUsed, long cpuTimeUsed, long concurrentCount,
                                               int tokensUsed, boolean success) {

        String status = success ? "SUCCESS" : "FAILED";
        String priority = chatPerformance.priority().toString();
        String metricName = chatPerformance.metricName().isEmpty() ? fullMethodName : chatPerformance.metricName();

        if (executionTime >= errorThreshold) {
            log.error("CHAT_PERFORMANCE_CRITICAL - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Memory: {}KB, CPU: {}ns, Concurrent: {}, Tokens: {}",
                    metricName, userEmail, executionTime, priority, status,
                    memoryUsed / 1024, cpuTimeUsed, concurrentCount, tokensUsed);
        } else if (executionTime >= warningThreshold) {
            log.warn("CHAT_PERFORMANCE_WARNING - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Memory: {}KB, CPU: {}ns, Concurrent: {}, Tokens: {}",
                    metricName, userEmail, executionTime, priority, status,
                    memoryUsed / 1024, cpuTimeUsed, concurrentCount, tokensUsed);
        } else if (chatPerformance.priority() == ChatPerformance.Priority.HIGH ||
                chatPerformance.priority() == ChatPerformance.Priority.CRITICAL) {
            log.info("CHAT_PERFORMANCE_NORMAL - Method: {}, User: {}, ExecutionTime: {}ms, " +
                            "Priority: {}, Status: {}, Tokens: {}",
                    metricName, userEmail, executionTime, priority, status, tokensUsed);
        }

        // 메트릭스 시스템 연동을 위한 구조화된 로깅
        StringBuilder metricsLog = new StringBuilder();
        metricsLog.append(String.format("METRICS: performance.chat.%s execution_time=%d success=%b user=%s tokens=%d",
                metricName.toLowerCase().replace(".", "_"), executionTime, success, userEmail, tokensUsed));

        if (chatPerformance.tags().length > 0) {
            metricsLog.append(" tags=").append(Arrays.toString(chatPerformance.tags()));
        }

        log.info(metricsLog.toString());
    }

    /**
     * 기본 성능 메트릭스 로깅
     */
    private void logPerformanceMetrics(String layer, String className, String methodName,
                                       String userEmail, long executionTime, int tokensUsed,
                                       long warningThreshold, long errorThreshold, boolean success) {

        String status = success ? "SUCCESS" : "FAILED";

        if (executionTime >= errorThreshold) {
            log.error("CHAT_PERFORMANCE_CRITICAL - {} - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Tokens: {}, Status: {}",
                    layer, className, methodName, userEmail, executionTime, tokensUsed, status);
        } else if (executionTime >= warningThreshold) {
            log.warn("CHAT_PERFORMANCE_WARNING - {} - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Tokens: {}, Status: {}",
                    layer, className, methodName, userEmail, executionTime, tokensUsed, status);
        } else {
            log.debug("CHAT_PERFORMANCE_NORMAL - {} - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Tokens: {}, Status: {}",
                    layer, className, methodName, userEmail, executionTime, tokensUsed, status);
        }

        // 메트릭스 시스템 연동
        log.info("METRICS: performance.chat.{}.{}.{} execution_time={} success={} user={} tokens={}",
                layer.toLowerCase(), className.toLowerCase(), methodName.toLowerCase(),
                executionTime, success, userEmail, tokensUsed);
    }

    /**
     * 성능 통계 업데이트
     */
    private void updatePerformanceStats(String methodName, long executionTime, int tokensUsed, boolean success) {
        performanceStats.computeIfAbsent(methodName, k -> new MethodPerformanceStats())
                .updateStats(executionTime, tokensUsed, success);
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

        log.info("CHAT_PERFORMANCE_REPORT - Method: {}, TotalCalls: {}, SuccessRate: {:.2f}%, " +
                        "AvgExecutionTime: {:.2f}ms, MinExecutionTime: {}ms, MaxExecutionTime: {}ms, " +
                        "TotalTokens: {}, AvgTokens: {:.1f}",
                methodName,
                stats.getTotalCalls(),
                stats.getSuccessRate(),
                stats.getAverageExecutionTime(),
                stats.getMinExecutionTime(),
                stats.getMaxExecutionTime(),
                stats.getTotalTokensUsed(),
                stats.getAverageTokensUsed());
    }

    /**
     * 레이어별 기본 경고 임계값 반환
     */
    private long getDefaultWarningThreshold(String className) {
        if (className.toLowerCase().contains("controller")) {
            return CONTROLLER_WARNING_THRESHOLD;
        } else if (className.toLowerCase().contains("gpt") || className.toLowerCase().contains("api")) {
            return GPT_API_WARNING_THRESHOLD;
        } else if (className.toLowerCase().contains("service")) {
            return SERVICE_WARNING_THRESHOLD;
        }
        return SERVICE_WARNING_THRESHOLD;
    }

    /**
     * 레이어별 기본 오류 임계값 반환
     */
    private long getDefaultErrorThreshold(String className) {
        if (className.toLowerCase().contains("controller")) {
            return CONTROLLER_ERROR_THRESHOLD;
        } else if (className.toLowerCase().contains("gpt") || className.toLowerCase().contains("api")) {
            return GPT_API_ERROR_THRESHOLD;
        } else if (className.toLowerCase().contains("service")) {
            return SERVICE_ERROR_THRESHOLD;
        }
        return SERVICE_ERROR_THRESHOLD;
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
        private final AtomicLong totalTokensUsed = new AtomicLong(0);
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = Long.MIN_VALUE;

        public synchronized void updateStats(long executionTime, int tokensUsed, boolean success) {
            totalCalls.incrementAndGet();
            if (success) {
                successCalls.incrementAndGet();
            }

            totalExecutionTime.addAndGet(executionTime);
            totalTokensUsed.addAndGet(tokensUsed);

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

        public long getTotalTokensUsed() {
            return totalTokensUsed.get();
        }

        public double getAverageTokensUsed() {
            long total = totalCalls.get();
            if (total == 0) return 0.0;
            return (double) totalTokensUsed.get() / total;
        }
    }

    /**
     * API 호출 빈도 추적 클래스
     */
    private static class ApiFrequencyTracker {
        private final AtomicLong callCount = new AtomicLong(0);
        private volatile long lastResetTime = System.currentTimeMillis();

        public void recordCall() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastResetTime > 60000) { // 1분마다 리셋
                callCount.set(0);
                lastResetTime = currentTime;
            }
            callCount.incrementAndGet();
        }

        public long getCallsPerMinute() {
            return callCount.get();
        }

        public boolean isExceedingLimit() {
            return getCallsPerMinute() > 60; // 분당 60회 제한
        }
    }
}