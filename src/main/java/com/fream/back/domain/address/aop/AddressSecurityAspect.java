package com.fream.back.domain.address.aop;

import com.fream.back.domain.address.aop.annotation.AddressSecurity;
import com.fream.back.domain.address.exception.AddressAccessDeniedException;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Address 도메인 보안 AOP
 * @AddressSecurity 어노테이션을 기반으로 보안 검증 수행
 */
@Aspect
@Component
@Slf4j
public class AddressSecurityAspect {

    // 속도 제한을 위한 요청 카운터 (실제 환경에서는 Redis 등 외부 저장소 사용 권장)
    private final ConcurrentHashMap<String, AtomicInteger> rateLimitCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> rateLimitWindows = new ConcurrentHashMap<>();

    /**
     * @AddressSecurity 어노테이션이 붙은 메서드의 보안 검증
     *
     * @param proceedingJoinPoint 조인포인트
     * @param addressSecurity 보안 어노테이션
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("@annotation(addressSecurity)")
    public Object secureAnnotatedMethod(ProceedingJoinPoint proceedingJoinPoint,
                                        AddressSecurity addressSecurity) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = extractUserEmailSafely();
        HttpServletRequest request = getCurrentRequest();

        try {
            // 보안 검증 수행
            performSecurityChecks(addressSecurity, className, methodName, userEmail, request, args);

            // 보안 검증 통과 시 메서드 실행
            Object result = proceedingJoinPoint.proceed();

            // 성공적인 보안 검증 로깅
            if (addressSecurity.enableAuditLog()) {
                logSecuritySuccess(className, methodName, userEmail, request);
            }

            return result;

        } catch (Exception e) {
            // 보안 위반 처리
            handleSecurityViolation(addressSecurity, className, methodName, userEmail, request, e, args);
            throw e; // 원래 예외 다시 던지기
        }
    }

    /**
     * 보안 검증 수행
     */
    private void performSecurityChecks(AddressSecurity addressSecurity, String className, String methodName,
                                       String userEmail, HttpServletRequest request, Object[] args) {

        for (AddressSecurity.SecurityCheck check : addressSecurity.checks()) {
            switch (check) {
                case AUTHENTICATION:
                    checkAuthentication(userEmail, className, methodName);
                    break;

                case AUTHORIZATION:
                    checkAuthorization(addressSecurity.requiredRoles(), userEmail, className, methodName);
                    break;

                case OWNERSHIP:
                    if (addressSecurity.ownerOnly()) {
                        checkOwnership(addressSecurity.ownerParamIndex(), userEmail, args, className, methodName);
                    }
                    break;

                case IP_RESTRICTION:
                    if (addressSecurity.checkIpRestriction()) {
                        checkIpRestriction(addressSecurity.allowedIpPatterns(), request, className, methodName);
                    }
                    break;

                case TIME_RESTRICTION:
                    if (addressSecurity.checkTimeRestriction()) {
                        checkTimeRestriction(addressSecurity.allowedTimeRange(), className, methodName);
                    }
                    break;

                case RATE_LIMIT:
                    if (addressSecurity.enableRateLimit()) {
                        checkRateLimit(addressSecurity.rateLimitWindow(), addressSecurity.maxRequestsPerWindow(),
                                userEmail, className, methodName);
                    }
                    break;

                case DATA_ENCRYPTION:
                    if (addressSecurity.accessesEncryptedData()) {
                        logEncryptedDataAccess(userEmail, className, methodName, args);
                    }
                    break;

                case AUDIT_TRAIL:
                    logAuditTrail(addressSecurity, userEmail, className, methodName, request);
                    break;
            }
        }

        // 개인정보 접근 로깅
        if (addressSecurity.accessesPersonalData()) {
            logPersonalDataAccess(addressSecurity.processingPurposes(), userEmail, className, methodName, request);
        }
    }

    /**
     * 인증 확인
     */
    private void checkAuthentication(String userEmail, String className, String methodName) {
        if ("anonymous".equals(userEmail) || "unknown".equals(userEmail)) {
            log.warn("SECURITY_VIOLATION - Authentication failed - Class: {}, Method: {}, User: {}",
                    className, methodName, userEmail);
            throw new AddressAccessDeniedException("인증이 필요합니다. 로그인 후 다시 시도해주세요.");
        }

        log.debug("SECURITY_CHECK - Authentication passed - Class: {}, Method: {}, User: {}",
                className, methodName, userEmail);
    }

    /**
     * 인가 확인
     */
    private void checkAuthorization(String[] requiredRoles, String userEmail, String className, String methodName) {
        if (requiredRoles.length == 0) {
            return; // 필요한 권한이 없으면 통과
        }

        // 실제 환경에서는 Spring Security Context나 JWT 토큰에서 권한 정보를 가져와야 함
        // 여기서는 예시로 기본 권한 체크 로직만 구현
        boolean hasRequiredRole = checkUserRoles(userEmail, requiredRoles);

        if (!hasRequiredRole) {
            log.warn("SECURITY_VIOLATION - Authorization failed - Class: {}, Method: {}, User: {}, RequiredRoles: {}",
                    className, methodName, userEmail, Arrays.toString(requiredRoles));
            throw new AddressAccessDeniedException("해당 작업에 대한 권한이 없습니다.");
        }

        log.debug("SECURITY_CHECK - Authorization passed - Class: {}, Method: {}, User: {}, Roles: {}",
                className, methodName, userEmail, Arrays.toString(requiredRoles));
    }

    /**
     * 소유권 확인
     */
    private void checkOwnership(int ownerParamIndex, String userEmail, Object[] args, String className, String methodName) {
        if (args == null || args.length <= ownerParamIndex) {
            log.warn("SECURITY_VIOLATION - Ownership check failed (invalid param index) - Class: {}, Method: {}, User: {}",
                    className, methodName, userEmail);
            throw new AddressAccessDeniedException("소유권 확인에 실패했습니다.");
        }

        Object ownerParam = args[ownerParamIndex];
        if (ownerParam == null) {
            log.warn("SECURITY_VIOLATION - Ownership check failed (null owner param) - Class: {}, Method: {}, User: {}",
                    className, methodName, userEmail);
            throw new AddressAccessDeniedException("소유권 확인에 실패했습니다.");
        }

        String paramEmail = ownerParam.toString();
        if (!userEmail.equals(paramEmail)) {
            log.warn("SECURITY_VIOLATION - Ownership check failed - Class: {}, Method: {}, User: {}, OwnerParam: {}",
                    className, methodName, userEmail, maskEmail(paramEmail));
            throw new AddressAccessDeniedException("본인의 주소에만 접근할 수 있습니다.");
        }

        log.debug("SECURITY_CHECK - Ownership passed - Class: {}, Method: {}, User: {}",
                className, methodName, userEmail);
    }

    /**
     * IP 제한 확인
     */
    private void checkIpRestriction(String[] allowedIpPatterns, HttpServletRequest request, String className, String methodName) {
        if (allowedIpPatterns.length == 0 || request == null) {
            return;
        }

        String clientIp = extractIpAddress(request);
        if (clientIp == null) {
            log.warn("SECURITY_VIOLATION - IP restriction failed (no IP) - Class: {}, Method: {}",
                    className, methodName);
            throw new AddressAccessDeniedException("IP 주소를 확인할 수 없습니다.");
        }

        boolean ipAllowed = Arrays.stream(allowedIpPatterns)
                .anyMatch(pattern -> Pattern.matches(pattern, clientIp));

        if (!ipAllowed) {
            log.warn("SECURITY_VIOLATION - IP restriction failed - Class: {}, Method: {}, ClientIP: {}, AllowedPatterns: {}",
                    className, methodName, clientIp, Arrays.toString(allowedIpPatterns));
            throw new AddressAccessDeniedException("허용되지 않은 IP 주소에서의 접근입니다.");
        }

        log.debug("SECURITY_CHECK - IP restriction passed - Class: {}, Method: {}, ClientIP: {}",
                className, methodName, clientIp);
    }

    /**
     * 시간 제한 확인
     */
    private void checkTimeRestriction(String allowedTimeRange, String className, String methodName) {
        if (allowedTimeRange.isEmpty()) {
            return;
        }

        LocalTime currentTime = LocalTime.now();

        try {
            String[] timeParts = allowedTimeRange.split("-");
            if (timeParts.length != 2) {
                log.error("SECURITY_CONFIG_ERROR - Invalid time range format: {}", allowedTimeRange);
                return;
            }

            LocalTime startTime = LocalTime.parse(timeParts[0], DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(timeParts[1], DateTimeFormatter.ofPattern("HH:mm"));

            boolean timeAllowed;
            if (startTime.isBefore(endTime)) {
                // 같은 날 범위 (예: 09:00-18:00)
                timeAllowed = !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
            } else {
                // 자정을 넘는 범위 (예: 22:00-06:00)
                timeAllowed = !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
            }

            if (!timeAllowed) {
                log.warn("SECURITY_VIOLATION - Time restriction failed - Class: {}, Method: {}, CurrentTime: {}, AllowedRange: {}",
                        className, methodName, currentTime, allowedTimeRange);
                throw new AddressAccessDeniedException("허용된 시간이 아닙니다. 접근 가능 시간: " + allowedTimeRange);
            }

            log.debug("SECURITY_CHECK - Time restriction passed - Class: {}, Method: {}, CurrentTime: {}",
                    className, methodName, currentTime);

        } catch (Exception e) {
            log.error("SECURITY_CONFIG_ERROR - Time restriction check failed: {}", e.getMessage());
        }
    }

    /**
     * 속도 제한 확인
     */
    private void checkRateLimit(int rateLimitWindow, int maxRequestsPerWindow, String userEmail,
                                String className, String methodName) {
        String rateLimitKey = userEmail + ":" + className + ":" + methodName;
        long currentTime = System.currentTimeMillis();
        long windowStart = (currentTime / (rateLimitWindow * 1000L)) * (rateLimitWindow * 1000L);

        // 윈도우 시작 시간 확인 및 카운터 리셋
        Long lastWindowStart = rateLimitWindows.get(rateLimitKey);
        if (lastWindowStart == null || lastWindowStart < windowStart) {
            rateLimitWindows.put(rateLimitKey, windowStart);
            rateLimitCounters.put(rateLimitKey, new AtomicInteger(0));
        }

        // 현재 요청 카운트 증가
        AtomicInteger counter = rateLimitCounters.get(rateLimitKey);
        int currentCount = counter.incrementAndGet();

        if (currentCount > maxRequestsPerWindow) {
            log.warn("SECURITY_VIOLATION - Rate limit exceeded - Class: {}, Method: {}, User: {}, " +
                            "Count: {}/{}, Window: {}s",
                    className, methodName, userEmail, currentCount, maxRequestsPerWindow, rateLimitWindow);
            throw new AddressAccessDeniedException(
                    String.format("요청 한도를 초과했습니다. %d초 후 다시 시도해주세요.", rateLimitWindow));
        }

        log.debug("SECURITY_CHECK - Rate limit passed - Class: {}, Method: {}, User: {}, Count: {}/{}",
                className, methodName, userEmail, currentCount, maxRequestsPerWindow);
    }

    /**
     * 암호화된 데이터 접근 로깅
     */
    private void logEncryptedDataAccess(String userEmail, String className, String methodName, Object[] args) {
        log.info("ENCRYPTED_DATA_ACCESS - Class: {}, Method: {}, User: {}, Args: {}",
                className, methodName, userEmail, formatArgsForLogging(args));
    }

    /**
     * 감사 추적 로깅
     */
    private void logAuditTrail(AddressSecurity addressSecurity, String userEmail, String className,
                               String methodName, HttpServletRequest request) {
        String clientIp = extractIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        log.info("SECURITY_AUDIT_TRAIL - Class: {}, Method: {}, User: {}, IP: {}, UserAgent: {}",
                className, methodName, userEmail, clientIp,
                userAgent != null ? maskUserAgent(userAgent) : "unknown");
    }

    /**
     * 개인정보 접근 로깅
     */
    private void logPersonalDataAccess(String[] processingPurposes, String userEmail, String className,
                                       String methodName, HttpServletRequest request) {
        String clientIp = extractIpAddress(request);

        log.info("PERSONAL_DATA_ACCESS - Class: {}, Method: {}, User: {}, IP: {}, Purposes: {}",
                className, methodName, userEmail, clientIp,
                processingPurposes.length > 0 ? Arrays.toString(processingPurposes) : "unspecified");
    }

    /**
     * 보안 위반 처리
     */
    private void handleSecurityViolation(AddressSecurity addressSecurity, String className, String methodName,
                                         String userEmail, HttpServletRequest request, Exception exception, Object[] args) {
        String clientIp = extractIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        // 보안 위반 로깅
        log.error("SECURITY_VIOLATION - Class: {}, Method: {}, User: {}, IP: {}, Exception: {}, Args: {}",
                className, methodName, userEmail, clientIp, exception.getMessage(), formatArgsForLogging(args));

        // 위반 처리 전략 실행
        switch (addressSecurity.violationAction()) {
            case THROW_EXCEPTION:
                // 예외는 이미 던져질 예정이므로 추가 처리 없음
                break;

            case LOG_AND_DENY:
                log.warn("SECURITY_DENY - Access denied with logging - Class: {}, Method: {}, User: {}",
                        className, methodName, userEmail);
                break;

            case LOG_AND_ALLOW:
                log.warn("SECURITY_ALLOW - Access allowed with warning - Class: {}, Method: {}, User: {}",
                        className, methodName, userEmail);
                // 실제로는 예외를 억제해야 하지만, 이 경우는 특별한 처리가 필요
                break;

            case SILENT_DENY:
                // 조용히 거부 (로그 최소화)
                log.debug("SECURITY_SILENT_DENY - Class: {}, Method: {}, User: {}",
                        className, methodName, userEmail);
                break;

            case REDIRECT:
                log.info("SECURITY_REDIRECT - Redirecting user - Class: {}, Method: {}, User: {}",
                        className, methodName, userEmail);
                // 실제 환경에서는 리다이렉트 처리
                break;
        }

        // 보안 감사 로그 활성화 시 상세 로깅
        if (addressSecurity.enableAuditLog()) {
            logDetailedSecurityViolation(className, methodName, userEmail, clientIp, userAgent, exception, args);
        }
    }

    /**
     * 상세 보안 위반 로깅
     */
    private void logDetailedSecurityViolation(String className, String methodName, String userEmail,
                                              String clientIp, String userAgent, Exception exception, Object[] args) {
        log.error("DETAILED_SECURITY_VIOLATION - Class: {}, Method: {}, User: {}, IP: {}, UserAgent: {}, " +
                        "Exception: {}, StackTrace: {}, Args: {}",
                className, methodName, userEmail, clientIp,
                userAgent != null ? maskUserAgent(userAgent) : "unknown",
                exception.getClass().getSimpleName(),
                exception.getStackTrace().length > 0 ? exception.getStackTrace()[0].toString() : "none",
                formatArgsForLogging(args));
    }

    /**
     * 보안 검증 성공 로깅
     */
    private void logSecuritySuccess(String className, String methodName, String userEmail, HttpServletRequest request) {
        String clientIp = extractIpAddress(request);

        log.debug("SECURITY_SUCCESS - All security checks passed - Class: {}, Method: {}, User: {}, IP: {}",
                className, methodName, userEmail, clientIp);
    }

    /**
     * 사용자 권한 확인 (실제 환경에서는 Spring Security Context 사용)
     */
    private boolean checkUserRoles(String userEmail, String[] requiredRoles) {
        // 실제 환경에서는 다음과 같이 구현:
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        // return authorities.stream().anyMatch(auth -> Arrays.asList(requiredRoles).contains(auth.getAuthority()));

        // 임시 구현: 관리자는 모든 권한, 일반 사용자는 USER 권한만
        if (userEmail.contains("admin")) {
            return true; // 관리자는 모든 권한 보유
        } else {
            return Arrays.asList(requiredRoles).contains("USER") ||
                    Arrays.asList(requiredRoles).contains("ADDRESS_USER");
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

        // X-Forwarded-For 헤더 확인 (프록시/로드밸런서 환경)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // X-Real-IP 헤더 확인
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // 직접 연결된 클라이언트 IP
        return request.getRemoteAddr();
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
     * User-Agent 마스킹
     */
    private String maskUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() < 10) {
            return "***";
        }

        return userAgent.substring(0, 30) + "***";
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
                    sb.append("phone:***");
                } else if (str.matches("\\d{5}")) {
                    sb.append("zipCode:***");
                } else if (str.length() > 50) {
                    sb.append("longString:***");
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
}