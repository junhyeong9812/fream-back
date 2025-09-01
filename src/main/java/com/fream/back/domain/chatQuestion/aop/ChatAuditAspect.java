package com.fream.back.domain.chatQuestion.aop;

import com.fream.back.domain.chatQuestion.aop.annotation.ChatAudit;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChatQuestion 도메인 감사 추적 AOP
 * @ChatAudit 어노테이션을 기반으로 감사 로그 기록
 */
@Aspect
@Component
@Slf4j
public class ChatAuditAspect {

    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // 감사 이벤트 카운터
    private final ConcurrentHashMap<String, AtomicLong> auditEventCounters = new ConcurrentHashMap<>();

    // 비용 추적을 위한 저장소
    private final ConcurrentHashMap<String, CostTracker> userCostTrackers = new ConcurrentHashMap<>();

    // 품질 메트릭 저장소
    private final ConcurrentHashMap<String, QualityMetrics> qualityMetricsMap = new ConcurrentHashMap<>();

    /**
     * @ChatAudit 어노테이션이 붙은 메서드의 감사 로그 처리
     */
    @Around("@annotation(chatAudit)")
    public Object handleAuditLogging(ProceedingJoinPoint proceedingJoinPoint,
                                     ChatAudit chatAudit) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        // 감사 컨텍스트 준비
        AuditContext auditContext = prepareAuditContext(chatAudit, methodName, className, args);

        // 메서드 실행 전 감사 로그
        logAuditEvent(chatAudit, auditContext, "BEFORE", null, null, 0);

        long startTime = System.currentTimeMillis();

        try {
            Object result = proceedingJoinPoint.proceed();

            long responseTime = System.currentTimeMillis() - startTime;

            // 메서드 실행 후 감사 로그
            logAuditEvent(chatAudit, auditContext, "SUCCESS", result, null, responseTime);

            // 비용 계산 및 기록
            if (chatAudit.calculateCost()) {
                calculateAndRecordCost(auditContext, result);
            }

            // 품질 메트릭 기록
            if (chatAudit.recordQualityMetrics()) {
                recordQualityMetrics(auditContext, result, responseTime);
            }

            // 비용 임계값 확인
            checkCostThreshold(chatAudit, auditContext);

            // 외부 시스템 연동
            if (chatAudit.sendToExternalSystem()) {
                sendToExternalSystem(auditContext, result);
            }

            // 실시간 알림
            if (chatAudit.realTimeAlert()) {
                sendRealTimeAlert(auditContext, result);
            }

            return result;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // 예외 발생 시 감사 로그
            logAuditEvent(chatAudit, auditContext, "ERROR", null, e, responseTime);

            throw e;
        }
    }

    /**
     * GPT API 호출 감사 로깅
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.chatQuestion.service.GPTService.getGPTResponseWithUsage(..))",
            returning = "result")
    public void auditGPTApiCall(JoinPoint joinPoint, Object result) {
        try {
            String userEmail = extractUserEmailSafely();
            Object[] args = joinPoint.getArgs();

            AuditContext context = new AuditContext();
            context.setUserEmail(userEmail);
            context.setEvent(ChatAudit.AuditEvent.GPT_API_CALLED);
            context.setTimestamp(LocalDateTime.now());
            context.setMethodName("getGPTResponseWithUsage");
            context.setClassName("GPTService");

            if (result instanceof GPTResponseDto) {
                GPTResponseDto gptResponse = (GPTResponseDto) result;
                context.setTokensUsed(gptResponse.getUsage() != null ? gptResponse.getUsage().getTotal_tokens() : 0);
                context.setModelUsed(gptResponse.getModel());
            }

            log.info("AUDIT_GPT_API_CALL - User: {}, Model: {}, Tokens: {}, Timestamp: {}",
                    context.getUserEmail(), context.getModelUsed(), context.getTokensUsed(),
                    context.getTimestamp().format(AUDIT_TIMESTAMP_FORMAT));

            // 이벤트 카운터 증가
            incrementEventCounter("GPT_API_CALL");

        } catch (Exception e) {
            log.error("GPT API 호출 감사 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 관리자 접근 감사 로깅
     */
    @Before("execution(* com.fream.back.domain.chatQuestion.controller.GPTUsageController.*(..))")
    public void auditAdminAccess(JoinPoint joinPoint) {
        try {
            String userEmail = extractUserEmailSafely();
            String methodName = joinPoint.getSignature().getName();

            log.warn("AUDIT_ADMIN_ACCESS - User: {}, Method: {}, IP: {}, UserAgent: {}, Timestamp: {}",
                    userEmail, methodName, getClientIp(), getUserAgent(),
                    LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));

            incrementEventCounter("ADMIN_ACCESS");

        } catch (Exception e) {
            log.error("관리자 접근 감사 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 높은 토큰 사용량 감사 로깅
     */
    @AfterReturning(pointcut = "execution(* com.fream.back.domain.chatQuestion.service.ChatService.processQuestion(..))",
            returning = "result")
    public void auditHighTokenUsage(JoinPoint joinPoint, Object result) {
        try {
            if (result instanceof QuestionResponseDto) {
                // GPT 응답에서 토큰 사용량을 추출하는 로직이 필요
                // 여기서는 예시로 임계값을 확인
                String userEmail = extractUserEmailSafely();

                log.info("AUDIT_QUESTION_PROCESSED - User: {}, Timestamp: {}",
                        userEmail, LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));

                incrementEventCounter("QUESTION_PROCESSED");
            }
        } catch (Exception e) {
            log.error("높은 토큰 사용량 감사 로깅 실패: {}", e.getMessage());
        }
    }

    /**
     * 감사 컨텍스트 준비
     */
    private AuditContext prepareAuditContext(ChatAudit chatAudit, String methodName,
                                             String className, Object[] args) {
        AuditContext context = new AuditContext();
        context.setAuditId(generateAuditId());
        context.setEvent(chatAudit.event());
        context.setLevel(chatAudit.level());
        context.setMethodName(methodName);
        context.setClassName(className);
        context.setUserEmail(extractUserEmailSafely());
        context.setTimestamp(LocalDateTime.now());

        // IP 주소 기록
        if (chatAudit.recordIpAddress()) {
            context.setClientIp(getClientIp());
        }

        // 사용자 에이전트 기록
        if (chatAudit.recordUserAgent()) {
            context.setUserAgent(getUserAgent());
        }

        // 요청 헤더 기록
        if (chatAudit.recordHeaders()) {
            context.setRequestHeaders(getRequestHeaders());
        }

        // 질문 내용 추출 (민감한 데이터 처리)
        if (chatAudit.containsSensitiveData()) {
            context.setQuestionContent(extractAndMaskQuestionContent(args));
        }

        return context;
    }

    /**
     * 감사 이벤트 로깅
     */
    private void logAuditEvent(ChatAudit chatAudit, AuditContext context, String phase,
                               Object result, Exception exception, long responseTime) {
        StringBuilder logMessage = new StringBuilder();

        logMessage.append("CHAT_AUDIT_EVENT - ")
                .append("AuditId: ").append(context.getAuditId())
                .append(", Event: ").append(context.getEvent().name())
                .append(", Phase: ").append(phase)
                .append(", User: ").append(context.getUserEmail())
                .append(", Method: ").append(context.getClassName()).append(".").append(context.getMethodName())
                .append(", Timestamp: ").append(context.getTimestamp().format(AUDIT_TIMESTAMP_FORMAT));

        if (chatAudit.recordIpAddress() && context.getClientIp() != null) {
            logMessage.append(", IP: ").append(context.getClientIp());
        }

        if (chatAudit.recordResponseTime() && responseTime > 0) {
            logMessage.append(", ResponseTime: ").append(responseTime).append("ms");
        }

        if (chatAudit.recordTokenUsage() && context.getTokensUsed() > 0) {
            logMessage.append(", Tokens: ").append(context.getTokensUsed());
        }

        if (chatAudit.recordModelInfo() && context.getModelUsed() != null) {
            logMessage.append(", Model: ").append(context.getModelUsed());
        }

        if (chatAudit.containsSensitiveData() && context.getQuestionContent() != null) {
            logMessage.append(", QuestionSummary: ").append(context.getQuestionContent());
        }

        if (exception != null) {
            logMessage.append(", Exception: ").append(exception.getClass().getSimpleName())
                    .append(", Message: ").append(exception.getMessage());
        }

        if (!chatAudit.description().isEmpty()) {
            logMessage.append(", Description: ").append(chatAudit.description());
        }

        // 레벨에 따른 로깅
        switch (chatAudit.level()) {
            case DEBUG:
                log.debug(logMessage.toString());
                break;
            case INFO:
                log.info(logMessage.toString());
                break;
            case WARN:
                log.warn(logMessage.toString());
                break;
            case ERROR:
                log.error(logMessage.toString());
                break;
            case SECURITY:
                log.warn("SECURITY_AUDIT - {}", logMessage.toString());
                break;
            case COMPLIANCE:
                log.info("COMPLIANCE_AUDIT - {}", logMessage.toString());
                break;
            case FINANCIAL:
                log.info("FINANCIAL_AUDIT - {}", logMessage.toString());
                break;
        }

        // 이벤트 카운터 증가
        incrementEventCounter(context.getEvent().name());
    }

    /**
     * 비용 계산 및 기록
     */
    private void calculateAndRecordCost(AuditContext context, Object result) {
        if (result instanceof GPTResponseDto) {
            GPTResponseDto gptResponse = (GPTResponseDto) result;
            if (gptResponse.getUsage() != null) {
                int totalTokens = gptResponse.getUsage().getTotal_tokens();
                int estimatedCostCents = calculateTokenCost(gptResponse.getModel(), totalTokens);

                context.setTokensUsed(totalTokens);
                context.setEstimatedCost(estimatedCostCents);

                // 사용자별 비용 추적
                CostTracker tracker = userCostTrackers.computeIfAbsent(context.getUserEmail(), k -> new CostTracker());
                tracker.addCost(estimatedCostCents);

                log.info("COST_TRACKING - User: {}, Tokens: {}, EstimatedCostCents: {}, TotalCostToday: {}",
                        context.getUserEmail(), totalTokens, estimatedCostCents, tracker.getTodayCost());
            }
        }
    }

    /**
     * 품질 메트릭 기록
     */
    private void recordQualityMetrics(AuditContext context, Object result, long responseTime) {
        if (result instanceof QuestionResponseDto) {
            QuestionResponseDto response = (QuestionResponseDto) result;

            QualityMetrics metrics = qualityMetricsMap.computeIfAbsent(context.getUserEmail(), k -> new QualityMetrics());

            // 응답 품질 평가
            int qualityScore = evaluateResponseQuality(response.getAnswer(), responseTime);
            metrics.addScore(qualityScore);

            log.info("QUALITY_METRICS - User: {}, ResponseTime: {}ms, QualityScore: {}, AvgQuality: {}",
                    context.getUserEmail(), responseTime, qualityScore, metrics.getAverageScore());
        }
    }

    /**
     * 비용 임계값 확인
     */
    private void checkCostThreshold(ChatAudit chatAudit, AuditContext context) {
        if (context.getEstimatedCost() > 0) {
            CostTracker tracker = userCostTrackers.get(context.getUserEmail());
            if (tracker != null && tracker.getTodayCost() > chatAudit.costThreshold()) {
                log.warn("COST_THRESHOLD_EXCEEDED - User: {}, TodayCost: {}cents, Threshold: {}cents",
                        context.getUserEmail(), tracker.getTodayCost(), chatAudit.costThreshold());

                incrementEventCounter("HIGH_COST_INCURRED");
            }
        }
    }

    /**
     * 외부 시스템 연동
     */
    private void sendToExternalSystem(AuditContext context, Object result) {
        // 외부 모니터링 시스템, 로그 집계 시스템 등으로 데이터 전송
        log.info("EXTERNAL_SYSTEM_SEND - AuditId: {}, Event: {}, User: {}, Timestamp: {}",
                context.getAuditId(), context.getEvent().name(), context.getUserEmail(),
                context.getTimestamp().format(AUDIT_TIMESTAMP_FORMAT));

        // 실제 구현에서는 Kafka, RabbitMQ, HTTP API 등을 사용하여 외부 시스템에 전송
    }

    /**
     * 실시간 알림
     */
    private void sendRealTimeAlert(AuditContext context, Object result) {
        // 실시간 알림 시스템 (Slack, 이메일, SMS 등)으로 알림 전송
        log.info("REAL_TIME_ALERT - AuditId: {}, Event: {}, User: {}, Description: {}",
                context.getAuditId(), context.getEvent().name(), context.getUserEmail(),
                context.getEvent().getDescription());

        // 실제 구현에서는 알림 서비스 API 호출
    }

    /**
     * 토큰 비용 계산
     */
    private int calculateTokenCost(String model, int tokens) {
        // GPT 모델별 토큰 당 비용 계산 (센트 단위)
        double costPerToken = 0.0;

        if (model != null) {
            if (model.contains("gpt-4")) {
                costPerToken = 0.03 / 1000.0; // GPT-4: $0.03/1K tokens
            } else if (model.contains("gpt-3.5")) {
                costPerToken = 0.0015 / 1000.0; // GPT-3.5: $0.0015/1K tokens
            } else {
                costPerToken = 0.002 / 1000.0; // 기본값
            }
        }

        return (int) Math.ceil(tokens * costPerToken * 100); // 센트 단위로 반환
    }

    /**
     * 응답 품질 평가
     */
    private int evaluateResponseQuality(String answer, long responseTime) {
        int score = 50; // 기본 점수

        if (answer != null) {
            // 답변 길이 점수
            if (answer.length() > 100) score += 10;
            if (answer.length() > 300) score += 10;

            // 응답 시간 점수
            if (responseTime < 3000) score += 15; // 3초 이내
            else if (responseTime < 5000) score += 10; // 5초 이내
            else if (responseTime > 10000) score -= 10; // 10초 초과 시 감점

            // 내용 품질 점수 (간단한 휴리스틱)
            if (!answer.toLowerCase().contains("죄송") && !answer.toLowerCase().contains("미안")) {
                score += 10; // 사과 표현이 없으면 더 도움이 되는 답변으로 간주
            }

            if (answer.contains("자세한") || answer.contains("구체적") || answer.contains("예시")) {
                score += 5; // 상세한 설명이 있으면 가점
            }
        }

        return Math.min(100, Math.max(0, score)); // 0-100 범위로 제한
    }

    /**
     * 질문 내용 추출 및 마스킹
     */
    private String extractAndMaskQuestionContent(Object[] args) {
        if (args == null) return null;

        for (Object arg : args) {
            if (arg instanceof QuestionRequestDto) {
                String question = ((QuestionRequestDto) arg).getQuestion();
                return maskSensitiveContent(question);
            }
            if (arg instanceof String && ((String) arg).length() > 10) {
                return maskSensitiveContent((String) arg);
            }
        }
        return null;
    }

    /**
     * 민감한 내용 마스킹
     */
    private String maskSensitiveContent(String content) {
        if (content == null) return null;

        // 이메일 주소 마스킹
        content = content.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "***@***.***");

        // 전화번호 마스킹
        content = content.replaceAll("\\b\\d{2,3}-\\d{3,4}-\\d{4}\\b", "***-****-****");

        // 주민등록번호 마스킹
        content = content.replaceAll("\\b\\d{6}-\\d{7}\\b", "******-*******");

        // 내용이 너무 길면 자르기
        if (content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }

        return content;
    }

    /**
     * 이벤트 카운터 증가
     */
    private void incrementEventCounter(String eventType) {
        auditEventCounters.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 감사 ID 생성
     */
    private String generateAuditId() {
        return "AUDIT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
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
     * 클라이언트 IP 주소 가져오기
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();

            String[] headers = {
                    "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
            };

            for (String header : headers) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip.split(",")[0].trim();
                }
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 사용자 에이전트 가져오기
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 요청 헤더 가져오기
     */
    private Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();

            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // 민감한 헤더는 제외
                if (!headerName.toLowerCase().contains("authorization") &&
                        !headerName.toLowerCase().contains("cookie")) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }
        } catch (Exception e) {
            headers.put("error", "Unable to retrieve headers");
        }
        return headers;
    }

    // 내부 클래스들
    private static class AuditContext {
        private String auditId;
        private ChatAudit.AuditEvent event;
        private ChatAudit.AuditLevel level;
        private String methodName;
        private String className;
        private String userEmail;
        private LocalDateTime timestamp;
        private String clientIp;
        private String userAgent;
        private Map<String, String> requestHeaders;
        private String questionContent;
        private int tokensUsed;
        private String modelUsed;
        private int estimatedCost;

        // Getters and Setters
        public String getAuditId() { return auditId; }
        public void setAuditId(String auditId) { this.auditId = auditId; }

        public ChatAudit.AuditEvent getEvent() { return event; }
        public void setEvent(ChatAudit.AuditEvent event) { this.event = event; }

        public ChatAudit.AuditLevel getLevel() { return level; }
        public void setLevel(ChatAudit.AuditLevel level) { this.level = level; }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public Map<String, String> getRequestHeaders() { return requestHeaders; }
        public void setRequestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; }

        public String getQuestionContent() { return questionContent; }
        public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }

        public int getTokensUsed() { return tokensUsed; }
        public void setTokensUsed(int tokensUsed) { this.tokensUsed = tokensUsed; }

        public String getModelUsed() { return modelUsed; }
        public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

        public int getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(int estimatedCost) { this.estimatedCost = estimatedCost; }
    }

    private static class CostTracker {
        private final AtomicLong totalCost = new AtomicLong(0);
        private final AtomicLong todayCost = new AtomicLong(0);
        private volatile LocalDateTime lastReset = LocalDateTime.now();

        public void addCost(int costCents) {
            totalCost.addAndGet(costCents);

            // 일일 비용 초기화 체크
            LocalDateTime now = LocalDateTime.now();
            if (!lastReset.toLocalDate().equals(now.toLocalDate())) {
                todayCost.set(0);
                lastReset = now;
            }

            todayCost.addAndGet(costCents);
        }

        public long getTotalCost() {
            return totalCost.get();
        }

        public long getTodayCost() {
            // 날짜가 바뀌었는지 확인
            LocalDateTime now = LocalDateTime.now();
            if (!lastReset.toLocalDate().equals(now.toLocalDate())) {
                todayCost.set(0);
                lastReset = now;
            }
            return todayCost.get();
        }
    }

    private static class QualityMetrics {
        private final AtomicLong totalScore = new AtomicLong(0);
        private final AtomicLong requestCount = new AtomicLong(0);

        public void addScore(int score) {
            totalScore.addAndGet(score);
            requestCount.incrementAndGet();
        }

        public double getAverageScore() {
            long count = requestCount.get();
            return count > 0 ? (double) totalScore.get() / count : 0.0;
        }

        public long getRequestCount() {
            return requestCount.get();
        }
    }
}