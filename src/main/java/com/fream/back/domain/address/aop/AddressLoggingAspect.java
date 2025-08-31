package com.fream.back.domain.address.aop;

import com.fream.back.domain.address.aop.annotation.AddressLogging;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Address 도메인 로깅 AOP
 * @AddressLogging 어노테이션을 기반으로 로깅 수행
 */
@Aspect
@Component
@Slf4j
public class AddressLoggingAspect {

    /**
     * @AddressLogging 어노테이션이 붙은 메서드의 로깅 처리
     *
     * @param proceedingJoinPoint 조인포인트
     * @param addressLogging 로깅 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("@annotation(addressLogging)")
    public Object logAnnotatedMethod(ProceedingJoinPoint proceedingJoinPoint,
                                     AddressLogging addressLogging) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = extractUserEmailSafely();
        String requestId = generateRequestId();

        long startTime = System.currentTimeMillis();

        try {
            // 메서드 실행 전 로깅
            if (addressLogging.logBefore()) {
                logMethodStart(addressLogging, className, methodName, userEmail, requestId, args);
            }

            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 메서드 실행 후 로깅
            if (addressLogging.logAfter()) {
                logMethodSuccess(addressLogging, className, methodName, userEmail, requestId,
                        executionTime, result);
            }

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 예외 발생 시 로깅
            if (addressLogging.logException()) {
                logMethodException(addressLogging, className, methodName, userEmail, requestId,
                        executionTime, e);
            }

            throw e;
        }
    }

    /**
     * Address Controller 메서드 기본 로깅
     */
    @Around("execution(* com.fream.back.domain.address.controller..*(..))")
    public Object logControllerMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = extractUserEmailSafely();
        String requestId = generateRequestId();

        long startTime = System.currentTimeMillis();

        log.info("REQUEST_START - [{}] Address Controller - Class: {}, Method: {}, User: {}, Args: {}",
                requestId, className, methodName, userEmail, formatArgsForLogging(args));

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("REQUEST_SUCCESS - [{}] Address Controller - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms",
                    requestId, className, methodName, userEmail, executionTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("REQUEST_FAILED - [{}] Address Controller - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Error: {}",
                    requestId, className, methodName, userEmail, executionTime, e.getMessage());

            throw e;
        }
    }

    /**
     * Address Service 메서드 기본 로깅
     */
    @Around("execution(* com.fream.back.domain.address.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        long startTime = System.currentTimeMillis();

        log.debug("SERVICE_START - Address Service - Class: {}, Method: {}, User: {}, Args: {}",
                className, methodName, userEmail, formatArgsForLogging(args));

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.debug("SERVICE_SUCCESS - Address Service - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms",
                    className, methodName, userEmail, executionTime);

            if (isImportantServiceMethod(methodName)) {
                log.info("IMPORTANT_SERVICE - Address Service - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms",
                        className, methodName, userEmail, executionTime);
            }

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("SERVICE_FAILED - Address Service - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Error: {}",
                    className, methodName, userEmail, executionTime, e.getMessage());

            throw e;
        }
    }

    /**
     * Address Repository 메서드 기본 로깅
     */
    @Around("execution(* com.fream.back.domain.address.repository..*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = extractUserEmailSafely();

        long startTime = System.currentTimeMillis();

        log.trace("REPOSITORY_START - Address Repository - Class: {}, Method: {}, User: {}, Args: {}",
                className, methodName, userEmail, formatArgsForLogging(args));

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.trace("REPOSITORY_SUCCESS - Address Repository - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms",
                    className, methodName, userEmail, executionTime);

            if (executionTime > 1000) {
                log.warn("SLOW_QUERY - Address Repository - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms",
                        className, methodName, userEmail, executionTime);
            }

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("REPOSITORY_FAILED - Address Repository - Class: {}, Method: {}, User: {}, ExecutionTime: {}ms, Error: {}",
                    className, methodName, userEmail, executionTime, e.getMessage());

            throw e;
        }
    }

    /**
     * 주소 생성 성공 후 로깅
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.command.AddressCommandService.createAddress(..))", returning = "result")
    public void logAddressCreation(JoinPoint joinPoint, Object result) {
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        log.info("ADDRESS_CREATED - User: {}, Method: createAddress, Args: {}",
                userEmail, formatArgsForLogging(args));
    }

    /**
     * 주소 수정 성공 후 로깅
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.command.AddressCommandService.updateAddress(..))", returning = "result")
    public void logAddressUpdate(JoinPoint joinPoint, Object result) {
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        log.info("ADDRESS_UPDATED - User: {}, Method: updateAddress, Args: {}",
                userEmail, formatArgsForLogging(args));
    }

    /**
     * 주소 삭제 성공 후 로깅
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.command.AddressCommandService.deleteAddress(..))", returning = "result")
    public void logAddressDeletion(JoinPoint joinPoint, Object result) {
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        log.info("ADDRESS_DELETED - User: {}, Method: deleteAddress, Args: {}",
                userEmail, formatArgsForLogging(args));
    }

    /**
     * 메서드 시작 로깅
     */
    private void logMethodStart(AddressLogging addressLogging, String className, String methodName,
                                String userEmail, String requestId, Object[] args) {
        String customMessage = addressLogging.message().isEmpty()
                ? String.format("METHOD_START - [%s] Address - Class: %s, Method: %s, User: %s",
                requestId, className, methodName, userEmail)
                : addressLogging.message();

        String argsString = "";
        if (Arrays.asList(addressLogging.types()).contains(AddressLogging.LogType.PARAMETERS)) {
            argsString = ", Args: " + formatArgsForAnnotation(args, addressLogging);
        }

        logWithLevel(addressLogging.level(), customMessage + argsString);
    }

    /**
     * 메서드 성공 로깅
     */
    private void logMethodSuccess(AddressLogging addressLogging, String className, String methodName,
                                  String userEmail, String requestId, long executionTime, Object result) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("METHOD_SUCCESS - [%s] Address - Class: %s, Method: %s, User: %s",
                requestId, className, methodName, userEmail));

        if (Arrays.asList(addressLogging.types()).contains(AddressLogging.LogType.EXECUTION_TIME)) {
            logMessage.append(", ExecutionTime: ").append(executionTime).append("ms");
        }

        if (addressLogging.logResult() &&
                Arrays.asList(addressLogging.types()).contains(AddressLogging.LogType.RESULT)) {
            logMessage.append(", Result: ").append(formatResultForLogging(result));
        }

        logWithLevel(addressLogging.level(), logMessage.toString());
    }

    /**
     * 메서드 예외 로깅
     */
    private void logMethodException(AddressLogging addressLogging, String className, String methodName,
                                    String userEmail, String requestId, long executionTime, Exception e) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("METHOD_FAILED - [%s] Address - Class: %s, Method: %s, User: %s",
                requestId, className, methodName, userEmail));

        if (Arrays.asList(addressLogging.types()).contains(AddressLogging.LogType.EXECUTION_TIME)) {
            logMessage.append(", ExecutionTime: ").append(executionTime).append("ms");
        }

        logMessage.append(", Error: ").append(e.getMessage());

        // 예외의 경우 항상 ERROR 레벨로 로깅
        log.error(logMessage.toString(), e);
    }

    /**
     * 어노테이션 설정에 따른 파라미터 포맷팅
     */
    private String formatArgsForAnnotation(Object[] args, AddressLogging addressLogging) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        int[] includeParams = addressLogging.includeParams();
        int[] excludeParams = addressLogging.excludeParams();

        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (int i = 0; i < args.length; i++) {
            // includeParams가 설정되어 있으면 해당 인덱스만 포함
            if (includeParams.length > 0) {
                boolean included = Arrays.stream(includeParams).anyMatch(idx -> idx == i);
                if (!included) continue;
            }

            // excludeParams에 포함된 인덱스는 제외
            if (excludeParams.length > 0) {
                boolean excluded = Arrays.stream(excludeParams).anyMatch(idx -> idx == i);
                if (excluded) continue;
            }

            if (!first) sb.append(", ");
            first = false;

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else {
                if (addressLogging.maskSensitiveData()) {
                    sb.append(formatSensitiveArg(arg));
                } else {
                    sb.append(arg.toString());
                }
            }
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 민감한 데이터 포맷팅
     */
    private String formatSensitiveArg(Object arg) {
        if (arg instanceof String) {
            String str = (String) arg;
            if (str.contains("@")) {
                return "email:" + maskEmail(str);
            } else if (str.matches("\\d{10,11}")) {
                return "phone:" + maskPhoneNumber(str);
            } else if (str.matches("\\d{5}")) {
                return "zipCode:" + maskZipCode(str);
            }
        }
        return arg.toString();
    }

    /**
     * 결과값 포맷팅
     */
    private String formatResultForLogging(Object result) {
        if (result == null) {
            return "null";
        } else if (result instanceof java.util.List) {
            return "List[size=" + ((java.util.List<?>) result).size() + "]";
        } else if (result instanceof String) {
            String str = (String) result;
            return str.length() > 100 ? str.substring(0, 100) + "..." : str;
        } else {
            return result.getClass().getSimpleName() + "@" + Integer.toHexString(result.hashCode());
        }
    }

    /**
     * 로그 레벨에 따른 로깅
     */
    private void logWithLevel(AddressLogging.LogLevel level, String message) {
        switch (level) {
            case TRACE:
                log.trace(message);
                break;
            case DEBUG:
                log.debug(message);
                break;
            case INFO:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
        }
    }

    /**
     * 중요한 서비스 메서드인지 확인
     */
    private boolean isImportantServiceMethod(String methodName) {
        return methodName.equals("createAddress") ||
                methodName.equals("updateAddress") ||
                methodName.equals("deleteAddress") ||
                methodName.contains("search");
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
     * 요청 ID 생성
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 로깅용 메서드 파라미터 포맷팅 (개인정보 보호)
     */
    private String formatArgsForLogging(Object[] args) {
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
                } else if (str.matches("\\d{10,11}")) {
                    sb.append("phone:").append(maskPhoneNumber(str));
                } else if (str.matches("\\d{5}")) {
                    sb.append("zipCode:").append(maskZipCode(str));
                } else if (str.length() > 50) {
                    sb.append("longString:").append(str.substring(0, 20)).append("...");
                } else {
                    sb.append("'").append(str).append("'");
                }
            } else if (arg instanceof Long) {
                sb.append("id:").append(arg);
            } else if (arg instanceof Boolean) {
                sb.append("flag:").append(arg);
            } else {
                sb.append(arg.getClass().getSimpleName()).append("@").append(Integer.toHexString(arg.hashCode()));
            }
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 이메일 마스킹
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
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }

        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 3);
    }

    /**
     * 우편번호 마스킹
     */
    private String maskZipCode(String zipCode) {
        if (zipCode == null || zipCode.length() != 5) {
            return "***";
        }

        return zipCode.substring(0, 2) + "***";
    }
}