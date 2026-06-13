package com.fream.back.domain.accessLog.aop.aspect;

import com.fream.back.domain.accessLog.aop.annotation.AccessLogMethodLogger;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 접근 로그 도메인의 메서드 로깅을 전담하는 AOP Aspect
 *
 * 주요 기능:
 * - 메서드 실행 전/후 로깅
 * - 파라미터 및 반환값 로깅 (선택적)
 * - 실행 시간 측정
 * - 예외 발생 시 로깅
 * - 메서드별 호출 통계 수집
 *
 * Order(2): 예외 처리 다음 순서로 실행
 * 로깅이 예외 처리 내부에서 이루어져야 정확한 로깅 가능
 */
@Aspect // AspectJ Aspect 클래스 선언
@Component // Spring Bean으로 등록
@Order(2) // 예외 처리 Aspect 다음 순서
@Slf4j // Lombok 로거 자동 생성
public class AccessLogMethodLoggingAspect {

    // 메서드별 통계를 위한 Thread-Safe 컬렉션들
    private final ConcurrentHashMap<String, AtomicLong> methodCallCount = new ConcurrentHashMap<>(); // 호출 횟수
    private final ConcurrentHashMap<String, AtomicLong> methodTotalTime = new ConcurrentHashMap<>(); // 총 실행 시간
    private final ConcurrentHashMap<String, StopWatch> activeStopWatches = new ConcurrentHashMap<>(); // 활성 StopWatch들

    // 로깅 통계
    private final AtomicLong totalMethodCalls = new AtomicLong(0); // 총 메서드 호출 횟수
    private final AtomicLong totalExceptions = new AtomicLong(0); // 총 예외 발생 횟수

    /**
     * @AccessLogMethodLogger 어노테이션이 적용된 메서드 실행 전 처리
     *
     * @Before 어드바이스: 메서드 실행 직전에 호출
     * 메서드 진입 시점을 기록하고 파라미터 정보를 로깅
     *
     * @param joinPoint 실행되는 메서드의 정보를 담고 있는 객체
     * @param methodLogger 메서드에 적용된 어노테이션 객체
     */
    @Before("@annotation(methodLogger)")
    public void logMethodEntry(JoinPoint joinPoint, AccessLogMethodLogger methodLogger) {
        String methodName = joinPoint.getSignature().toShortString(); // 메서드 시그니처 (클래스.메서드명)
        String className = joinPoint.getTarget().getClass().getSimpleName(); // 실행 클래스명

        // 메서드 호출 통계 업데이트
        totalMethodCalls.incrementAndGet();
        methodCallCount.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();

        // 실행 시간 측정 시작 (measureExecutionTime이 true인 경우)
        if (methodLogger.measureExecutionTime()) {
            StopWatch stopWatch = new StopWatch(methodName);
            stopWatch.start();
            activeStopWatches.put(getThreadMethodKey(methodName), stopWatch);
        }

        // 로그 메시지 구성
        String logMessage = buildEntryLogMessage(methodLogger, methodName, joinPoint.getArgs());

        // 설정된 로그 레벨에 따라 로깅
        logByLevel(methodLogger.level(), logMessage);

        // 디버그 레벨에서 상세 정보 추가 로깅
        if (log.isDebugEnabled()) {
            log.debug("[메서드 진입] 클래스: {} | 스레드: {} | 호출 횟수: {}",
                    className, Thread.currentThread().getName(), methodCallCount.get(methodName).get());
        }
    }

    /**
     * @AccessLogMethodLogger 어노테이션이 적용된 메서드 정상 종료 시 처리
     *
     * @AfterReturning 어드바이스: 메서드가 정상적으로 반환값을 리턴했을 때 호출
     * 메서드 종료 시점을 기록하고 반환값 및 실행 시간을 로깅
     *
     * @param joinPoint 실행된 메서드의 정보
     * @param methodLogger 메서드에 적용된 어노테이션
     * @param returnValue 메서드의 반환값
     */
    @AfterReturning(pointcut = "@annotation(methodLogger)", returning = "returnValue")
    public void logMethodExit(JoinPoint joinPoint, AccessLogMethodLogger methodLogger, Object returnValue) {
        String methodName = joinPoint.getSignature().toShortString();
        String threadMethodKey = getThreadMethodKey(methodName);

        // 실행 시간 측정 종료
        long executionTime = 0L;
        if (methodLogger.measureExecutionTime()) {
            StopWatch stopWatch = activeStopWatches.remove(threadMethodKey);
            if (stopWatch != null && stopWatch.isRunning()) {
                stopWatch.stop();
                executionTime = stopWatch.getTotalTimeMillis();

                // 총 실행 시간 통계 업데이트
                methodTotalTime.computeIfAbsent(methodName, k -> new AtomicLong(0))
                        .addAndGet(executionTime);
            }
        }

        // 로그 메시지 구성
        String logMessage = buildExitLogMessage(methodLogger, methodName, returnValue, executionTime);

        // 설정된 로그 레벨에 따라 로깅
        logByLevel(methodLogger.level(), logMessage);

        // 통계 정보 주기적 로깅 (100번 호출마다)
        long currentCallCount = methodCallCount.get(methodName).get();
        if (currentCallCount % 100 == 0) {
            logMethodStatistics(methodName);
        }
    }

    /**
     * @AccessLogMethodLogger 어노테이션이 적용된 메서드에서 예외 발생 시 처리
     *
     * @AfterThrowing 어드바이스: 메서드에서 예외가 발생했을 때 호출
     * 예외 정보와 함께 메서드 실행 정보를 로깅
     *
     * @param joinPoint 실행된 메서드의 정보
     * @param methodLogger 메서드에 적용된 어노테이션
     * @param exception 발생한 예외 객체
     */
    @AfterThrowing(pointcut = "@annotation(methodLogger)", throwing = "exception")
    public void logMethodException(JoinPoint joinPoint, AccessLogMethodLogger methodLogger, Throwable exception) {
        String methodName = joinPoint.getSignature().toShortString();
        String threadMethodKey = getThreadMethodKey(methodName);

        // 예외 발생 통계 업데이트
        totalExceptions.incrementAndGet();

        // 실행 시간 측정 종료 (예외 발생 시에도)
        long executionTime = 0L;
        if (methodLogger.measureExecutionTime()) {
            StopWatch stopWatch = activeStopWatches.remove(threadMethodKey);
            if (stopWatch != null && stopWatch.isRunning()) {
                stopWatch.stop();
                executionTime = stopWatch.getTotalTimeMillis();
            }
        }

        // 예외 로그 메시지 구성
        String logMessage = String.format("[%s] 메서드 실행 중 예외 발생 | 실행시간: %dms | 예외: %s | 메시지: %s",
                methodName, executionTime, exception.getClass().getSimpleName(), exception.getMessage());

        // 예외 발생 시에는 항상 ERROR 레벨로 로깅
        log.error(logMessage, exception);

        // 스택 트레이스가 필요한 경우 추가 정보 로깅
        if (log.isDebugEnabled()) {
            log.debug("[예외 상세] {} | 발생 위치: {} | 스레드: {}",
                    methodName,
                    exception.getStackTrace().length > 0 ? exception.getStackTrace()[0] : "Unknown",
                    Thread.currentThread().getName());
        }
    }

    /**
     * 메서드 진입 로그 메시지 구성
     *
     * @param methodLogger 어노테이션 설정
     * @param methodName 메서드명
     * @param args 메서드 파라미터 배열
     * @return 구성된 로그 메시지
     */
    private String buildEntryLogMessage(AccessLogMethodLogger methodLogger, String methodName, Object[] args) {
        StringBuilder logMessage = new StringBuilder();

        // 커스텀 메시지가 있으면 사용, 없으면 기본 메시지 생성
        if (!methodLogger.customMessage().isEmpty()) {
            logMessage.append("[진입] ").append(methodLogger.customMessage());
        } else {
            logMessage.append("[진입] ").append(methodName).append(" 시작");
        }

        // 파라미터 로깅이 활성화되어 있고 파라미터가 존재하는 경우
        if (methodLogger.logParameters() && args != null && args.length > 0) {
            String parametersStr = formatParameters(args);
            logMessage.append(" | 파라미터: ").append(parametersStr);
        }

        return logMessage.toString();
    }

    /**
     * 메서드 종료 로그 메시지 구성
     *
     * @param methodLogger 어노테이션 설정
     * @param methodName 메서드명
     * @param returnValue 반환값
     * @param executionTime 실행 시간
     * @return 구성된 로그 메시지
     */
    private String buildExitLogMessage(AccessLogMethodLogger methodLogger, String methodName,
                                       Object returnValue, long executionTime) {
        StringBuilder logMessage = new StringBuilder();

        logMessage.append("[종료] ").append(methodName).append(" 완료");

        // 실행 시간 정보 추가
        if (methodLogger.measureExecutionTime()) {
            logMessage.append(" | 실행시간: ").append(executionTime).append("ms");
        }

        // 반환값 로깅이 활성화되어 있고 반환값이 존재하는 경우
        if (methodLogger.logReturnValue() && returnValue != null) {
            String returnValueStr = formatReturnValue(returnValue);
            logMessage.append(" | 반환값: ").append(returnValueStr);
        }

        return logMessage.toString();
    }

    /**
     * 파라미터 배열을 로깅용 문자열로 포맷팅
     * 개인정보 및 민감한 정보 마스킹 처리
     *
     * @param args 파라미터 배열
     * @return 포맷팅된 파라미터 문자열
     */
    private String formatParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "없음";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            if (args[i] == null) {
                sb.append("null");
            } else {
                String paramStr = args[i].toString();

                // 개인정보 마스킹 처리
                paramStr = maskSensitiveData(paramStr);

                // 너무 긴 파라미터는 잘라서 표시
                if (paramStr.length() > 100) {
                    paramStr = paramStr.substring(0, 100) + "...";
                }

                sb.append(paramStr);
            }
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * 반환값을 로깅용 문자열로 포맷팅
     *
     * @param returnValue 반환값 객체
     * @return 포맷팅된 반환값 문자열
     */
    private String formatReturnValue(Object returnValue) {
        if (returnValue == null) {
            return "null";
        }

        String returnStr = returnValue.toString();

        // 반환값이 너무 크면 일부만 로깅 (성능 및 로그 크기 고려)
        if (returnStr.length() > 200) {
            returnStr = returnStr.substring(0, 200) + "...";
        }

        // 민감한 정보 마스킹
        return maskSensitiveData(returnStr);
    }

    /**
     * 민감한 데이터 마스킹 처리
     * 이메일, IP 주소, 패스워드 등의 개인정보 보호
     *
     * @param data 원본 데이터 문자열
     * @return 마스킹 처리된 문자열
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        // 이메일 주소 마스킹: user@example.com -> u***@example.com
        data = data.replaceAll("([a-zA-Z0-9])([a-zA-Z0-9._%+-]*[a-zA-Z0-9])?@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
                "$1***@$3");

        // IP 주소 마스킹: 192.168.1.100 -> 192.168.1.***
        data = data.replaceAll("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.)\\d{1,3}\\b", "$1***");

        // 패스워드 필드 마스킹: password=abc123 -> password=***
        data = data.replaceAll("(?i)(password|pwd|pass)\\s*[=:]\\s*\\S+", "$1=***");

        return data;
    }

    /**
     * 로그 레벨에 따른 로깅 수행
     *
     * @param level 로그 레벨
     * @param message 로그 메시지
     */
    private void logByLevel(AccessLogMethodLogger.LogLevel level, String message) {
        switch (level) {
            case ERROR:
                log.error(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case INFO:
                log.info(message);
                break;
            case DEBUG:
                log.debug(message);
                break;
            case TRACE:
                log.trace(message);
                break;
        }
    }

    /**
     * 스레드별 메서드 키 생성
     * 멀티스레드 환경에서 StopWatch 충돌 방지
     *
     * @param methodName 메서드명
     * @return 스레드 고유 키
     */
    private String getThreadMethodKey(String methodName) {
        return Thread.currentThread().getId() + ":" + methodName;
    }

    /**
     * 메서드별 통계 정보 로깅
     *
     * @param methodName 메서드명
     */
    private void logMethodStatistics(String methodName) {
        long callCount = methodCallCount.get(methodName).get();
        long totalTime = methodTotalTime.getOrDefault(methodName, new AtomicLong(0)).get();
        long avgTime = callCount > 0 ? totalTime / callCount : 0;

        log.info("[통계] {} | 총 호출: {}회 | 총 시간: {}ms | 평균 시간: {}ms",
                methodName, callCount, totalTime, avgTime);
    }

    /**
     * 전체 로깅 통계 정보 조회
     * 모니터링 및 성능 분석용
     *
     * @return 로깅 통계 정보
     */
    public LoggingStats getLoggingStats() {
        return new LoggingStats(
                totalMethodCalls.get(),
                totalExceptions.get(),
                methodCallCount.size(),
                activeStopWatches.size()
        );
    }

    /**
     * 로깅 통계 정보를 담는 내부 클래스
     */
    public static class LoggingStats {
        private final long totalMethodCalls;  // 총 메서드 호출 횟수
        private final long totalExceptions;   // 총 예외 발생 횟수
        private final int uniqueMethods;      // 고유 메서드 수
        private final int activeStopWatches;  // 활성 StopWatch 수

        public LoggingStats(long totalMethodCalls, long totalExceptions, int uniqueMethods, int activeStopWatches) {
            this.totalMethodCalls = totalMethodCalls;
            this.totalExceptions = totalExceptions;
            this.uniqueMethods = uniqueMethods;
            this.activeStopWatches = activeStopWatches;
        }

        // Getter 메서드들
        public long getTotalMethodCalls() { return totalMethodCalls; }
        public long getTotalExceptions() { return totalExceptions; }
        public int getUniqueMethods() { return uniqueMethods; }
        public int getActiveStopWatches() { return activeStopWatches; }

        public double getExceptionRate() {
            return totalMethodCalls > 0 ? (double) totalExceptions / totalMethodCalls * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("LoggingStats{calls=%d, exceptions=%d, methods=%d, active=%d, exceptionRate=%.2f%%}",
                    totalMethodCalls, totalExceptions, uniqueMethods, activeStopWatches, getExceptionRate());
        }
    }
}