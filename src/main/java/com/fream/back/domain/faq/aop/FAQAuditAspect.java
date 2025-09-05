package com.fream.back.domain.faq.aop;

import com.fream.back.domain.faq.aop.annotation.FAQAudit;
import com.fream.back.domain.faq.aop.annotation.FAQAudit.AuditEvent;
import com.fream.back.domain.faq.aop.annotation.FAQAudit.AuditLevel;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FAQAuditAspect {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Long> viewCountMap = new ConcurrentHashMap<>();
    private final Map<FAQCategory, Long> categoryStatsMap = new ConcurrentHashMap<>();

    @Around("@annotation(faqAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, FAQAudit faqAudit) throws Throwable {
        if (!faqAudit.enabled()) {
            return joinPoint.proceed();
        }

        String auditId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        AuditContext context = buildAuditContext(joinPoint, faqAudit);

        try {
            // 사전 감사 로깅
            logAuditStart(auditId, context, faqAudit);

            // 메서드 실행
            Object result = joinPoint.proceed();

            // 사후 감사 처리
            long executionTime = System.currentTimeMillis() - startTime;
            processAuditSuccess(auditId, context, result, executionTime, faqAudit);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            processAuditFailure(auditId, context, e, executionTime, faqAudit);
            throw e;
        }
    }

    private AuditContext buildAuditContext(ProceedingJoinPoint joinPoint, FAQAudit faqAudit) {
        AuditContext context = new AuditContext();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        context.setMethodName(method.getName());
        context.setClassName(joinPoint.getTarget().getClass().getSimpleName());
        context.setTimestamp(LocalDateTime.now());

        // 사용자 정보 기록
        if (faqAudit.recordUser()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                context.setUsername(auth.getName());
                context.setUserRoles(auth.getAuthorities().toString());
            }
        }

        // IP 주소 기록
        if (faqAudit.recordIpAddress()) {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                context.setIpAddress(getClientIpAddress(request));
                context.setUserAgent(request.getHeader("User-Agent"));
            }
        }

        // 파라미터 기록
        if (faqAudit.recordParameters()) {
            context.setParameters(extractParameters(joinPoint, faqAudit.maskSensitiveData()));
        }

        // 이벤트 타입 결정
        context.setEventType(determineEventType(method.getName(), faqAudit.event()));

        return context;
    }

    private void logAuditStart(String auditId, AuditContext context, FAQAudit faqAudit) {
        if (faqAudit.level() == AuditLevel.DEBUG || faqAudit.detailed()) {
            log.debug("[AUDIT-START] ID: {}, Method: {}.{}, User: {}, Event: {}",
                    auditId, context.getClassName(), context.getMethodName(),
                    context.getUsername(), context.getEventType());
        }
    }

    private void processAuditSuccess(String auditId, AuditContext context,
                                     Object result, long executionTime, FAQAudit faqAudit) {

        context.setExecutionTime(executionTime);
        context.setSuccess(true);

        // 결과 기록
        if (faqAudit.recordResult() && result != null) {
            context.setResult(result.toString());
        }

        // 조회수 추적
        if (faqAudit.trackViewCount() && context.getEventType() == AuditEvent.FAQ_VIEWED) {
            trackViewCount(context);
        }

        // 카테고리별 통계
        if (faqAudit.collectCategoryStats()) {
            collectCategoryStats(context);
        }

        // 로깅
        logAuditEvent(auditId, context, faqAudit);

        // 데이터베이스 저장
        if (faqAudit.persistToDatabase()) {
            persistAuditLog(auditId, context, faqAudit);
        }

        // 외부 시스템 전송
        if (faqAudit.sendToExternalSystem() && !faqAudit.externalSystemUrl().isEmpty()) {
            sendToExternalSystem(auditId, context, faqAudit.externalSystemUrl());
        }

        // 실시간 모니터링
        if (faqAudit.realTimeMonitoring()) {
            publishRealTimeEvent(auditId, context);
        }
    }

    private void processAuditFailure(String auditId, AuditContext context,
                                     Exception error, long executionTime, FAQAudit faqAudit) {

        context.setExecutionTime(executionTime);
        context.setSuccess(false);
        context.setErrorMessage(error.getMessage());

        // 오류 레벨 로깅
        log.error("[AUDIT-ERROR] ID: {}, Method: {}.{}, User: {}, Error: {}",
                auditId, context.getClassName(), context.getMethodName(),
                context.getUsername(), error.getMessage());

        // 중요 이벤트 알림
        if (faqAudit.alertOnCriticalEvents()) {
            alertCriticalEvent(auditId, context, error);
        }

        // 데이터베이스 저장
        if (faqAudit.persistToDatabase()) {
            persistAuditLog(auditId, context, faqAudit);
        }
    }

    private void trackViewCount(AuditContext context) {
        String faqId = extractFaqId(context.getParameters());
        if (faqId != null) {
            viewCountMap.compute(faqId, (k, v) -> (v == null) ? 1L : v + 1);

            // 주기적으로 DB에 반영 (별도 스케줄러 필요)
            if (viewCountMap.get(faqId) % 10 == 0) {
                updateViewCountInDB(faqId, viewCountMap.get(faqId));
            }
        }
    }

    private void collectCategoryStats(AuditContext context) {
        FAQCategory category = extractCategory(context.getParameters());
        if (category != null) {
            categoryStatsMap.compute(category, (k, v) -> (v == null) ? 1L : v + 1);
        }
    }

    private void persistAuditLog(String auditId, AuditContext context, FAQAudit faqAudit) {
        CompletableFuture.runAsync(() -> {
            try {
                String sql = "INSERT INTO faq_audit_log (audit_id, event_type, username, " +
                        "ip_address, method_name, parameters, result, execution_time, " +
                        "success, error_message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(sql,
                        auditId,
                        context.getEventType().toString(),
                        context.getUsername(),
                        context.getIpAddress(),
                        context.getClassName() + "." + context.getMethodName(),
                        context.getParameters(),
                        context.getResult(),
                        context.getExecutionTime(),
                        context.isSuccess(),
                        context.getErrorMessage(),
                        context.getTimestamp()
                );

                // 보존 기간 관리
                cleanOldAuditLogs(faqAudit.retentionDays());

            } catch (Exception e) {
                log.error("Failed to persist audit log: {}", e.getMessage());
            }
        });
    }

    private void cleanOldAuditLogs(int retentionDays) {
        try {
            String sql = "DELETE FROM faq_audit_log WHERE created_at < ?";
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            jdbcTemplate.update(sql, cutoffDate);
        } catch (Exception e) {
            log.error("Failed to clean old audit logs: {}", e.getMessage());
        }
    }

    private void sendToExternalSystem(String auditId, AuditContext context, String url) {
        CompletableFuture.runAsync(() -> {
            try {
                // HTTP 클라이언트를 사용한 외부 시스템 전송 로직
                Map<String, Object> payload = new HashMap<>();
                payload.put("auditId", auditId);
                payload.put("event", context.getEventType());
                payload.put("timestamp", context.getTimestamp());
                payload.put("user", context.getUsername());
                payload.put("executionTime", context.getExecutionTime());

                // RestTemplate 또는 WebClient를 사용한 전송
                log.info("Sending audit log to external system: {}", url);

            } catch (Exception e) {
                log.error("Failed to send audit log to external system: {}", e.getMessage());
            }
        });
    }

    private void publishRealTimeEvent(String auditId, AuditContext context) {
        // WebSocket 또는 SSE를 통한 실시간 이벤트 발행
        log.debug("Publishing real-time audit event: {}", auditId);
    }

    private void alertCriticalEvent(String auditId, AuditContext context, Exception error) {
        // 이메일, 슬랙 등으로 중요 이벤트 알림
        log.warn("CRITICAL EVENT ALERT - Audit ID: {}, Error: {}", auditId, error.getMessage());
    }

    private void updateViewCountInDB(String faqId, Long count) {
        try {
            String sql = "UPDATE faq SET view_count = ? WHERE id = ?";
            jdbcTemplate.update(sql, count, Long.parseLong(faqId));
        } catch (Exception e) {
            log.error("Failed to update view count: {}", e.getMessage());
        }
    }

    private void logAuditEvent(String auditId, AuditContext context, FAQAudit faqAudit) {
        String logMessage = String.format(
                "[AUDIT] ID: %s, Event: %s, User: %s, Method: %s.%s, Time: %dms, Success: %s",
                auditId, context.getEventType(), context.getUsername(),
                context.getClassName(), context.getMethodName(),
                context.getExecutionTime(), context.isSuccess()
        );

        switch (faqAudit.level()) {
            case DEBUG:
                log.debug(logMessage);
                break;
            case INFO:
                log.info(logMessage);
                break;
            case WARN:
                log.warn(logMessage);
                break;
            case ERROR:
                log.error(logMessage);
                break;
            case CRITICAL:
                log.error("[CRITICAL] " + logMessage);
                break;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> extractParameters(ProceedingJoinPoint joinPoint, boolean maskSensitive) {
        Map<String, String> params = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if (args[i] != null) {
                String value = args[i].toString();
                if (maskSensitive && isSensitiveParam(paramNames[i])) {
                    value = maskValue(value);
                }
                params.put(paramNames[i], value);
            }
        }

        return params;
    }

    private boolean isSensitiveParam(String paramName) {
        return paramName.toLowerCase().contains("password") ||
                paramName.toLowerCase().contains("secret") ||
                paramName.toLowerCase().contains("token");
    }

    private String maskValue(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private AuditEvent determineEventType(String methodName, AuditEvent configuredEvent) {
        if (configuredEvent != AuditEvent.UNKNOWN) {
            return configuredEvent;
        }

        String method = methodName.toLowerCase();
        if (method.contains("create") || method.contains("add")) {
            return AuditEvent.FAQ_CREATED;
        } else if (method.contains("update") || method.contains("modify")) {
            return AuditEvent.FAQ_UPDATED;
        } else if (method.contains("delete") || method.contains("remove")) {
            return AuditEvent.FAQ_DELETED;
        } else if (method.contains("search")) {
            return AuditEvent.FAQ_SEARCHED;
        } else if (method.contains("get") || method.contains("find") || method.contains("view")) {
            return AuditEvent.FAQ_VIEWED;
        } else if (method.contains("upload")) {
            return AuditEvent.FILE_UPLOADED;
        }

        return AuditEvent.UNKNOWN;
    }

    private String extractFaqId(Map<String, String> parameters) {
        return parameters.get("id");
    }

    private FAQCategory extractCategory(Map<String, String> parameters) {
        String category = parameters.get("category");
        if (category != null) {
            try {
                return FAQCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    // 내부 감사 컨텍스트 클래스
    private static class AuditContext {
        private String methodName;
        private String className;
        private LocalDateTime timestamp;
        private String username;
        private String userRoles;
        private String ipAddress;
        private String userAgent;
        private Map<String, String> parameters;
        private AuditEvent eventType;
        private long executionTime;
        private boolean success;
        private String result;
        private String errorMessage;

        // Getters and Setters
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getUserRoles() { return userRoles; }
        public void setUserRoles(String userRoles) { this.userRoles = userRoles; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

        public AuditEvent getEventType() { return eventType; }
        public void setEventType(AuditEvent eventType) { this.eventType = eventType; }

        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}