package com.fream.back.domain.chatQuestion.aop;

import com.fream.back.domain.chatQuestion.aop.annotation.ChatLogging;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionRequestDto;
import com.fream.back.domain.chatQuestion.dto.chat.QuestionResponseDto;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatQuestion 도메인 로깅 AOP
 * @ChatLogging 어노테이션을 기반으로 세밀한 로깅 제어 수행
 */
@Aspect
@Component
@Slf4j
public class ChatLoggingAspect {

    // 사용자 세션 추적을 위한 저장소
    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();

    // GPT API 호출 추적을 위한 저장소
    private final ConcurrentHashMap<String, GPTApiCallInfo> gptApiCalls = new ConcurrentHashMap<>();

    /**
     * @ChatLogging 어노테이션이 붙은 메서드의 로깅 처리
     */
    @Around("@annotation(chatLogging)")
    public Object handleAnnotatedLogging(ProceedingJoinPoint proceedingJoinPoint,
                                         ChatLogging chatLogging) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();
        String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        Object[] args = proceedingJoinPoint.getArgs();
        String userEmail = extractUserEmailSafely();

        // 요청 ID 생성
        String requestId = generateRequestId();

        // 사용자 세션 추적
        String sessionId = null;
        if (chatLogging.trackUserSession()) {
            sessionId = trackUserSession(userEmail);
        }

        // 메서드 실행 전 로깅
        if (chatLogging.logBefore()) {
            logBefore(chatLogging, fullMethodName, userEmail, requestId, sessionId, args);
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = proceedingJoinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // 메서드 실행 후 로깅
            if (chatLogging.logAfter()) {
                logAfter(chatLogging, fullMethodName, userEmail, requestId, sessionId,
                        executionTime, result, args);
            }

            // GPT API 호출 추적
            if (chatLogging.trackGPTApiCall() && result instanceof GPTResponseDto) {
                trackGPTApiCall(requestId, userEmail, (GPTResponseDto) result, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // 예외 발생 시 로깅
            if (chatLogging.logException()) {
                logException(chatLogging, fullMethodName, userEmail, requestId, sessionId,
                        executionTime, e, args);
            }

            throw e;
        }
    }

    /**
     * Chat Service 메서드 기본 로깅
     */
    @Before("execution(* com.fream.back.domain.chatQuestion.service..*(..))")
    public void logServiceMethodEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String userEmail = extractUserEmailSafely();

        log.debug("CHAT_SERVICE_ENTRY - Class: {}, Method: {}, User: {}",
                className, methodName, userEmail);
    }

    /**
     * Chat Controller 메서드 기본 로깅
     */
    @Before("execution(* com.fream.back.domain.chatQuestion.controller..*(..))")
    public void logControllerMethodEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String userEmail = extractUserEmailSafely();
        Object[] args = joinPoint.getArgs();

        log.info("CHAT_CONTROLLER_ENTRY - Class: {}, Method: {}, User: {}, Args: {}",
                className, methodName, userEmail, formatArgsForLogging(args, true));
    }

    /**
     * 메서드 실행 전 로깅
     */
    private void logBefore(ChatLogging chatLogging, String fullMethodName, String userEmail,
                           String requestId, String sessionId, Object[] args) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("CHAT_METHOD_START - Method: ").append(fullMethodName)
                .append(", User: ").append(userEmail)
                .append(", RequestId: ").append(requestId);

        if (sessionId != null) {
            logMessage.append(", SessionId: ").append(sessionId);
        }

        // 로깅 타입에 따른 정보 추가
        for (ChatLogging.LogType type : chatLogging.types()) {
            switch (type) {
                case PARAMETERS:
                    if (args != null && args.length > 0) {
                        logMessage.append(", Parameters: ")
                                .append(formatArgsForLogging(args, chatLogging.maskQuestionContent()));
                    }
                    break;
                case REQUEST_ID:
                    logMessage.append(", RequestId: ").append(requestId);
                    break;
                case QUESTION_SUMMARY:
                    String questionSummary = extractQuestionSummary(args, chatLogging.maskQuestionContent());
                    if (!questionSummary.isEmpty()) {
                        logMessage.append(", QuestionSummary: ").append(questionSummary);
                    }
                    break;
            }
        }

        if (!chatLogging.message().isEmpty()) {
            logMessage.append(", CustomMessage: ").append(chatLogging.message());
        }

        logAtLevel(chatLogging.level(), logMessage.toString());
    }

    /**
     * 메서드 실행 후 로깅
     */
    private void logAfter(ChatLogging chatLogging, String fullMethodName, String userEmail,
                          String requestId, String sessionId, long executionTime,
                          Object result, Object[] args) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("CHAT_METHOD_END - Method: ").append(fullMethodName)
                .append(", User: ").append(userEmail)
                .append(", RequestId: ").append(requestId);

        if (sessionId != null) {
            logMessage.append(", SessionId: ").append(sessionId);
        }

        // 로깅 타입에 따른 정보 추가
        for (ChatLogging.LogType type : chatLogging.types()) {
            switch (type) {
                case EXECUTION_TIME:
                    logMessage.append(", ExecutionTime: ").append(executionTime).append("ms");
                    break;
                case RESULT:
                    if (result != null) {
                        logMessage.append(", ResultType: ").append(result.getClass().getSimpleName());
                    }
                    break;
                case GPT_USAGE:
                    String tokenInfo = extractTokenUsageInfo(result);
                    if (!tokenInfo.isEmpty()) {
                        logMessage.append(", ").append(tokenInfo);
                    }
                    break;
                case RESPONSE_SUMMARY:
                    String responseSummary = extractResponseSummary(result);
                    if (!responseSummary.isEmpty()) {
                        logMessage.append(", ResponseSummary: ").append(responseSummary);
                    }
                    break;
            }
        }

        // GPT 응답 내용 로깅
        if (chatLogging.logGPTResponse() && result instanceof GPTResponseDto) {
            GPTResponseDto gptResponse = (GPTResponseDto) result;
            String answer = gptResponse.getAnswer();
            if (answer != null && answer.length() > 100) {
                logMessage.append(", GPTResponse: ").append(answer.substring(0, 100)).append("...");
            } else {
                logMessage.append(", GPTResponse: ").append(answer);
            }
        }

        logAtLevel(chatLogging.level(), logMessage.toString());
    }

    /**
     * 예외 발생 시 로깅
     */
    private void logException(ChatLogging chatLogging, String fullMethodName, String userEmail,
                              String requestId, String sessionId, long executionTime,
                              Exception exception, Object[] args) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("CHAT_METHOD_EXCEPTION - Method: ").append(fullMethodName)
                .append(", User: ").append(userEmail)
                .append(", RequestId: ").append(requestId)
                .append(", ExecutionTime: ").append(executionTime).append("ms")
                .append(", Exception: ").append(exception.getClass().getSimpleName())
                .append(", Message: ").append(exception.getMessage());

        if (sessionId != null) {
            logMessage.append(", SessionId: ").append(sessionId);
        }

        if (chatLogging.maskQuestionContent() && args != null) {
            logMessage.append(", Parameters: ").append(formatArgsForLogging(args, true));
        }

        log.error(logMessage.toString(), exception);
    }

    /**
     * 사용자 세션 추적
     */
    private String trackUserSession(String userEmail) {
        return userSessions.computeIfAbsent(userEmail, k -> {
            String sessionId = UUID.randomUUID().toString().substring(0, 8);
            log.info("CHAT_SESSION_START - User: {}, SessionId: {}, Timestamp: {}",
                    userEmail, sessionId, LocalDateTime.now());
            return sessionId;
        });
    }

    /**
     * GPT API 호출 추적
     */
    private void trackGPTApiCall(String requestId, String userEmail, GPTResponseDto response, long executionTime) {
        GPTApiCallInfo callInfo = new GPTApiCallInfo(
                requestId, userEmail, response.getModel(),
                response.getUsage() != null ? response.getUsage().getTotal_tokens() : 0,
                executionTime, LocalDateTime.now()
        );

        gptApiCalls.put(requestId, callInfo);

        log.info("GPT_API_CALL_TRACKED - RequestId: {}, User: {}, Model: {}, Tokens: {}, ExecutionTime: {}ms",
                requestId, userEmail, callInfo.getModel(), callInfo.getTokensUsed(), executionTime);
    }

    /**
     * 질문 요약 추출
     */
    private String extractQuestionSummary(Object[] args, boolean maskContent) {
        if (args == null || args.length == 0) return "";

        for (Object arg : args) {
            if (arg instanceof QuestionRequestDto) {
                QuestionRequestDto request = (QuestionRequestDto) arg;
                String question = request.getQuestion();
                if (question != null) {
                    if (maskContent) {
                        return question.length() > 30 ?
                                question.substring(0, 30) + "..." : question;
                    } else {
                        return "[MASKED_CONTENT]";
                    }
                }
            }
            if (arg instanceof String && ((String) arg).length() > 10) {
                String str = (String) arg;
                if (maskContent) {
                    return str.length() > 30 ? str.substring(0, 30) + "..." : str;
                } else {
                    return "[MASKED_CONTENT]";
                }
            }
        }
        return "";
    }

    /**
     * 응답 요약 추출
     */
    private String extractResponseSummary(Object result) {
        if (result instanceof QuestionResponseDto) {
            QuestionResponseDto response = (QuestionResponseDto) result;
            String answer = response.getAnswer();
            return answer != null && answer.length() > 50 ?
                    answer.substring(0, 50) + "..." : (answer != null ? answer : "");
        }
        if (result instanceof GPTResponseDto) {
            GPTResponseDto gptResponse = (GPTResponseDto) result;
            String answer = gptResponse.getAnswer();
            return answer != null && answer.length() > 50 ?
                    answer.substring(0, 50) + "..." : (answer != null ? answer : "");
        }
        return "";
    }

    /**
     * 토큰 사용량 정보 추출
     */
    private String extractTokenUsageInfo(Object result) {
        if (result instanceof GPTResponseDto) {
            GPTResponseDto gptResponse = (GPTResponseDto) result;
            if (gptResponse.getUsage() != null) {
                return String.format("TokenUsage: %d (prompt: %d, completion: %d)",
                        gptResponse.getUsage().getTotal_tokens(),
                        gptResponse.getUsage().getPrompt_tokens(),
                        gptResponse.getUsage().getCompletion_tokens());
            }
        }
        return "";
    }

    /**
     * 인자들을 로깅용으로 포맷팅 (마스킹 처리 포함)
     */
    private String formatArgsForLogging(Object[] args, boolean maskQuestionContent) {
        if (args == null || args.length == 0) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof QuestionRequestDto) {
                QuestionRequestDto request = (QuestionRequestDto) arg;
                if (maskQuestionContent) {
                    String question = request.getQuestion();
                    String maskedQuestion = question != null && question.length() > 30 ?
                            question.substring(0, 30) + "..." : question;
                    sb.append("QuestionRequest{question='").append(maskedQuestion).append("'}");
                } else {
                    sb.append("QuestionRequest{question='[MASKED]'}");
                }
            } else if (arg instanceof String) {
                String str = (String) arg;
                if (str.contains("@")) {
                    sb.append("email:").append(maskEmail(str));
                } else if (str.length() > 50 && maskQuestionContent) {
                    sb.append("'").append(str.substring(0, 50)).append("...'");
                } else if (!maskQuestionContent && str.length() > 10) {
                    sb.append("'[MASKED_CONTENT]'");
                } else {
                    sb.append("'").append(str).append("'");
                }
            } else {
                sb.append(arg.getClass().getSimpleName())
                        .append("@").append(Integer.toHexString(arg.hashCode()));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 로그 레벨에 따라 로깅
     */
    private void logAtLevel(ChatLogging.LogLevel level, String message) {
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
            default:
                log.info(message);
        }
    }

    /**
     * 요청 ID 생성
     */
    private String generateRequestId() {
        return "REQ_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 안전하게 사용자 이메일 추출
     */
    private String extractUserEmailSafely() {
        try {
            return SecurityUtils.extractEmailOrAnonymous();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 이메일 마스킹 처리
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";

        int atIndex = email.indexOf("@");
        if (atIndex <= 0) return "***";

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domainPart;
        } else {
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
        }
    }

    /**
     * GPT API 호출 정보를 저장하는 내부 클래스
     */
    private static class GPTApiCallInfo {
        private final String requestId;
        private final String userEmail;
        private final String model;
        private final int tokensUsed;
        private final long executionTime;
        private final LocalDateTime timestamp;

        public GPTApiCallInfo(String requestId, String userEmail, String model,
                              int tokensUsed, long executionTime, LocalDateTime timestamp) {
            this.requestId = requestId;
            this.userEmail = userEmail;
            this.model = model;
            this.tokensUsed = tokensUsed;
            this.executionTime = executionTime;
            this.timestamp = timestamp;
        }

        // Getters
        public String getRequestId() { return requestId; }
        public String getUserEmail() { return userEmail; }
        public String getModel() { return model; }
        public int getTokensUsed() { return tokensUsed; }
        public long getExecutionTime() { return executionTime; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}