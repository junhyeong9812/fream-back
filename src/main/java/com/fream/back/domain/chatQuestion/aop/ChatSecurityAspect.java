package com.fream.back.domain.chatQuestion.aop;

import com.fream.back.domain.chatQuestion.aop.annotation.ChatSecurity;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.exception.ChatPermissionException;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * ChatQuestion 도메인 보안 AOP
 * @ChatSecurity 어노테이션을 기반으로 보안 제어 수행
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSecurityAspect {

    private final UserQueryService userQueryService;

    // 속도 제한을 위한 사용자별 요청 카운터
    private final ConcurrentHashMap<String, UserRateLimit> rateLimitMap = new ConcurrentHashMap<>();

    // 일일 토큰 사용량 추적
    private final ConcurrentHashMap<String, UserTokenUsage> dailyTokenUsage = new ConcurrentHashMap<>();

    // 월간 토큰 사용량 추적
    private final ConcurrentHashMap<String, UserTokenUsage> monthlyTokenUsage = new ConcurrentHashMap<>();

    // 반복 질문 감지를 위한 저장소
    private final ConcurrentHashMap<String, RepeatedQuestionTracker> repeatedQuestionMap = new ConcurrentHashMap<>();

    // 스팸 감지를 위한 저장소
    private final ConcurrentHashMap<String, SpamDetectionInfo> spamDetectionMap = new ConcurrentHashMap<>();

    // 악성 키워드 패턴들
    private static final Pattern[] MALICIOUS_PATTERNS = {
            Pattern.compile("(?i).*(hack|crack|exploit|vulnerability).*"),
            Pattern.compile("(?i).*(password|credential|token|key).*leak.*"),
            Pattern.compile("(?i).*personal.*information.*(leak|steal|obtain).*"),
            Pattern.compile("(?i).*(bomb|weapon|terrorist|illegal).*"),
            Pattern.compile("(?i).*(adult|sexual|inappropriate).*content.*")
    };

    // 민감한 정보 요청 패턴들
    private static final Pattern[] SENSITIVE_PATTERNS = {
            Pattern.compile("(?i).*what.*is.*my.*(password|credit.*card|ssn|social.*security).*"),
            Pattern.compile("(?i).*give.*me.*(admin|root|master).*access.*"),
            Pattern.compile("(?i).*show.*me.*(database|config|secret|key).*"),
            Pattern.compile("(?i).*bypass.*(security|authentication|authorization).*")
    };

    /**
     * @ChatSecurity 어노테이션이 붙은 메서드의 보안 검증
     */
    @Around("@annotation(chatSecurity)")
    public Object handleSecurityChecks(ProceedingJoinPoint proceedingJoinPoint,
                                       ChatSecurity chatSecurity) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        Object[] args = proceedingJoinPoint.getArgs();

        String userEmail = null;
        String clientIp = null;
        HttpServletRequest request = getCurrentRequest();

        if (request != null) {
            clientIp = getClientIpAddress(request);
        }

        // 인증 확인
        if (chatSecurity.requireAuthentication()) {
            userEmail = performAuthenticationCheck(chatSecurity);
        }

        // 보안 검증 수행
        for (ChatSecurity.SecurityCheck check : chatSecurity.checks()) {
            performSecurityCheck(check, chatSecurity, userEmail, clientIp, methodName, className, args);
        }

        // 보안 감사 로그
        if (chatSecurity.enableAuditLog()) {
            logSecurityAudit(methodName, className, userEmail, clientIp, "SECURITY_CHECK_PASSED", args);
        }

        try {
            return proceedingJoinPoint.proceed();
        } catch (Exception e) {
            // 보안 위반 처리
            handleSecurityViolation(chatSecurity, e, methodName, className, userEmail, clientIp);
            throw e;
        }
    }

    /**
     * 인증 확인 수행
     */
    private String performAuthenticationCheck(ChatSecurity chatSecurity) {
        try {
            String userEmail = SecurityUtils.extractEmailFromSecurityContext();

            // 관리자 권한 필요 시 확인
            if (chatSecurity.requireAdminRole()) {
                userQueryService.checkAdminRole(userEmail);
            }

            // 역할 기반 권한 확인
            if (chatSecurity.requiredRoles().length > 0) {
                checkUserRoles(userEmail, chatSecurity.requiredRoles());
            }

            return userEmail;

        } catch (Exception e) {
            log.error("인증 확인 실패: {}", e.getMessage());
            handleViolationAction(chatSecurity.violationAction(), "AUTHENTICATION_FAILED", e.getMessage());
            return null;
        }
    }

    /**
     * 보안 검증 수행
     */
    private void performSecurityCheck(ChatSecurity.SecurityCheck check, ChatSecurity chatSecurity,
                                      String userEmail, String clientIp, String methodName,
                                      String className, Object[] args) {
        switch (check) {
            case AUTHENTICATION:
                if (chatSecurity.requireAuthentication() && userEmail == null) {
                    handleViolationAction(chatSecurity.violationAction(), "AUTHENTICATION_REQUIRED", "Authentication required");
                }
                break;

            case AUTHORIZATION:
                performAuthorizationCheck(chatSecurity, userEmail);
                break;

            case RATE_LIMIT:
                if (chatSecurity.enableRateLimit()) {
                    checkRateLimit(userEmail != null ? userEmail : clientIp, chatSecurity);
                }
                break;

            case IP_RESTRICTION:
                if (chatSecurity.checkIpRestriction()) {
                    checkIpRestriction(clientIp, chatSecurity);
                }
                break;

            case TIME_RESTRICTION:
                if (chatSecurity.checkTimeRestriction()) {
                    checkTimeRestriction(chatSecurity);
                }
                break;

            case TOKEN_LIMIT:
                checkTokenLimit(userEmail, chatSecurity);
                break;

            case CONTENT_FILTER:
                if (chatSecurity.filterMaliciousContent()) {
                    checkContentFilter(args);
                }
                break;

            case SPAM_DETECTION:
                if (chatSecurity.detectSpam()) {
                    checkSpamDetection(userEmail != null ? userEmail : clientIp, args);
                }
                break;

            case REPEATED_QUESTION:
                if (chatSecurity.limitRepeatedQuestions()) {
                    checkRepeatedQuestion(userEmail != null ? userEmail : clientIp, args);
                }
                break;

            case SENSITIVE_DATA_BLOCK:
                if (chatSecurity.blockSensitiveRequests()) {
                    checkSensitiveDataRequest(args);
                }
                break;
        }
    }

    /**
     * 인가 확인
     */
    private void performAuthorizationCheck(ChatSecurity chatSecurity, String userEmail) {
        if (chatSecurity.requiredRoles().length > 0) {
            checkUserRoles(userEmail, chatSecurity.requiredRoles());
        }
    }

    /**
     * 사용자 역할 확인
     */
    private void checkUserRoles(String userEmail, String[] requiredRoles) {
        try {
            User user = userQueryService.findByEmail(userEmail);
            boolean hasRequiredRole = Arrays.stream(requiredRoles)
                    .anyMatch(role -> user.getRole().name().equals(role));

            if (!hasRequiredRole) {
                throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                        "필요한 권한이 없습니다: " + Arrays.toString(requiredRoles));
            }
        } catch (Exception e) {
            log.error("사용자 역할 확인 실패: {}", e.getMessage());
            throw new ChatPermissionException(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED,
                    "권한 확인 중 오류가 발생했습니다.");
        }
    }

    /**
     * 속도 제한 확인
     */
    private void checkRateLimit(String identifier, ChatSecurity chatSecurity) {
        UserRateLimit rateLimit = rateLimitMap.computeIfAbsent(identifier, k -> new UserRateLimit());

        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (chatSecurity.rateLimitWindow() * 1000L);

        // 윈도우 밖의 요청들 제거
        rateLimit.cleanExpiredRequests(windowStart);

        // 현재 윈도우 내 요청 수 확인
        if (rateLimit.getRequestCount() >= chatSecurity.maxRequestsPerWindow()) {
            log.warn("속도 제한 초과 - Identifier: {}, RequestCount: {}, Limit: {}",
                    identifier, rateLimit.getRequestCount(), chatSecurity.maxRequestsPerWindow());

            handleViolationAction(chatSecurity.violationAction(), "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Limit: " + chatSecurity.maxRequestsPerWindow() + " per " + chatSecurity.rateLimitWindow() + " seconds");
            return;
        }

        rateLimit.addRequest(currentTime);
    }

    /**
     * IP 주소 제한 확인
     */
    private void checkIpRestriction(String clientIp, ChatSecurity chatSecurity) {
        if (clientIp == null || chatSecurity.allowedIpPatterns().length == 0) {
            return;
        }

        boolean allowed = Arrays.stream(chatSecurity.allowedIpPatterns())
                .anyMatch(pattern -> clientIp.matches(pattern));

        if (!allowed) {
            log.warn("IP 주소 제한 위반 - IP: {}, AllowedPatterns: {}",
                    clientIp, Arrays.toString(chatSecurity.allowedIpPatterns()));

            handleViolationAction(chatSecurity.violationAction(), "IP_RESTRICTION_VIOLATION",
                    "IP address not allowed: " + clientIp);
        }
    }

    /**
     * 시간 제한 확인
     */
    private void checkTimeRestriction(ChatSecurity chatSecurity) {
        if (chatSecurity.allowedTimeRange().isEmpty()) {
            return;
        }

        try {
            String[] timeRange = chatSecurity.allowedTimeRange().split("-");
            if (timeRange.length != 2) {
                log.warn("잘못된 시간 범위 설정: {}", chatSecurity.allowedTimeRange());
                return;
            }

            LocalTime startTime = LocalTime.parse(timeRange[0]);
            LocalTime endTime = LocalTime.parse(timeRange[1]);
            LocalTime currentTime = LocalTime.now();

            boolean isAllowed = currentTime.isAfter(startTime) && currentTime.isBefore(endTime);

            if (!isAllowed) {
                log.warn("시간 제한 위반 - CurrentTime: {}, AllowedRange: {}",
                        currentTime, chatSecurity.allowedTimeRange());

                handleViolationAction(chatSecurity.violationAction(), "TIME_RESTRICTION_VIOLATION",
                        "Access not allowed at this time. Allowed: " + chatSecurity.allowedTimeRange());
            }
        } catch (Exception e) {
            log.error("시간 제한 확인 중 오류: {}", e.getMessage());
        }
    }

    /**
     * 토큰 사용량 제한 확인
     */
    private void checkTokenLimit(String userEmail, ChatSecurity chatSecurity) {
        if (userEmail == null) return;

        // 일일 토큰 사용량 확인
        if (chatSecurity.dailyTokenLimit() > 0) {
            UserTokenUsage dailyUsage = dailyTokenUsage.computeIfAbsent(userEmail, k -> new UserTokenUsage());
            if (dailyUsage.isNewDay()) {
                dailyUsage.reset();
            }

            if (dailyUsage.getUsage() >= chatSecurity.dailyTokenLimit()) {
                log.warn("일일 토큰 사용량 초과 - User: {}, Usage: {}, Limit: {}",
                        userEmail, dailyUsage.getUsage(), chatSecurity.dailyTokenLimit());

                handleViolationAction(chatSecurity.violationAction(), "DAILY_TOKEN_LIMIT_EXCEEDED",
                        "Daily token limit exceeded: " + chatSecurity.dailyTokenLimit());
                return;
            }
        }

        // 월간 토큰 사용량 확인
        if (chatSecurity.monthlyTokenLimit() > 0) {
            UserTokenUsage monthlyUsage = monthlyTokenUsage.computeIfAbsent(userEmail, k -> new UserTokenUsage());
            if (monthlyUsage.isNewMonth()) {
                monthlyUsage.reset();
            }

            if (monthlyUsage.getUsage() >= chatSecurity.monthlyTokenLimit()) {
                log.warn("월간 토큰 사용량 초과 - User: {}, Usage: {}, Limit: {}",
                        userEmail, monthlyUsage.getUsage(), chatSecurity.monthlyTokenLimit());

                handleViolationAction(chatSecurity.violationAction(), "MONTHLY_TOKEN_LIMIT_EXCEEDED",
                        "Monthly token limit exceeded: " + chatSecurity.monthlyTokenLimit());
            }
        }
    }

    /**
     * 콘텐츠 필터링
     */
    private void checkContentFilter(Object[] args) {
        String questionContent = extractQuestionContent(args);
        if (questionContent == null || questionContent.trim().isEmpty()) {
            return;
        }

        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(questionContent).matches()) {
                log.warn("악성 콘텐츠 감지 - Content: {}", questionContent.substring(0, Math.min(50, questionContent.length())));

                throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                        "부적절한 내용이 포함된 질문입니다.");
            }
        }
    }

    /**
     * 스팸 감지
     */
    private void checkSpamDetection(String identifier, Object[] args) {
        String questionContent = extractQuestionContent(args);
        if (questionContent == null) return;

        SpamDetectionInfo spamInfo = spamDetectionMap.computeIfAbsent(identifier, k -> new SpamDetectionInfo());

        // 동일한 질문 반복 확인
        if (spamInfo.isDuplicateQuestion(questionContent)) {
            log.warn("스팸 질문 감지 - Identifier: {}, Question: {}",
                    identifier, questionContent.substring(0, Math.min(30, questionContent.length())));

            throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                    "스팸으로 판단된 질문입니다.");
        }

        spamInfo.addQuestion(questionContent);
    }

    /**
     * 반복 질문 확인
     */
    private void checkRepeatedQuestion(String identifier, Object[] args) {
        String questionContent = extractQuestionContent(args);
        if (questionContent == null) return;

        RepeatedQuestionTracker tracker = repeatedQuestionMap.computeIfAbsent(identifier, k -> new RepeatedQuestionTracker());

        if (tracker.isRepeatedQuestion(questionContent)) {
            log.warn("반복 질문 감지 - Identifier: {}, Question: {}",
                    identifier, questionContent.substring(0, Math.min(30, questionContent.length())));

            throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                    "동일한 질문이 너무 자주 반복되고 있습니다.");
        }

        tracker.addQuestion(questionContent);
    }

    /**
     * 민감한 데이터 요청 확인
     */
    private void checkSensitiveDataRequest(Object[] args) {
        String questionContent = extractQuestionContent(args);
        if (questionContent == null || questionContent.trim().isEmpty()) {
            return;
        }

        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(questionContent).matches()) {
                log.warn("민감한 정보 요청 감지 - Content: {}",
                        questionContent.substring(0, Math.min(50, questionContent.length())));

                throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED,
                        "민감한 정보에 대한 질문은 허용되지 않습니다.");
            }
        }
    }

    /**
     * 보안 위반 처리
     */
    private void handleSecurityViolation(ChatSecurity chatSecurity, Exception exception,
                                         String methodName, String className,
                                         String userEmail, String clientIp) {
        logSecurityAudit(methodName, className, userEmail, clientIp, "SECURITY_VIOLATION",
                new Object[]{exception.getMessage()});
    }

    /**
     * 위반 액션 처리
     */
    private void handleViolationAction(ChatSecurity.ViolationAction action, String violationType, String message) {
        switch (action) {
            case THROW_EXCEPTION:
                throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED, message);
            case LOG_AND_DENY:
                log.warn("보안 위반 - Type: {}, Message: {}", violationType, message);
                throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED, message);
            case LOG_AND_THROTTLE:
                log.warn("보안 위반 (스로틀링) - Type: {}, Message: {}", violationType, message);
                try {
                    Thread.sleep(2000); // 2초 지연
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            case SILENT_DENY:
                throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED, "접근이 거부되었습니다.");
            case FALLBACK_RESPONSE:
                log.info("보안 위반 폴백 - Type: {}, Message: {}", violationType, message);
                break;
            case BLACKLIST_USER:
                log.error("사용자 블랙리스트 추가 - Type: {}, Message: {}", violationType, message);
                throw new ChatPermissionException(ChatQuestionErrorCode.QUESTION_PERMISSION_DENIED, "계정이 제한되었습니다.");
        }
    }

    /**
     * 보안 감사 로그 기록
     */
    private void logSecurityAudit(String methodName, String className, String userEmail,
                                  String clientIp, String event, Object[] args) {
        log.info("SECURITY_AUDIT - Event: {}, Method: {}.{}, User: {}, IP: {}, Args: {}, Timestamp: {}",
                event, className, methodName, userEmail, clientIp,
                formatArgsForAudit(args), LocalDateTime.now());
    }

    /**
     * 질문 내용 추출
     */
    private String extractQuestionContent(Object[] args) {
        if (args == null) return null;

        for (Object arg : args) {
            if (arg instanceof QuestionRequestDto) {
                return ((QuestionRequestDto) arg).getQuestion();
            }
            if (arg instanceof String && ((String) arg).length() > 10) {
                return (String) arg;
            }
        }
        return null;
    }

    /**
     * 현재 HTTP 요청 가져오기
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 감사 로그용 인자 포맷팅
     */
    private String formatArgsForAudit(Object[] args) {
        if (args == null || args.length == 0) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            Object arg = args[i];
            if (arg instanceof QuestionRequestDto) {
                QuestionRequestDto request = (QuestionRequestDto) arg;
                String question = request.getQuestion();
                if (question != null && question.length() > 30) {
                    sb.append("QuestionRequest{question='").append(question.substring(0, 30)).append("...'}");
                } else {
                    sb.append("QuestionRequest{question='").append(question).append("'}");
                }
            } else if (arg instanceof String) {
                String str = (String) arg;
                if (str.length() > 30) {
                    sb.append("'").append(str.substring(0, 30)).append("...'");
                } else {
                    sb.append("'").append(str).append("'");
                }
            } else {
                sb.append(arg != null ? arg.getClass().getSimpleName() : "null");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // 내부 클래스들
    private static class UserRateLimit {
        private final ConcurrentHashMap<Long, AtomicInteger> requests = new ConcurrentHashMap<>();

        public void addRequest(long timestamp) {
            requests.computeIfAbsent(timestamp / 1000, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public int getRequestCount() {
            return requests.values().stream().mapToInt(AtomicInteger::get).sum();
        }

        public void cleanExpiredRequests(long windowStart) {
            requests.entrySet().removeIf(entry -> (entry.getKey() * 1000) < windowStart);
        }
    }

    private static class UserTokenUsage {
        private final AtomicLong usage = new AtomicLong(0);
        private volatile LocalDateTime lastUpdate = LocalDateTime.now();

        public void addUsage(long tokens) {
            usage.addAndGet(tokens);
            lastUpdate = LocalDateTime.now();
        }

        public long getUsage() {
            return usage.get();
        }

        public void reset() {
            usage.set(0);
            lastUpdate = LocalDateTime.now();
        }

        public boolean isNewDay() {
            return !lastUpdate.toLocalDate().equals(LocalDateTime.now().toLocalDate());
        }

        public boolean isNewMonth() {
            LocalDateTime now = LocalDateTime.now();
            return lastUpdate.getYear() != now.getYear() || lastUpdate.getMonthValue() != now.getMonthValue();
        }
    }

    private static class SpamDetectionInfo {
        private final ConcurrentHashMap<String, AtomicInteger> questionCounts = new ConcurrentHashMap<>();
        private volatile long lastCleanup = System.currentTimeMillis();

        public boolean isDuplicateQuestion(String question) {
            cleanupOldEntries();
            AtomicInteger count = questionCounts.computeIfAbsent(question, k -> new AtomicInteger(0));
            return count.incrementAndGet() > 3; // 동일한 질문 3회 이상 시 스팸으로 판정
        }

        public void addQuestion(String question) {
            questionCounts.computeIfAbsent(question, k -> new AtomicInteger(0)).incrementAndGet();
        }

        private void cleanupOldEntries() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanup > 3600000) { // 1시간마다 정리
                questionCounts.clear();
                lastCleanup = currentTime;
            }
        }
    }

    private static class RepeatedQuestionTracker {
        private final ConcurrentHashMap<String, Long> lastQuestionTime = new ConcurrentHashMap<>();
        private static final long MIN_INTERVAL = 60000; // 1분

        public boolean isRepeatedQuestion(String question) {
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastQuestionTime.get(question);

            if (lastTime != null && (currentTime - lastTime) < MIN_INTERVAL) {
                return true;
            }

            lastQuestionTime.put(question, currentTime);
            return false;
        }

        public void addQuestion(String question) {
            lastQuestionTime.put(question, System.currentTimeMillis());
        }
    }
}