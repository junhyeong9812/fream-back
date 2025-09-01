package com.fream.back.domain.chatQuestion.aop;

import com.fream.back.domain.chatQuestion.aop.annotation.ChatExceptionHandling;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionException;
import com.fream.back.domain.chatQuestion.exception.GPTApiException;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChatQuestion 도메인 예외 처리 AOP
 * @ChatExceptionHandling 어노테이션을 기반으로 예외 처리 수행
 */
@Aspect
@Component
@Slf4j
public class ChatExceptionAspect {

    // 서킷 브레이커 상태 관리
    private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakerStates = new ConcurrentHashMap<>();

    /**
     * @ChatExceptionHandling 어노테이션이 붙은 메서드의 예외 처리
     *
     * @param proceedingJoinPoint 조인포인트
     * @param exceptionHandling 예외 처리 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 처리되지 않은 예외
     */
    @Around("@annotation(exceptionHandling)")
    public Object handleAnnotatedException(ProceedingJoinPoint proceedingJoinPoint,
                                           ChatExceptionHandling exceptionHandling) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();
        String userEmail = extractUserEmailSafely();

        // 서킷 브레이커 검사
        if (exceptionHandling.strategy() == ChatExceptionHandling.Strategy.CIRCUIT_BREAKER) {
            if (isCircuitOpen(className + "." + methodName)) {
                return handleCircuitOpenFallback(proceedingJoinPoint, exceptionHandling);
            }
        }

        int retryCount = 0;
        int maxRetries = exceptionHandling.retryable() ? exceptionHandling.maxRetries() : 0;
        long retryDelay = exceptionHandling.retryDelay();

        while (retryCount <= maxRetries) {
            try {
                Object result = proceedingJoinPoint.proceed();

                // 성공 시 서킷 브레이커 상태 리셋
                if (exceptionHandling.strategy() == ChatExceptionHandling.Strategy.CIRCUIT_BREAKER) {
                    recordCircuitBreakerSuccess(className + "." + methodName);
                }

                return result;

            } catch (Exception exception) {
                boolean shouldHandle = shouldHandleException(exception, exceptionHandling);

                if (!shouldHandle) {
                    throw exception;
                }

                // 서킷 브레이커 실패 기록
                if (exceptionHandling.strategy() == ChatExceptionHandling.Strategy.CIRCUIT_BREAKER) {
                    recordCircuitBreakerFailure(className + "." + methodName);
                }

                // 재시도 로직
                if (exceptionHandling.retryable() && retryCount < maxRetries && isRetryableException(exception)) {
                    retryCount++;
                    log.warn("Chat 메서드 재시도 - Class: {}, Method: {}, User: {}, Retry: {}/{}, Exception: {}",
                            className, methodName, userEmail, retryCount, maxRetries, exception.getMessage());

                    if (retryDelay > 0) {
                        Thread.sleep(retryDelay * retryCount); // 지수 백오프
                    }
                    continue;
                }

                // 예외 처리 전략 실행
                return executeExceptionStrategy(proceedingJoinPoint, exception, exceptionHandling,
                        methodName, className, userEmail, args);
            }
        }

        throw new RuntimeException("Unexpected error in retry logic");
    }

    /**
     * Chat Service 메서드에서 예외 발생 시 기본 처리
     */
    @AfterThrowing(pointcut = "execution(* com.fream.back.domain.chatQuestion.service..*(..))", throwing = "exception")
    public void handleServiceException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        log.error("Chat Service 예외 발생 - Class: {}, Method: {}, User: {}, Exception: {}",
                className, methodName, userEmail, exception.getMessage(), exception);

        // 예외 타입별 추가 처리
        if (exception instanceof GPTApiException) {
            handleGPTApiException((GPTApiException) exception, methodName, userEmail, args);
        } else if (exception instanceof ChatQuestionException) {
            handleChatQuestionException((ChatQuestionException) exception, methodName, userEmail, args);
        } else {
            handleGenericException(exception, methodName, userEmail, args);
        }
    }

    /**
     * Chat Controller 메서드에서 예외 발생 시 기본 처리
     */
    @AfterThrowing(pointcut = "execution(* com.fream.back.domain.chatQuestion.controller..*(..))", throwing = "exception")
    public void handleControllerException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        log.error("Chat Controller 예외 발생 - Class: {}, Method: {}, User: {}, Exception: {}",
                className, methodName, userEmail, exception.getMessage(), exception);

        logControllerExceptionMetrics(methodName, exception, userEmail);
    }

    /**
     * 예외 처리 전략 실행
     */
    private Object executeExceptionStrategy(ProceedingJoinPoint proceedingJoinPoint, Exception exception,
                                            ChatExceptionHandling exceptionHandling,
                                            String methodName, String className, String userEmail, Object[] args) throws Throwable {

        logExceptionDetails(exception, exceptionHandling, methodName, className, userEmail, args);

        switch (exceptionHandling.strategy()) {
            case LOG_ONLY:
                log.warn("예외 로그만 기록 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                return getDefaultReturnValue(proceedingJoinPoint);

            case LOG_AND_RETHROW:
                log.error("예외 로그 후 재발생 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                throw exception;

            case TRANSFORM:
                String customMessage = exceptionHandling.customMessage().isEmpty()
                        ? "채팅 처리 중 오류가 발생했습니다."
                        : exceptionHandling.customMessage();
                throw new ChatQuestionException(ChatQuestionErrorCode.CHAT_QUESTION_SAVE_ERROR, customMessage, exception);

            case SUPPRESS:
                log.debug("예외 무시 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                return getDefaultReturnValue(proceedingJoinPoint);

            case FALLBACK:
                log.info("폴백 처리 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                return executeFallbackLogic(proceedingJoinPoint, exception, exceptionHandling);

            case CIRCUIT_BREAKER:
                log.warn("서킷 브레이커 동작 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                return executeFallbackLogic(proceedingJoinPoint, exception, exceptionHandling);

            default:
                throw exception;
        }
    }

    /**
     * GPT API 예외 세부 처리
     */
    private void handleGPTApiException(GPTApiException gptException, String methodName, String userEmail, Object[] args) {
        ChatQuestionErrorCode errorCode = (ChatQuestionErrorCode) gptException.getErrorCode();

        switch (errorCode) {
            case GPT_API_ERROR:
                log.error("GPT API 호출 실패 - Method: {}, User: {}, Args: {}", methodName, userEmail, formatArgs(args));
                break;
            case GPT_USAGE_LIMIT_EXCEEDED:
                log.warn("GPT 사용량 한도 초과 - Method: {}, User: {}, Args: {}", methodName, userEmail, formatArgs(args));
                break;
            case GPT_RESPONSE_PROCESSING_ERROR:
                log.error("GPT 응답 처리 오류 - Method: {}, User: {}, Args: {}", methodName, userEmail, formatArgs(args));
                break;
            default:
                log.error("알 수 없는 GPT API 예외 - ErrorCode: {}, Method: {}, User: {}",
                        errorCode.getCode(), methodName, userEmail);
        }
    }

    /**
     * ChatQuestion 예외 세부 처리
     */
    private void handleChatQuestionException(ChatQuestionException chatException, String methodName, String userEmail, Object[] args) {
        ChatQuestionErrorCode errorCode = (ChatQuestionErrorCode) chatException.getErrorCode();

        switch (errorCode) {
            case INVALID_QUESTION_DATA:
            case QUESTION_LENGTH_EXCEEDED:
                log.warn("질문 데이터 유효성 검증 실패 - ErrorCode: {}, Method: {}, User: {}, Args: {}",
                        errorCode.getCode(), methodName, userEmail, formatArgs(args));
                break;
            case QUESTION_PERMISSION_DENIED:
            case ADMIN_PERMISSION_REQUIRED:
                log.warn("권한 부족 - ErrorCode: {}, Method: {}, User: {}, Args: {}",
                        errorCode.getCode(), methodName, userEmail, formatArgs(args));
                break;
            case CHAT_QUESTION_SAVE_ERROR:
            case CHAT_HISTORY_QUERY_ERROR:
                log.error("데이터베이스 작업 실패 - ErrorCode: {}, Method: {}, User: {}, Args: {}",
                        errorCode.getCode(), methodName, userEmail, formatArgs(args));
                break;
            default:
                log.error("알 수 없는 Chat 예외 - ErrorCode: {}, Method: {}, User: {}",
                        errorCode.getCode(), methodName, userEmail);
        }
    }

    /**
     * 일반 예외 처리
     */
    private void handleGenericException(Exception exception, String methodName, String userEmail, Object[] args) {
        if (exception instanceof IllegalArgumentException) {
            log.warn("잘못된 파라미터 - Method: {}, User: {}, Args: {}, Message: {}",
                    methodName, userEmail, formatArgs(args), exception.getMessage());
        } else if (exception instanceof NullPointerException) {
            log.error("Null 포인터 예외 - Method: {}, User: {}, Args: {}",
                    methodName, userEmail, formatArgs(args), exception);
        } else {
            log.error("예상치 못한 예외 - Method: {}, User: {}, Type: {}, Message: {}",
                    methodName, userEmail, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }

    /**
     * 폴백 로직 실행
     */
    private Object executeFallbackLogic(ProceedingJoinPoint proceedingJoinPoint, Exception exception,
                                        ChatExceptionHandling exceptionHandling) {

        // 메서드 반환 타입에 따른 폴백 처리
        Class<?> returnType = ((org.aspectj.lang.reflect.MethodSignature)
                proceedingJoinPoint.getSignature()).getReturnType();

        if (returnType == QuestionResponseDto.class) {
            // 채팅 응답의 경우 폴백 메시지 반환
            return QuestionResponseDto.builder()
                    .question("질문 처리 중 오류가 발생했습니다.")
                    .answer(exceptionHandling.fallbackMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        return getDefaultReturnValue(proceedingJoinPoint);
    }

    /**
     * 메서드 반환 타입에 따른 기본값 반환
     */
    private Object getDefaultReturnValue(ProceedingJoinPoint proceedingJoinPoint) {
        Class<?> returnType = ((org.aspectj.lang.reflect.MethodSignature)
                proceedingJoinPoint.getSignature()).getReturnType();

        if (returnType == void.class) {
            return null;
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        } else if (returnType.isPrimitive()) {
            return 0;
        } else if (java.util.List.class.isAssignableFrom(returnType)) {
            return java.util.Collections.emptyList();
        } else if (java.util.Map.class.isAssignableFrom(returnType)) {
            return java.util.Collections.emptyMap();
        } else {
            return null;
        }
    }

    /**
     * 재시도 가능한 예외인지 확인
     */
    private boolean isRetryableException(Exception exception) {
        return exception instanceof ResourceAccessException ||
                exception instanceof HttpServerErrorException ||
                (exception instanceof HttpClientErrorException &&
                        ((HttpClientErrorException) exception).getStatusCode().value() == 429);
    }

    /**
     * 서킷 브레이커 상태 관리
     */
    private boolean isCircuitOpen(String methodKey) {
        CircuitBreakerState state = circuitBreakerStates.get(methodKey);
        if (state == null) {
            return false;
        }

        // 실패율이 50% 이상이고 최소 10회 호출된 경우 서킷 오픈
        return state.getFailureRate() > 0.5 && state.getTotalCalls() >= 10;
    }

    private Object handleCircuitOpenFallback(ProceedingJoinPoint proceedingJoinPoint,
                                             ChatExceptionHandling exceptionHandling) {
        log.warn("서킷 브레이커 오픈 상태 - 폴백 응답 반환: {}",
                proceedingJoinPoint.getSignature().getName());
        return executeFallbackLogic(proceedingJoinPoint,
                new RuntimeException("서킷 브레이커 오픈"), exceptionHandling);
    }

    private void recordCircuitBreakerSuccess(String methodKey) {
        circuitBreakerStates.computeIfAbsent(methodKey, k -> new CircuitBreakerState())
                .recordSuccess();
    }

    private void recordCircuitBreakerFailure(String methodKey) {
        circuitBreakerStates.computeIfAbsent(methodKey, k -> new CircuitBreakerState())
                .recordFailure();
    }

    /**
     * 예외 처리 여부 결정
     */
    private boolean shouldHandleException(Exception exception, ChatExceptionHandling exceptionHandling) {
        Class<? extends Throwable>[] handleExceptions = exceptionHandling.handleExceptions();
        Class<? extends Throwable>[] ignoreExceptions = exceptionHandling.ignoreExceptions();

        // 무시할 예외 타입인지 확인
        if (ignoreExceptions.length > 0) {
            for (Class<? extends Throwable> ignoreType : ignoreExceptions) {
                if (ignoreType.isAssignableFrom(exception.getClass())) {
                    return false;
                }
            }
        }

        // 처리할 예외 타입인지 확인
        if (handleExceptions.length > 0) {
            for (Class<? extends Throwable> handleType : handleExceptions) {
                if (handleType.isAssignableFrom(exception.getClass())) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * 예외 상세 로깅
     */
    private void logExceptionDetails(Exception exception, ChatExceptionHandling exceptionHandling,
                                     String methodName, String className, String userEmail, Object[] args) {
        if (exceptionHandling.logStackTrace()) {
            log.error("Chat 예외 스택트레이스 - Class: {}, Method: {}, User: {}",
                    className, methodName, userEmail, exception);
        }

        if (exceptionHandling.includeUserInfo()) {
            log.error("Chat 예외 사용자 정보 - Method: {}, User: {}, Exception: {}",
                    methodName, userEmail, exception.getMessage());
        }

        if (exceptionHandling.includeQuestionContent() && args != null) {
            log.error("Chat 예외 질문 정보 - Method: {}, User: {}, Args: {}, Exception: {}",
                    methodName, userEmail, formatArgs(args), exception.getMessage());
        }

        if (exceptionHandling.collectMetrics()) {
            log.info("METRICS: chat.exception.{}.{} user={} exception_type={}",
                    className.toLowerCase(), methodName.toLowerCase(),
                    userEmail, exception.getClass().getSimpleName());
        }

        if (exceptionHandling.sendAlert()) {
            sendAlert(exception, exceptionHandling.alertLevel(), methodName, className, userEmail);
        }
    }

    /**
     * 알림 전송
     */
    private void sendAlert(Exception exception, ChatExceptionHandling.AlertLevel alertLevel,
                           String methodName, String className, String userEmail) {
        log.warn("ALERT[{}]: Chat Exception - Class: {}, Method: {}, User: {}, Exception: {}",
                alertLevel, className, methodName, userEmail, exception.getMessage());
    }

    /**
     * 컨트롤러 예외 메트릭스 로깅
     */
    private void logControllerExceptionMetrics(String methodName, Exception exception, String userEmail) {
        log.info("METRICS: Chat Controller Exception - Method: {}, Exception: {}, User: {}",
                methodName, exception.getClass().getSimpleName(), userEmail);
    }

    /**
     * 안전하게 사용자 이메일 추출
     */
    private String extractUserEmailSafely() {
        try {
            return SecurityUtils.extractEmailOrAnonymous();
        } catch (Exception e) {
            log.debug("사용자 이메일 추출 실패: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 메서드 파라미터를 안전하게 문자열로 변환 (질문 내용 마스킹 포함)
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                String str = (String) arg;
                if (str.contains("@")) {
                    sb.append("email:").append(maskEmail(str));
                } else if (str.length() > 100) {
                    // 긴 질문 내용은 요약해서 로깅
                    sb.append("question:").append(str.substring(0, 30)).append("...");
                } else {
                    sb.append("'").append(str).append("'");
                }
            } else {
                sb.append(arg.getClass().getSimpleName()).append("@").append(Integer.toHexString(arg.hashCode()));
            }
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 이메일 마스킹 처리
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }

        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domainPart;
        } else {
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
        }
    }

    /**
     * 서킷 브레이커 상태 관리 클래스
     */
    private static class CircuitBreakerState {
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);

        public void recordSuccess() {
            successCount.incrementAndGet();
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
        }

        public int getTotalCalls() {
            return successCount.get() + failureCount.get();
        }

        public double getFailureRate() {
            int total = getTotalCalls();
            return total == 0 ? 0.0 : (double) failureCount.get() / total;
        }
    }
}