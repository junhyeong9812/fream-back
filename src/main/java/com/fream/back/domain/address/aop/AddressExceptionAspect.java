package com.fream.back.domain.address.aop;

import com.fream.back.domain.address.aop.annotation.AddressExceptionHandling;
import com.fream.back.domain.address.exception.AddressErrorCode;
import com.fream.back.domain.address.exception.AddressException;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Address 도메인 예외 처리 AOP
 * @AddressExceptionHandling 어노테이션을 기반으로 예외 처리 수행
 */
@Aspect
@Component
@Slf4j
public class AddressExceptionAspect {

    /**
     * @AddressExceptionHandling 어노테이션이 붙은 메서드의 예외 처리
     *
     * @param proceedingJoinPoint 조인포인트
     * @param exceptionHandling 예외 처리 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 처리되지 않은 예외
     */
    @Around("@annotation(exceptionHandling)")
    public Object handleAnnotatedException(ProceedingJoinPoint proceedingJoinPoint,
                                           AddressExceptionHandling exceptionHandling) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();
        String userEmail = extractUserEmailSafely();

        int retryCount = 0;
        int maxRetries = exceptionHandling.retryable() ? exceptionHandling.maxRetries() : 0;
        long retryDelay = exceptionHandling.retryDelay();

        while (retryCount <= maxRetries) {
            try {
                return proceedingJoinPoint.proceed();

            } catch (Exception exception) {
                boolean shouldHandle = shouldHandleException(exception, exceptionHandling);

                if (!shouldHandle) {
                    throw exception;
                }

                // 재시도 로직
                if (exceptionHandling.retryable() && retryCount < maxRetries) {
                    retryCount++;
                    log.warn("Address 메서드 재시도 - Class: {}, Method: {}, User: {}, Retry: {}/{}, Exception: {}",
                            className, methodName, userEmail, retryCount, maxRetries, exception.getMessage());

                    if (retryDelay > 0) {
                        Thread.sleep(retryDelay);
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
     * Address Service 메서드에서 예외 발생 시 기본 처리
     */
    @AfterThrowing(pointcut = "execution(* com.fream.back.domain.address.service..*(..))", throwing = "exception")
    public void handleServiceException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        log.error("Address Service 예외 발생 - Class: {}, Method: {}, User: {}, Exception: {}",
                className, methodName, userEmail, exception.getMessage(), exception);

        // 예외 타입별 추가 처리
        if (exception instanceof AddressException) {
            handleAddressException((AddressException) exception, methodName, userEmail, args);
        } else {
            handleGenericException(exception, methodName, userEmail, args);
        }
    }

    /**
     * Address Controller 메서드에서 예외 발생 시 기본 처리
     */
    @AfterThrowing(pointcut = "execution(* com.fream.back.domain.address.controller..*(..))", throwing = "exception")
    public void handleControllerException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        log.error("Address Controller 예외 발생 - Class: {}, Method: {}, User: {}, Exception: {}",
                className, methodName, userEmail, exception.getMessage(), exception);

        logControllerExceptionMetrics(methodName, exception, userEmail);
    }

    /**
     * Address Repository 메서드에서 예외 발생 시 기본 처리
     */
    @AfterThrowing(pointcut = "execution(* com.fream.back.domain.address.repository..*(..))", throwing = "exception")
    public void handleRepositoryException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        log.error("Address Repository 예외 발생 - Class: {}, Method: {}, User: {}, Exception: {}",
                className, methodName, userEmail, exception.getMessage(), exception);

        logDatabaseExceptionMetrics(methodName, exception);
    }

    /**
     * 예외 처리 전략 실행
     */
    private Object executeExceptionStrategy(ProceedingJoinPoint proceedingJoinPoint, Exception exception,
                                            AddressExceptionHandling exceptionHandling,
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
                        ? "Address 작업 중 오류가 발생했습니다."
                        : exceptionHandling.customMessage();
                throw new AddressException(AddressErrorCode.ADDRESS_CREATE_ERROR, customMessage, exception);

            case SUPPRESS:
                log.debug("예외 무시 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                return getDefaultReturnValue(proceedingJoinPoint);

            case FALLBACK:
                log.info("폴백 처리 - Class: {}, Method: {}, User: {}, Exception: {}",
                        className, methodName, userEmail, exception.getMessage());
                return executeFallbackLogic(proceedingJoinPoint, exception);

            default:
                throw exception;
        }
    }

    /**
     * 예외 상세 로깅
     */
    private void logExceptionDetails(Exception exception, AddressExceptionHandling exceptionHandling,
                                     String methodName, String className, String userEmail, Object[] args) {
        if (exceptionHandling.logStackTrace()) {
            log.error("Address 예외 스택트레이스 - Class: {}, Method: {}, User: {}",
                    className, methodName, userEmail, exception);
        }

        if (exceptionHandling.includeUserInfo()) {
            log.error("Address 예외 사용자 정보 - Method: {}, User: {}, Exception: {}",
                    methodName, userEmail, exception.getMessage());
        }

        if (exceptionHandling.includeParameters() && args != null) {
            log.error("Address 예외 파라미터 정보 - Method: {}, User: {}, Args: {}, Exception: {}",
                    methodName, userEmail, formatArgs(args), exception.getMessage());
        }

        if (exceptionHandling.collectMetrics()) {
            log.info("METRICS: address.exception.{}.{} user={} exception_type={}",
                    className.toLowerCase(), methodName.toLowerCase(),
                    userEmail, exception.getClass().getSimpleName());
        }

        if (exceptionHandling.sendAlert()) {
            sendAlert(exception, exceptionHandling.alertLevel(), methodName, className, userEmail);
        }
    }

    /**
     * 예외 처리 여부 결정
     */
    private boolean shouldHandleException(Exception exception, AddressExceptionHandling exceptionHandling) {
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

        return true; // 기본적으로 모든 예외 처리
    }

    /**
     * AddressException 타입별 세부 처리
     */
    private void handleAddressException(AddressException addressException, String methodName, String userEmail, Object[] args) {
        AddressErrorCode errorCode = (AddressErrorCode) addressException.getErrorCode();

        switch (errorCode) {
            case ADDRESS_NOT_FOUND:
                log.warn("주소 미존재 - Method: {}, User: {}, Args: {}", methodName, userEmail, formatArgs(args));
                break;
            case ADDRESS_USER_NOT_FOUND:
                log.warn("주소 연관 사용자 미존재 - Method: {}, User: {}, Args: {}", methodName, userEmail, formatArgs(args));
                break;
            case ADDRESS_ACCESS_DENIED:
                log.warn("주소 접근 권한 없음 - Method: {}, User: {}, Args: {}", methodName, userEmail, formatArgs(args));
                break;
            case ADDRESS_CREATE_ERROR:
            case ADDRESS_UPDATE_ERROR:
            case ADDRESS_DELETE_ERROR:
            case ADDRESS_QUERY_ERROR:
                log.error("주소 작업 실패 - ErrorCode: {}, Method: {}, User: {}, Args: {}",
                        errorCode.getCode(), methodName, userEmail, formatArgs(args));
                break;
            case ADDRESS_INVALID_DATA:
            case ADDRESS_INVALID_ZIP_CODE:
            case ADDRESS_INVALID_PHONE_NUMBER:
                log.warn("주소 데이터 유효성 검증 실패 - ErrorCode: {}, Method: {}, User: {}, Args: {}",
                        errorCode.getCode(), methodName, userEmail, formatArgs(args));
                break;
            default:
                log.error("알 수 없는 주소 예외 - ErrorCode: {}, Method: {}, User: {}",
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
    private Object executeFallbackLogic(ProceedingJoinPoint proceedingJoinPoint, Exception exception) {
        // 메서드 시그니처에 따른 기본값 반환
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
        } else {
            return null;
        }
    }

    /**
     * 알림 전송
     */
    private void sendAlert(Exception exception, AddressExceptionHandling.AlertLevel alertLevel,
                           String methodName, String className, String userEmail) {
        // 실제 환경에서는 Slack, 이메일, SMS 등으로 알림 전송
        log.warn("ALERT[{}]: Address Exception - Class: {}, Method: {}, User: {}, Exception: {}",
                alertLevel, className, methodName, userEmail, exception.getMessage());
    }

    /**
     * 컨트롤러 예외 메트릭스 로깅
     */
    private void logControllerExceptionMetrics(String methodName, Exception exception, String userEmail) {
        log.info("METRICS: Address Controller Exception - Method: {}, Exception: {}, User: {}",
                methodName, exception.getClass().getSimpleName(), userEmail);
    }

    /**
     * 데이터베이스 예외 메트릭스 로깅
     */
    private void logDatabaseExceptionMetrics(String methodName, Exception exception) {
        log.info("METRICS: Address Database Exception - Method: {}, Exception: {}",
                methodName, exception.getClass().getSimpleName());
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
     * 메서드 파라미터를 안전하게 문자열로 변환
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
                    sb.append(maskEmail(str));
                } else if (str.matches("\\d{10,11}")) {
                    sb.append(maskPhoneNumber(str));
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
     * 전화번호 마스킹 처리
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }

        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 3);
    }
}