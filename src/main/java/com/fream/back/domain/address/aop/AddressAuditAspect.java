package com.fream.back.domain.address.aop;

import com.fream.back.domain.address.aop.annotation.AddressAudit;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Address 도메인 감사 추적 AOP
 * @AddressAudit 어노테이션을 기반으로 감사 로그 수행
 */
@Aspect
@Component
@Slf4j
public class AddressAuditAspect {

    private static final DateTimeFormatter AUDIT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * @AddressAudit 어노테이션이 붙은 메서드의 감사 로그 처리
     *
     * @param proceedingJoinPoint 조인포인트
     * @param addressAudit 감사 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("@annotation(addressAudit)")
    public Object auditAnnotatedMethod(ProceedingJoinPoint proceedingJoinPoint,
                                       AddressAudit addressAudit) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = extractUserEmailSafely();
        String auditId = generateAuditId();
        LocalDateTime startTime = LocalDateTime.now();

        // HTTP 요청 정보 추출
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request, addressAudit);
        Map<String, String> headers = extractHeaders(request, addressAudit);

        // 감사 로그 시작
        logAuditStart(addressAudit, className, methodName, userEmail, auditId,
                startTime, ipAddress, userAgent, headers, args);

        try {
            Object result = proceedingJoinPoint.proceed();

            LocalDateTime endTime = LocalDateTime.now();

            // 감사 로그 성공
            logAuditSuccess(addressAudit, className, methodName, userEmail, auditId,
                    startTime, endTime, ipAddress, result);

            // 변경 추적
            if (addressAudit.trackChanges()) {
                logChangeTracking(addressAudit, methodName, userEmail, args, result);
            }

            // 외부 시스템 연동
            if (addressAudit.sendToExternalSystem()) {
                sendToExternalAuditSystem(addressAudit, className, methodName, userEmail, result);
            }

            // 실시간 알림
            if (addressAudit.realTimeAlert()) {
                sendRealTimeAlert(addressAudit, className, methodName, userEmail, "SUCCESS");
            }

            return result;

        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();

            // 감사 로그 실패
            logAuditFailure(addressAudit, className, methodName, userEmail, auditId,
                    startTime, endTime, ipAddress, e);

            // 실시간 알림 (실패)
            if (addressAudit.realTimeAlert()) {
                sendRealTimeAlert(addressAudit, className, methodName, userEmail, "FAILED");
            }

            throw e;
        }
    }

    /**
     * 주소 생성 감사 로그
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.command.AddressCommandService.createAddress(..))", returning = "result")
    public void auditAddressCreation(JoinPoint joinPoint, Object result) {
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        logBusinessAudit(AddressAudit.AuditEvent.ADDRESS_CREATE, "주소 생성",
                userEmail, args, result, AddressAudit.AuditLevel.INFO);
    }

    /**
     * 주소 수정 감사 로그
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.command.AddressCommandService.updateAddress(..))", returning = "result")
    public void auditAddressUpdate(JoinPoint joinPoint, Object result) {
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        logBusinessAudit(AddressAudit.AuditEvent.ADDRESS_UPDATE, "주소 수정",
                userEmail, args, result, AddressAudit.AuditLevel.INFO);
    }

    /**
     * 주소 삭제 감사 로그
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.command.AddressCommandService.deleteAddress(..))", returning = "result")
    public void auditAddressDeletion(JoinPoint joinPoint, Object result) {
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        logBusinessAudit(AddressAudit.AuditEvent.ADDRESS_DELETE, "주소 삭제",
                userEmail, args, result, AddressAudit.AuditLevel.WARN);
    }

    /**
     * 주소 조회 감사 로그 (민감한 정보 접근)
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.address.service.query.AddressQueryService.*(..))", returning = "result")
    public void auditAddressAccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        AddressAudit.AuditEvent auditEvent;
        if (methodName.contains("getAddresses")) {
            auditEvent = AddressAudit.AuditEvent.ADDRESS_LIST_VIEW;
        } else if (methodName.contains("search")) {
            auditEvent = AddressAudit.AuditEvent.ADDRESS_SEARCH;
        } else {
            auditEvent = AddressAudit.AuditEvent.ADDRESS_VIEW;
        }

        logBusinessAudit(auditEvent, auditEvent.getDescription(),
                userEmail, args, result, AddressAudit.AuditLevel.DEBUG);
    }

    /**
     * 보안 위반 감사 로그
     */
    @AfterThrowing(pointcut = "execution(* com.fream.back.domain.address..*(..))", throwing = "exception")
    public void auditSecurityViolation(JoinPoint joinPoint, Exception exception) {
        if (isSecurityViolation(exception)) {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String userEmail = extractUserEmailSafely();
            Object[] args = joinPoint.getArgs();

            logSecurityAudit(AddressAudit.AuditEvent.SECURITY_VIOLATION,
                    className + "." + methodName, userEmail, args, exception);
        }
    }

    /**
     * 감사 로그 시작
     */
    private void logAuditStart(AddressAudit addressAudit, String className, String methodName,
                               String userEmail, String auditId, LocalDateTime startTime,
                               String ipAddress, String userAgent, Map<String, String> headers, Object[] args) {

        StringBuilder auditLog = new StringBuilder();
        auditLog.append(String.format("AUDIT_START - [%s] Event: %s, User: %s, Method: %s.%s, Time: %s",
                auditId, addressAudit.event().name(), userEmail, className, methodName,
                startTime.format(AUDIT_DATE_FORMAT)));

        if (addressAudit.recordIpAddress() && ipAddress != null) {
            auditLog.append(", IP: ").append(ipAddress);
        }

        if (addressAudit.recordUserAgent() && userAgent != null) {
            auditLog.append(", UserAgent: ").append(userAgent);
        }

        if (addressAudit.includeBusinessContext() && args != null) {
            auditLog.append(", Context: ").append(formatBusinessContext(args, addressAudit.containsSensitiveData()));
        }

        if (addressAudit.recordHeaders() && !headers.isEmpty()) {
            auditLog.append(", Headers: ").append(headers);
        }

        String description = addressAudit.description().isEmpty()
                ? addressAudit.event().getDescription()
                : addressAudit.description();
        auditLog.append(", Description: ").append(description);

        logWithAuditLevel(addressAudit.level(), auditLog.toString());
    }

    /**
     * 감사 로그 성공
     */
    private void logAuditSuccess(AddressAudit addressAudit, String className, String methodName,
                                 String userEmail, String auditId, LocalDateTime startTime, LocalDateTime endTime,
                                 String ipAddress, Object result) {

        long executionTime = java.time.Duration.between(startTime, endTime).toMillis();

        StringBuilder auditLog = new StringBuilder();
        auditLog.append(String.format("AUDIT_SUCCESS - [%s] Event: %s, User: %s, Method: %s.%s, " +
                        "StartTime: %s, EndTime: %s, Duration: %dms",
                auditId, addressAudit.event().name(), userEmail, className, methodName,
                startTime.format(AUDIT_DATE_FORMAT), endTime.format(AUDIT_DATE_FORMAT), executionTime));

        if (addressAudit.recordIpAddress() && ipAddress != null) {
            auditLog.append(", IP: ").append(ipAddress);
        }

        // 결과 크기 정보 (개인정보는 로그하지 않음)
        if (result != null) {
            if (result instanceof java.util.Collection) {
                auditLog.append(", ResultCount: ").append(((java.util.Collection<?>) result).size());
            } else {
                auditLog.append(", ResultType: ").append(result.getClass().getSimpleName());
            }
        }

        logWithAuditLevel(addressAudit.level(), auditLog.toString());

        // 보존 기간 정보 (향후 자동 삭제를 위한 메타데이터)
        if (addressAudit.retentionDays() > 0) {
            LocalDateTime deleteAfter = endTime.plusDays(addressAudit.retentionDays());
            log.debug("AUDIT_RETENTION - [{}] DeleteAfter: {}", auditId, deleteAfter.format(AUDIT_DATE_FORMAT));
        }
    }

    /**
     * 감사 로그 실패
     */
    private void logAuditFailure(AddressAudit addressAudit, String className, String methodName,
                                 String userEmail, String auditId, LocalDateTime startTime, LocalDateTime endTime,
                                 String ipAddress, Exception exception) {

        long executionTime = java.time.Duration.between(startTime, endTime).toMillis();

        StringBuilder auditLog = new StringBuilder();
        auditLog.append(String.format("AUDIT_FAILED - [%s] Event: %s, User: %s, Method: %s.%s, " +
                        "StartTime: %s, EndTime: %s, Duration: %dms, Error: %s",
                auditId, addressAudit.event().name(), userEmail, className, methodName,
                startTime.format(AUDIT_DATE_FORMAT), endTime.format(AUDIT_DATE_FORMAT),
                executionTime, exception.getMessage()));

        if (addressAudit.recordIpAddress() && ipAddress != null) {
            auditLog.append(", IP: ").append(ipAddress);
        }

        // 실패의 경우 ERROR 레벨로 로깅
        log.error(auditLog.toString());
    }

    /**
     * 비즈니스 감사 로그
     */
    private void logBusinessAudit(AddressAudit.AuditEvent event, String description,
                                  String userEmail, Object[] args, Object result, AddressAudit.AuditLevel level) {
        String auditId = generateAuditId();
        LocalDateTime auditTime = LocalDateTime.now();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = extractIpAddress(request);

        StringBuilder auditLog = new StringBuilder();
        auditLog.append(String.format("BUSINESS_AUDIT - [%s] Event: %s, User: %s, Time: %s, Description: %s",
                auditId, event.name(), userEmail, auditTime.format(AUDIT_DATE_FORMAT), description));

        if (ipAddress != null) {
            auditLog.append(", IP: ").append(ipAddress);
        }

        if (args != null && args.length > 0) {
            auditLog.append(", Context: ").append(formatBusinessContext(args, true));
        }

        if (result instanceof java.util.Collection) {
            auditLog.append(", ResultCount: ").append(((java.util.Collection<?>) result).size());
        }

        logWithAuditLevel(level, auditLog.toString());
    }

    /**
     * 보안 감사 로그
     */
    private void logSecurityAudit(AddressAudit.AuditEvent event, String method,
                                  String userEmail, Object[] args, Exception exception) {
        String auditId = generateAuditId();
        LocalDateTime auditTime = LocalDateTime.now();
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request, null);

        StringBuilder auditLog = new StringBuilder();
        auditLog.append(String.format("SECURITY_AUDIT - [%s] Event: %s, User: %s, Method: %s, Time: %s, " +
                        "Violation: %s",
                auditId, event.name(), userEmail, method, auditTime.format(AUDIT_DATE_FORMAT),
                exception.getClass().getSimpleName()));

        if (ipAddress != null) {
            auditLog.append(", IP: ").append(ipAddress);
        }

        if (userAgent != null) {
            auditLog.append(", UserAgent: ").append(maskUserAgent(userAgent));
        }

        // 보안 위반은 항상 SECURITY 레벨로 로깅
        log.error(auditLog.toString());

        // 즉시 알림 (실제 환경에서는 보안팀에 알림)
        log.warn("SECURITY_ALERT - Potential security violation detected: User={}, IP={}, Method={}",
                userEmail, ipAddress, method);
    }

    /**
     * 변경 추적 로깅
     */
    private void logChangeTracking(AddressAudit addressAudit, String methodName,
                                   String userEmail, Object[] args, Object result) {
        // 변경 전후 데이터 비교 (실제 구현에서는 더 정교한 비교 로직 필요)
        String auditId = generateAuditId();

        log.info("CHANGE_TRACKING - [{}] User: {}, Method: {}, Changes: [BeforeArgs: {}, AfterResult: {}]",
                auditId, userEmail, methodName,
                formatBusinessContext(args, true),
                result != null ? result.getClass().getSimpleName() : "null");
    }

    /**
     * 비즈니스 컨텍스트 포맷팅
     */
    private String formatBusinessContext(Object[] args, boolean containsSensitiveData) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String && containsSensitiveData) {
                sb.append(formatSensitiveString((String) arg));
            } else if (arg instanceof Long) {
                sb.append("id:").append(arg);
            } else {
                sb.append(arg.getClass().getSimpleName()).append("@").append(Integer.toHexString(arg.hashCode()));
            }
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 민감한 문자열 포맷팅
     */
    private String formatSensitiveString(String str) {
        if (str.contains("@")) {
            return "email:" + maskEmail(str);
        } else if (str.matches("\\d{10,11}")) {
            return "phone:" + maskPhoneNumber(str);
        } else if (str.matches("\\d{5}")) {
            return "zipCode:" + maskZipCode(str);
        } else {
            return "string:" + (str.length() > 10 ? str.substring(0, 3) + "***" : "***");
        }
    }

    /**
     * 보안 위반 여부 확인
     */
    private boolean isSecurityViolation(Exception exception) {
        String exceptionType = exception.getClass().getSimpleName();
        return exceptionType.contains("Access") ||
                exceptionType.contains("Security") ||
                exceptionType.contains("Authentication") ||
                exceptionType.contains("Authorization") ||
                exception.getMessage().toLowerCase().contains("access denied");
    }

    /**
     * 외부 감사 시스템으로 전송
     */
    private void sendToExternalAuditSystem(AddressAudit addressAudit, String className,
                                           String methodName, String userEmail, Object result) {
        // 실제 환경에서는 외부 SIEM, ELK Stack, Splunk 등으로 전송
        log.info("EXTERNAL_AUDIT - Event: {}, Class: {}, Method: {}, User: {}, Sent to external system",
                addressAudit.event().name(), className, methodName, userEmail);
    }

    /**
     * 실시간 알림 전송
     */
    private void sendRealTimeAlert(AddressAudit addressAudit, String className,
                                   String methodName, String userEmail, String status) {
        // 실제 환경에서는 Slack, Teams, 이메일 등으로 알림 전송
        log.warn("REAL_TIME_ALERT - Event: {}, Class: {}, Method: {}, User: {}, Status: {}",
                addressAudit.event().name(), className, methodName, userEmail, status);
    }

    /**
     * 감사 레벨에 따른 로깅
     */
    private void logWithAuditLevel(AddressAudit.AuditLevel level, String message) {
        switch (level) {
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
            case SECURITY:
            case COMPLIANCE:
                log.error("[{}] {}", level, message);
                break;
        }
    }

    /**
     * 현재 HTTP 요청 추출
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("HTTP 요청 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * IP 주소 추출
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) return null;

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * User-Agent 추출
     */
    private String extractUserAgent(HttpServletRequest request, AddressAudit addressAudit) {
        if (request == null || (addressAudit != null && !addressAudit.recordUserAgent())) {
            return null;
        }

        return request.getHeader("User-Agent");
    }

    /**
     * HTTP 헤더 추출
     */
    private Map<String, String> extractHeaders(HttpServletRequest request, AddressAudit addressAudit) {
        Map<String, String> headers = new HashMap<>();

        if (request == null || !addressAudit.recordHeaders()) {
            return headers;
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);

            // 민감한 헤더는 마스킹
            if (isSensitiveHeader(headerName)) {
                headerValue = "***";
            }

            headers.put(headerName, headerValue);
        }

        return headers;
    }

    /**
     * 민감한 헤더인지 확인
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.contains("authorization") ||
                lowerHeaderName.contains("cookie") ||
                lowerHeaderName.contains("token") ||
                lowerHeaderName.contains("password");
    }

    /**
     * User-Agent 마스킹
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() < 10) {
            return "***";
        }

        return userAgent.substring(0, 20) + "***";
    }

    /**
     * 감사 ID 생성
     */
    private String generateAuditId() {
        return "AUDIT-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
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