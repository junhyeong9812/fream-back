package com.fream.back.domain.accessLog.aop.aspect;

import com.fream.back.domain.accessLog.aop.annotation.AccessLogExceptionHandler;
import com.fream.back.domain.accessLog.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 접근 로그 도메인의 예외 처리를 전담하는 AOP Aspect
 *
 * 주요 기능:
 * - 일반 예외를 도메인 특화 예외로 자동 변환
 * - 재시도 로직 (지수 백오프 방식)
 * - 예외별 로깅 레벨 제어
 * - 예외 발생 통계 수집
 *
 * Order(1): 가장 높은 우선순위로 설정하여 다른 Aspect보다 먼저 실행
 * 예외 처리가 가장 바깥쪽에서 이루어져야 하기 때문
 */
@Aspect // AspectJ Aspect 클래스 선언
@Component // Spring Bean으로 등록
@Order(1) // 실행 순서 최우선 (낮은 숫자가 높은 우선순위)
@Slf4j // Lombok 로거 자동 생성
public class AccessLogExceptionAspect {

    // 예외 발생 통계를 위한 Thread-Safe 카운터들
    private final AtomicLong totalExceptions = new AtomicLong(0); // 총 예외 발생 횟수
    private final AtomicLong retriedExceptions = new AtomicLong(0); // 재시도된 예외 횟수
    private final AtomicLong convertedExceptions = new AtomicLong(0); // 변환된 예외 횟수

    /**
     * @AccessLogExceptionHandler 어노테이션이 적용된 메서드의 예외 처리
     *
     * @Around 어드바이스를 사용하여 메서드 실행을 완전히 제어
     * 예외 발생 시 도메인 예외로 변환하고 재시도 로직 적용
     *
     * @param joinPoint 실행될 메서드의 정보와 제어권
     * @param exceptionHandler 예외 처리 설정 어노테이션
     * @return 원래 메서드의 반환값
     * @throws Throwable 변환된 도메인 예외 또는 원본 예외
     */
    @Around("@annotation(exceptionHandler)")
    public Object handleAccessLogException(ProceedingJoinPoint joinPoint,
                                           AccessLogExceptionHandler exceptionHandler) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString(); // 메서드 시그니처 문자열
        String className = joinPoint.getTarget().getClass().getSimpleName(); // 실행 클래스명

        // 재시도 기능이 활성화된 경우
        if (exceptionHandler.retry()) {
            return executeWithRetry(joinPoint, exceptionHandler, methodName);
        }

        // 일반적인 예외 처리 (재시도 없음)
        return executeWithExceptionHandling(joinPoint, exceptionHandler, methodName);
    }

    /**
     * 재시도 로직이 포함된 메서드 실행
     *
     * 지수 백오프(Exponential Backoff) 방식으로 재시도 간격을 점진적으로 증가
     * 재시도 간격: 1초 -> 2초 -> 4초 -> 8초 (최대 10초)
     *
     * @param joinPoint 실행될 메서드
     * @param exceptionHandler 예외 처리 설정
     * @param methodName 메서드명 (로깅용)
     * @return 메서드 실행 결과
     * @throws Throwable 최종 실패 시 예외
     */
    private Object executeWithRetry(ProceedingJoinPoint joinPoint,
                                    AccessLogExceptionHandler exceptionHandler,
                                    String methodName) throws Throwable {

        int maxRetries = exceptionHandler.retryCount(); // 최대 재시도 횟수
        Exception lastException = null; // 마지막 발생한 예외 저장

        log.debug("[재시도 시작] {} | 최대 재시도 횟수: {}", methodName, maxRetries);

        // 재시도 루프: 0부터 maxRetries까지 (총 maxRetries+1번 실행)
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // 재시도인 경우 대기 시간 적용
                if (attempt > 0) {
                    long waitTime = calculateBackoffTime(attempt); // 지수 백오프 계산
                    log.info("[재시도] {} | {}번째 시도 | 대기시간: {}ms", methodName, attempt + 1, waitTime);

                    Thread.sleep(waitTime); // 지정된 시간만큼 대기
                    retriedExceptions.incrementAndGet(); // 재시도 통계 증가
                }

                // 실제 메서드 실행
                Object result = joinPoint.proceed();

                // 성공 시 로깅 (재시도였던 경우에만)
                if (attempt > 0) {
                    log.info("[재시도 성공] {} | {}번째 시도에서 성공", methodName, attempt + 1);
                }

                return result; // 성공 시 결과 반환

            } catch (InterruptedException ie) {
                // 재시도 중 인터럽트 발생 시 처리
                Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                log.warn("[재시도 중단] {} | 인터럽트 발생", methodName);

                throw new AccessLogException(AccessLogErrorCode.ACCESS_LOG_SAVE_ERROR,
                        "재시도 중 인터럽트가 발생했습니다.", ie);

            } catch (Exception e) {
                lastException = e; // 예외 저장
                totalExceptions.incrementAndGet(); // 예외 통계 증가

                log.warn("[재시도 실패] {} | {}번째 시도 실패 | 오류: {}",
                        methodName, attempt + 1, e.getMessage());

                // 마지막 시도가 아니면 계속 재시도
                if (attempt < maxRetries) {
                    continue;
                }

                // 모든 재시도 실패 시 예외 변환 후 던지기
                log.error("[재시도 최종 실패] {} | 총 {}번 시도 후 실패", methodName, maxRetries + 1);
                throw convertAndLogException(lastException, exceptionHandler, methodName);
            }
        }

        // 이론적으로 도달하지 않는 코드 (컴파일러 만족용)
        throw new AccessLogException(AccessLogErrorCode.ACCESS_LOG_SAVE_ERROR,
                "알 수 없는 오류가 발생했습니다.", lastException);
    }

    /**
     * 일반적인 예외 처리 (재시도 없음)
     *
     * @param joinPoint 실행될 메서드
     * @param exceptionHandler 예외 처리 설정
     * @param methodName 메서드명
     * @return 메서드 실행 결과
     * @throws Throwable 변환된 예외
     */
    private Object executeWithExceptionHandling(ProceedingJoinPoint joinPoint,
                                                AccessLogExceptionHandler exceptionHandler,
                                                String methodName) throws Throwable {
        try {
            // 메서드 실행
            return joinPoint.proceed();

        } catch (AccessLogException e) {
            // 이미 AccessLog 도메인 예외인 경우 그대로 전파
            logException(exceptionHandler.logLevel(), methodName, e);
            throw e;

        } catch (Exception e) {
            // 다른 예외를 AccessLog 도메인 예외로 변환
            totalExceptions.incrementAndGet();
            throw convertAndLogException(e, exceptionHandler, methodName);
        }
    }

    /**
     * 예외 변환 및 로깅
     *
     * @param originalException 원본 예외
     * @param exceptionHandler 예외 처리 설정
     * @param methodName 메서드명
     * @return 변환된 도메인 예외
     */
    private AccessLogException convertAndLogException(Exception originalException,
                                                      AccessLogExceptionHandler exceptionHandler,
                                                      String methodName) {
        AccessLogException convertedException = convertToAccessLogException(originalException, exceptionHandler);
        logException(exceptionHandler.logLevel(), methodName, convertedException);
        convertedExceptions.incrementAndGet(); // 변환 통계 증가
        return convertedException;
    }

    /**
     * 원본 예외를 AccessLog 도메인 예외로 변환
     *
     * 예외 타입과 원본 예외의 종류에 따라 적절한 도메인 예외 생성
     *
     * @param originalException 원본 예외
     * @param annotation 예외 처리 설정
     * @return 변환된 도메인 예외
     */
    private AccessLogException convertToAccessLogException(Exception originalException,
                                                           AccessLogExceptionHandler annotation) {

        String customMessage = annotation.message().isEmpty() ? null : annotation.message();

        // 예외 타입에 따른 분기 처리
        switch (annotation.defaultType()) {
            case SAVE:
                return createSaveException(originalException, customMessage);
            case QUERY:
                return createQueryException(originalException, customMessage);
            case KAFKA:
                return createKafkaException(originalException, customMessage);
            case GEO_IP:
                return createGeoIPException(originalException, customMessage);
            case VALIDATION:
                return createValidationException(originalException, customMessage);
            case GENERAL:
            default:
                return createGeneralException(originalException, customMessage);
        }
    }

    /**
     * 저장 관련 예외 생성
     *
     * DataAccessException인 경우 데이터베이스 관련 메시지 적용
     *
     * @param originalException 원본 예외
     * @param customMessage 사용자 정의 메시지
     * @return AccessLogSaveException
     */
    private AccessLogSaveException createSaveException(Exception originalException, String customMessage) {
        if (originalException instanceof DataAccessException) {
            return new AccessLogSaveException(
                    AccessLogErrorCode.ACCESS_LOG_SAVE_ERROR,
                    customMessage != null ? customMessage : "접근 로그 저장 중 데이터베이스 오류가 발생했습니다.",
                    originalException
            );
        }

        return new AccessLogSaveException(
                AccessLogErrorCode.ACCESS_LOG_SAVE_ERROR,
                customMessage != null ? customMessage : originalException.getMessage(),
                originalException
        );
    }

    /**
     * 조회 관련 예외 생성
     *
     * @param originalException 원본 예외
     * @param customMessage 사용자 정의 메시지
     * @return AccessLogQueryException
     */
    private AccessLogQueryException createQueryException(Exception originalException, String customMessage) {
        if (originalException instanceof DataAccessException) {
            return new AccessLogQueryException(
                    AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR,
                    customMessage != null ? customMessage : "접근 로그 조회 중 데이터베이스 오류가 발생했습니다.",
                    originalException
            );
        }

        return new AccessLogQueryException(
                AccessLogErrorCode.ACCESS_LOG_QUERY_ERROR,
                customMessage != null ? customMessage : originalException.getMessage(),
                originalException
        );
    }

    /**
     * Kafka 관련 예외 생성
     *
     * @param originalException 원본 예외
     * @param customMessage 사용자 정의 메시지
     * @return AccessLogKafkaException
     */
    private AccessLogKafkaException createKafkaException(Exception originalException, String customMessage) {
        if (originalException instanceof KafkaException) {
            return new AccessLogKafkaException(
                    AccessLogErrorCode.KAFKA_SEND_ERROR,
                    customMessage != null ? customMessage : "Kafka 메시지 처리 중 오류가 발생했습니다.",
                    originalException
            );
        }

        return new AccessLogKafkaException(
                AccessLogErrorCode.KAFKA_SEND_ERROR,
                customMessage != null ? customMessage : originalException.getMessage(),
                originalException
        );
    }

    /**
     * GeoIP 관련 예외 생성
     *
     * @param originalException 원본 예외
     * @param customMessage 사용자 정의 메시지
     * @return GeoIPException
     */
    private GeoIPException createGeoIPException(Exception originalException, String customMessage) {
        return new GeoIPException(
                AccessLogErrorCode.GEO_IP_LOOKUP_ERROR,
                customMessage != null ? customMessage : originalException.getMessage(),
                originalException
        );
    }

    /**
     * 파라미터 검증 관련 예외 생성
     *
     * @param originalException 원본 예외
     * @param customMessage 사용자 정의 메시지
     * @return InvalidParameterException
     */
    private InvalidParameterException createValidationException(Exception originalException, String customMessage) {
        return new InvalidParameterException(
                AccessLogErrorCode.INVALID_ACCESS_LOG_DATA,
                customMessage != null ? customMessage : originalException.getMessage(),
                originalException
        );
    }

    /**
     * 일반적인 AccessLog 예외 생성
     *
     * @param originalException 원본 예외
     * @param customMessage 사용자 정의 메시지
     * @return AccessLogException
     */
    private AccessLogException createGeneralException(Exception originalException, String customMessage) {
        return new AccessLogException(
                AccessLogErrorCode.ACCESS_LOG_SAVE_ERROR, // 기본 에러 코드
                customMessage != null ? customMessage : originalException.getMessage(),
                originalException
        );
    }

    /**
     * 로그 레벨에 따른 예외 로깅
     *
     * @param logLevel 로깅 레벨
     * @param methodName 메서드명
     * @param exception 예외 객체
     */
    private void logException(AccessLogExceptionHandler.LogLevel logLevel, String methodName, Exception exception) {
        String logMessage = "[예외 처리] {} | 예외 타입: {} | 메시지: {}";

        switch (logLevel) {
            case ERROR:
                log.error(logMessage, methodName, exception.getClass().getSimpleName(), exception.getMessage(), exception);
                break;
            case WARN:
                log.warn(logMessage, methodName, exception.getClass().getSimpleName(), exception.getMessage());
                break;
            case INFO:
                log.info(logMessage, methodName, exception.getClass().getSimpleName(), exception.getMessage());
                break;
            case DEBUG:
                log.debug(logMessage, methodName, exception.getClass().getSimpleName(), exception.getMessage());
                break;
        }
    }

    /**
     * 지수 백오프 방식의 대기 시간 계산
     *
     * 재시도 횟수에 따라 대기 시간을 지수적으로 증가
     * 최대 10초까지 제한하여 무한정 대기 방지
     *
     * @param attempt 재시도 횟수 (1부터 시작)
     * @return 대기 시간 (밀리초)
     */
    private long calculateBackoffTime(int attempt) {
        // 2^(attempt-1) * 1000ms, 최대 10초
        long backoffTime = (long) (1000 * Math.pow(2, attempt - 1));
        return Math.min(backoffTime, 10000L); // 최대 10초로 제한
    }

    /**
     * 예외 처리 통계 정보 조회
     * 모니터링 및 디버깅용
     *
     * @return 예외 통계 정보
     */
    public ExceptionStats getExceptionStats() {
        return new ExceptionStats(
                totalExceptions.get(),
                retriedExceptions.get(),
                convertedExceptions.get()
        );
    }

    /**
     * 예외 통계 정보를 담는 내부 클래스
     */
    public static class ExceptionStats {
        private final long totalExceptions;    // 총 예외 발생 횟수
        private final long retriedExceptions;  // 재시도된 예외 횟수
        private final long convertedExceptions; // 변환된 예외 횟수

        public ExceptionStats(long totalExceptions, long retriedExceptions, long convertedExceptions) {
            this.totalExceptions = totalExceptions;
            this.retriedExceptions = retriedExceptions;
            this.convertedExceptions = convertedExceptions;
        }

        // Getter 메서드들
        public long getTotalExceptions() { return totalExceptions; }
        public long getRetriedExceptions() { return retriedExceptions; }
        public long getConvertedExceptions() { return convertedExceptions; }

        public double getRetryRate() {
            return totalExceptions > 0 ? (double) retriedExceptions / totalExceptions * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("ExceptionStats{total=%d, retried=%d, converted=%d, retryRate=%.2f%%}",
                    totalExceptions, retriedExceptions, convertedExceptions, getRetryRate());
        }
    }
}