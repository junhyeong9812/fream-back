# ChatQuestion Domain AOP Implementation

## 개요

이 문서는 fream 프로젝트의 ChatQuestion 도메인에서 구현된 AOP(Aspect-Oriented Programming) 시스템에 대한 상세한 설명서입니다. GPT API 사용량 관리, 보안 제어, 성능 모니터링, 감사 추적, 로깅 등 횡단 관심사를 체계적으로 관리하기 위해 5개의 전문화된 AOP 클래스를 구현했습니다.

## 목차

1. [구현 배경 및 목적](#구현-배경-및-목적)
2. [전체 아키텍처](#전체-아키텍처)
3. [AOP 구현체 상세 분석](#aop-구현체-상세-분석)
    - [ChatExceptionAspect](#1-chatexceptionaspect)
    - [ChatPerformanceAspect](#2-chatperformanceaspect)
    - [ChatLoggingAspect](#3-chatloggingaspect)
    - [ChatSecurityAspect](#4-chatsecurityaspect)
    - [ChatAuditAspect](#5-chatauditaspect)
4. [어노테이션 시스템](#어노테이션-시스템)
5. [사용 방법](#사용-방법)
6. [설정 및 커스터마이징](#설정-및-커스터마이징)
7. [모니터링 및 알림](#모니터링-및-알림)
8. [성능 고려사항](#성능-고려사항)
9. [보안 고려사항](#보안-고려사항)

## 구현 배경 및 목적

### 왜 AOP를 도입했는가?

**1. GPT API 비용 관리 필요성**
- GPT API 호출은 토큰 기반으로 과금되어 비용 관리가 중요
- 사용자별, 일별, 월별 사용량 추적 및 제한 필요
- 예상치 못한 과다 사용으로 인한 비용 폭증 방지

**2. 보안 강화 요구사항**
- AI 서비스 특성상 악용 가능성 존재 (스팸, 악성 질문 등)
- 관리자 기능에 대한 강화된 접근 제어
- 개인정보 보호 및 데이터 유출 방지

**3. 성능 및 안정성 보장**
- GPT API 외부 의존성으로 인한 불안정성
- 응답 시간 최적화 및 병목 지점 식별
- 장애 상황에서의 우아한 성능 저하(graceful degradation)

**4. 규정 준수 및 감사 추적**
- AI 서비스 사용에 대한 완전한 추적성 확보
- 규정 준수를 위한 상세한 로그 기록
- 비즈니스 인텔리전스를 위한 데이터 수집

### AOP 선택 이유

**횡단 관심사의 효율적 관리**
- 비즈니스 로직과 인프라 관심사의 분리
- 코드 중복 제거 및 유지보수성 향상
- 선언적 프로그래밍을 통한 가독성 증대

**유연성과 확장성**
- 어노테이션 기반으로 세밀한 제어 가능
- 런타임 시점에서의 동적 기능 적용
- 기존 코드 수정 없이 새로운 기능 추가

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                   ChatQuestion Domain                      │
├─────────────────────────────────────────────────────────────┤
│  Controllers    │    Services    │   Repositories           │
│  - ChatController │  - ChatService  │ - ChatQuestionRepository │
│  - GPTUsageController│ - GPTService   │ - GPTUsageLogRepository  │
│                 │  - GPTUsageService│                      │
├─────────────────────────────────────────────────────────────┤
│                      AOP Layer                              │
├─────────────────────────────────────────────────────────────┤
│ Exception │Performance│ Logging │ Security │ Audit         │
│ Handling  │Monitoring │         │          │ Tracking      │
│           │           │         │          │               │
│ • 재시도   │ • 성능측정  │ • 세션추적│ • 인증/인가│ • 감사로그   │
│ • 폴백    │ • 임계값   │ • API추적 │ • 속도제한 │ • 비용추적   │
│ • 서킷브레이커│ • 메트릭 │ • 마스킹  │ • 콘텐츠필터│ • 품질평가  │
└─────────────────────────────────────────────────────────────┘
```

## AOP 구현체 상세 분석

## 1. ChatExceptionAspect

### 구현 목적
GPT API 호출 실패, 네트워크 오류, 데이터베이스 접근 오류 등 ChatQuestion 도메인에서 발생하는 다양한 예외를 체계적으로 처리하여 서비스의 안정성을 확보합니다.

### 핵심 기능

**1. 예외 처리 전략**
```java
public enum Strategy {
    LOG_ONLY,           // 로그만 남기고 정상 처리
    LOG_AND_RETHROW,    // 로그 남기고 예외 재발생
    TRANSFORM,          // 다른 예외로 변환
    SUPPRESS,           // 예외 무시
    FALLBACK,           // 폴백 응답 반환
    CIRCUIT_BREAKER     // 서킷 브레이커 패턴 적용
}
```

**2. 자동 재시도 메커니즘**
```java
@ChatExceptionHandling(
    retryable = true, 
    maxRetries = 3, 
    retryDelay = 1000
)
```
- GPT API 호출 실패 시 지수 백오프로 재시도
- 네트워크 오류나 일시적 서버 오류에 대한 복원력 제공

**3. 서킷 브레이커 패턴**
```java
private static class CircuitBreakerState {
    public boolean isCircuitOpen() {
        return getFailureRate() > 0.5 && getTotalCalls() >= 10;
    }
}
```
- 연속적인 실패 시 서킷을 오픈하여 추가적인 요청 차단
- 시스템 리소스 보호 및 연쇄 장애 방지

**4. 폴백 응답 제공**
- GPT API 실패 시 미리 정의된 응답 제공
- 사용자 경험 연속성 보장

### 구현 상세

**예외 유형별 처리**
```java
private void handleGPTApiException(GPTApiException gptException) {
    switch (errorCode) {
        case GPT_API_ERROR:
            // API 호출 실패 처리
        case GPT_USAGE_LIMIT_EXCEEDED:
            // 사용량 제한 초과 처리
        case GPT_RESPONSE_PROCESSING_ERROR:
            // 응답 처리 오류 처리
    }
}
```

**메트릭 수집**
- 예외 발생 빈도 및 유형별 통계
- 서킷 브레이커 상태 모니터링
- 재시도 성공/실패율 추적

## 2. ChatPerformanceAspect

### 구현 목적
GPT API 호출 시간, 데이터베이스 쿼리 시간, 전체 응답 시간 등을 모니터링하여 성능 병목 지점을 식별하고 최적화 기회를 제공합니다.

### 핵심 기능

**1. 레이어별 성능 임계값**
```java
// Controller Layer
private static final long CONTROLLER_WARNING_THRESHOLD = 5000L;   // 5초
private static final long CONTROLLER_ERROR_THRESHOLD = 10000L;    // 10초

// Service Layer  
private static final long SERVICE_WARNING_THRESHOLD = 3000L;      // 3초
private static final long SERVICE_ERROR_THRESHOLD = 8000L;        // 8초

// GPT API
private static final long GPT_API_WARNING_THRESHOLD = 5000L;      // 5초
private static final long GPT_API_ERROR_THRESHOLD = 15000L;       // 15초
```

**2. 고급 성능 모니터링**
```java
@ChatPerformance(
    priority = Priority.HIGH,
    monitorConcurrentRequests = true,
    monitorApiFrequency = true,
    evaluateResponseQuality = true
)
```

**3. 메모리 및 CPU 모니터링**
```java
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

long startMemory = memoryBean.getHeapMemoryUsage().getUsed();
long startCpuTime = threadBean.getCurrentThreadCpuTime();
```

**4. 토큰 사용량 추적**
- GPT API 응답에서 토큰 사용량 자동 추출
- 토큰 사용량 임계값 초과 시 알림
- 비용 예측을 위한 데이터 수집

### 성능 통계 수집

**메서드별 성능 통계**
```java
private static class MethodPerformanceStats {
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong successCalls = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong totalTokensUsed = new AtomicLong(0);
    private volatile long minExecutionTime = Long.MAX_VALUE;
    private volatile long maxExecutionTime = Long.MIN_VALUE;
}
```

**주기적 성능 리포트**
```java
log.info("CHAT_PERFORMANCE_REPORT - Method: {}, TotalCalls: {}, SuccessRate: {:.2f}%, " +
         "AvgExecutionTime: {:.2f}ms, MinExecutionTime: {}ms, MaxExecutionTime: {}ms, " +
         "TotalTokens: {}, AvgTokens: {:.1f}",
         methodName, stats.getTotalCalls(), stats.getSuccessRate(), 
         stats.getAverageExecutionTime(), stats.getMinExecutionTime(), 
         stats.getMaxExecutionTime(), stats.getTotalTokensUsed(), stats.getAverageTokensUsed());
```

## 3. ChatLoggingAspect

### 구현 목적
ChatQuestion 도메인의 모든 활동을 상세하게 기록하되, 개인정보 보호를 위한 마스킹 처리를 통해 보안과 추적성을 동시에 보장합니다.

### 핵심 기능

**1. 세밀한 로깅 제어**
```java
public enum LogType {
    EXECUTION_TIME,    // 실행 시간
    PARAMETERS,        // 메서드 파라미터
    RESULT,           // 반환값
    USER_INFO,        // 사용자 정보
    REQUEST_ID,       // 요청 ID
    GPT_USAGE,        // GPT 토큰 사용량
    QUESTION_SUMMARY, // 질문 요약
    RESPONSE_SUMMARY  // 응답 요약
}
```

**2. 개인정보 보호**
```java
private String maskSensitiveContent(String content) {
    // 이메일 주소 마스킹
    content = content.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "***@***.***");
    
    // 전화번호 마스킹
    content = content.replaceAll("\\b\\d{2,3}-\\d{3,4}-\\d{4}\\b", "***-****-****");
    
    // 주민등록번호 마스킹
    content = content.replaceAll("\\b\\d{6}-\\d{7}\\b", "******-*******");
}
```

**3. 사용자 세션 추적**
```java
private String trackUserSession(String userEmail) {
    return userSessions.computeIfAbsent(userEmail, k -> {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        log.info("CHAT_SESSION_START - User: {}, SessionId: {}, Timestamp: {}", 
                userEmail, sessionId, LocalDateTime.now());
        return sessionId;
    });
}
```

**4. GPT API 호출 추적**
```java
private static class GPTApiCallInfo {
    private final String requestId;
    private final String userEmail;
    private final String model;
    private final int tokensUsed;
    private final long executionTime;
    private final LocalDateTime timestamp;
}
```

### 로깅 레벨별 처리
- **TRACE**: 상세한 디버깅 정보
- **DEBUG**: 개발자를 위한 상세 정보
- **INFO**: 일반적인 애플리케이션 흐름
- **WARN**: 주의가 필요한 상황
- **ERROR**: 오류 상황

## 4. ChatSecurityAspect

### 구현 목적
AI 서비스의 특성상 발생할 수 있는 다양한 보안 위협으로부터 시스템을 보호하고, 적절한 사용량 제한을 통해 서비스 안정성을 확보합니다.

### 핵심 기능

**1. 다층 보안 검증**
```java
public enum SecurityCheck {
    AUTHENTICATION,          // 인증 확인
    AUTHORIZATION,          // 인가 확인
    RATE_LIMIT,            // 속도 제한 확인
    IP_RESTRICTION,        // IP 제한 확인
    TIME_RESTRICTION,      // 시간 제한 확인
    TOKEN_LIMIT,          // 토큰 사용량 제한
    CONTENT_FILTER,       // 콘텐츠 필터링
    SPAM_DETECTION,       // 스팸 감지
    REPEATED_QUESTION,    // 반복 질문 제한
    API_KEY_PROTECTION,   // API 키 보호
    SENSITIVE_DATA_BLOCK  // 민감한 데이터 요청 차단
}
```

**2. 속도 제한 (Rate Limiting)**
```java
@ChatSecurity(
    enableRateLimit = true,
    rateLimitWindow = 60,      // 60초 윈도우
    maxRequestsPerWindow = 10   // 최대 10회 요청
)
```

**3. 토큰 사용량 제한**
```java
@ChatSecurity(
    dailyTokenLimit = 10000,    // 일일 10,000 토큰
    monthlyTokenLimit = 100000  // 월간 100,000 토큰
)
```

**4. 콘텐츠 필터링**
```java
private static final Pattern[] MALICIOUS_PATTERNS = {
    Pattern.compile("(?i).*(hack|crack|exploit|vulnerability).*"),
    Pattern.compile("(?i).*(password|credential|token|key).*leak.*"),
    Pattern.compile("(?i).*personal.*information.*(leak|steal|obtain).*"),
    Pattern.compile("(?i).*(bomb|weapon|terrorist|illegal).*"),
    Pattern.compile("(?i).*(adult|sexual|inappropriate).*content.*")
};
```

**5. 스팸 및 반복 질문 감지**
```java
private static class SpamDetectionInfo {
    public boolean isDuplicateQuestion(String question) {
        AtomicInteger count = questionCounts.computeIfAbsent(question, k -> new AtomicInteger(0));
        return count.incrementAndGet() > 3; // 동일한 질문 3회 이상 시 스팸으로 판정
    }
}
```

### 보안 위반 처리 전략
```java
public enum ViolationAction {
    THROW_EXCEPTION,     // 예외 발생
    LOG_AND_DENY,       // 로그 남기고 접근 거부
    LOG_AND_THROTTLE,   // 로그 남기고 요청 지연
    SILENT_DENY,        // 조용히 접근 거부
    FALLBACK_RESPONSE,  // 기본 응답 반환
    BLACKLIST_USER      // 사용자 블랙리스트 추가
}
```

## 5. ChatAuditAspect

### 구현 목적
규정 준수, 비즈니스 인텔리전스, 보안 감사를 위해 ChatQuestion 도메인의 모든 중요한 활동을 추적하고 기록합니다.

### 핵심 기능

**1. 감사 이벤트 분류**
```java
public enum AuditEvent {
    CHAT_QUESTION_SUBMITTED("채팅 질문 제출"),
    CHAT_QUESTION_PROCESSED("채팅 질문 처리 완료"),
    GPT_API_CALLED("GPT API 호출"),
    GPT_API_RESPONSE_RECEIVED("GPT API 응답 수신"),
    TOKEN_USAGE_RECORDED("토큰 사용량 기록"),
    USAGE_STATS_ACCESSED("사용량 통계 조회"),
    CHAT_HISTORY_ACCESSED("채팅 기록 조회"),
    ADMIN_ACCESS("관리자 접근"),
    HIGH_TOKEN_USAGE("높은 토큰 사용량"),
    HIGH_COST_INCURRED("높은 비용 발생"),
    SUSPICIOUS_ACTIVITY("의심스러운 활동")
}
```

**2. 비용 추적 및 분석**
```java
private int calculateTokenCost(String model, int tokens) {
    double costPerToken = 0.0;
    
    if (model != null) {
        if (model.contains("gpt-4")) {
            costPerToken = 0.03 / 1000.0; // GPT-4: $0.03/1K tokens
        } else if (model.contains("gpt-3.5")) {
            costPerToken = 0.0015 / 1000.0; // GPT-3.5: $0.0015/1K tokens
        }
    }
    
    return (int) Math.ceil(tokens * costPerToken * 100); // 센트 단위로 반환
}
```

**3. 품질 메트릭 평가**
```java
private int evaluateResponseQuality(String answer, long responseTime) {
    int score = 50; // 기본 점수
    
    // 답변 길이 점수
    if (answer.length() > 100) score += 10;
    if (answer.length() > 300) score += 10;
    
    // 응답 시간 점수
    if (responseTime < 3000) score += 15;
    else if (responseTime > 10000) score -= 10;
    
    // 내용 품질 점수
    if (!answer.toLowerCase().contains("죄송") && !answer.toLowerCase().contains("미안")) {
        score += 10;
    }
    
    return Math.min(100, Math.max(0, score));
}
```

**4. 민감한 데이터 마스킹**
```java
private String maskSensitiveContent(String content) {
    if (content == null) return null;
    
    // 이메일 주소 마스킹
    content = content.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "***@***.***");
    
    // 전화번호 마스킹  
    content = content.replaceAll("\\b\\d{2,3}-\\d{3,4}-\\d{4}\\b", "***-****-****");
    
    // 주민등록번호 마스킹
    content = content.replaceAll("\\b\\d{6}-\\d{7}\\b", "******-*******");
    
    return content;
}
```

### 감사 레벨별 처리
- **DEBUG**: 개발 및 디버깅용 상세 정보
- **INFO**: 일반적인 비즈니스 활동
- **WARN**: 주의가 필요한 활동
- **ERROR**: 오류 및 예외 상황
- **SECURITY**: 보안 관련 중요 이벤트
- **COMPLIANCE**: 규정 준수 관련 활동
- **FINANCIAL**: 비용 및 재무 관련 활동

## 어노테이션 시스템

### 설계 철학
각 AOP 기능을 독립적으로 제어할 수 있도록 전용 어노테이션을 설계했습니다. 이를 통해 메서드별로 필요한 기능만 선택적으로 적용할 수 있습니다.

### 1. @ChatExceptionHandling
```java
@ChatExceptionHandling(
    strategy = Strategy.FALLBACK,
    retryable = true,
    maxRetries = 3,
    retryDelay = 1000,
    fallbackMessage = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
)
```

**주요 속성**
- `strategy`: 예외 처리 전략 선택
- `retryable`: 재시도 활성화 여부
- `maxRetries`: 최대 재시도 횟수
- `retryDelay`: 재시도 간격
- `fallbackMessage`: 폴백 응답 메시지

### 2. @ChatPerformance
```java
@ChatPerformance(
    warningThreshold = 3000,
    errorThreshold = 8000,
    priority = Priority.HIGH,
    monitorConcurrentRequests = true,
    tokenUsageThreshold = 2000
)
```

**주요 속성**
- `warningThreshold`: 성능 경고 임계값 (ms)
- `errorThreshold`: 성능 오류 임계값 (ms)
- `priority`: 모니터링 우선순위
- `tokenUsageThreshold`: 토큰 사용량 임계값

### 3. @ChatLogging
```java
@ChatLogging(
    level = LogLevel.INFO,
    types = {LogType.EXECUTION_TIME, LogType.USER_INFO, LogType.GPT_USAGE},
    maskQuestionContent = true,
    trackUserSession = true
)
```

**주요 속성**
- `level`: 로깅 레벨
- `types`: 로깅할 정보 타입들
- `maskQuestionContent`: 질문 내용 마스킹 여부
- `trackUserSession`: 사용자 세션 추적 여부

### 4. @ChatSecurity
```java
@ChatSecurity(
    checks = {SecurityCheck.AUTHENTICATION, SecurityCheck.RATE_LIMIT, SecurityCheck.CONTENT_FILTER},
    enableRateLimit = true,
    maxRequestsPerWindow = 10,
    dailyTokenLimit = 10000,
    detectSpam = true
)
```

**주요 속성**
- `checks`: 수행할 보안 검증 타입들
- `enableRateLimit`: 속도 제한 활성화
- `maxRequestsPerWindow`: 윈도우당 최대 요청 수
- `dailyTokenLimit`: 일일 토큰 사용 제한

### 5. @ChatAudit
```java
@ChatAudit(
    event = AuditEvent.CHAT_QUESTION_PROCESSED,
    level = AuditLevel.INFO,
    recordTokenUsage = true,
    calculateCost = true,
    recordQualityMetrics = true
)
```

**주요 속성**
- `event`: 감사 이벤트 타입
- `level`: 감사 레벨
- `recordTokenUsage`: 토큰 사용량 기록 여부
- `calculateCost`: 비용 계산 여부

## 사용 방법

### 1. 기본 사용 예제

**채팅 질문 처리 메서드**
```java
@Service
public class ChatService {
    
    @ChatAudit(event = ChatAudit.AuditEvent.CHAT_QUESTION_SUBMITTED)
    @ChatSecurity(checks = {SecurityCheck.AUTHENTICATION, SecurityCheck.RATE_LIMIT})
    @ChatPerformance(priority = Priority.HIGH)
    @ChatExceptionHandling(strategy = Strategy.FALLBACK, retryable = true)
    @ChatLogging(types = {LogType.EXECUTION_TIME, LogType.GPT_USAGE})
    public QuestionResponseDto processQuestion(String email, QuestionRequestDto requestDto) {
        // 메서드 구현
    }
}
```

**GPT API 호출 메서드**
```java
@Service
public class GPTService {
    
    @ChatPerformance(
        warningThreshold = 5000,
        errorThreshold = 15000,
        priority = Priority.CRITICAL
    )
    @ChatExceptionHandling(
        strategy = Strategy.CIRCUIT_BREAKER,
        retryable = true,
        maxRetries = 3
    )
    @ChatAudit(
        event = AuditEvent.GPT_API_CALLED,
        recordTokenUsage = true,
        calculateCost = true
    )
    public GPTResponseDto getGPTResponseWithUsage(String question, List<FAQResponseDto> faqList) {
        // GPT API 호출 구현
    }
}
```

**관리자 전용 메서드**
```java
@RestController
public class GPTUsageController {
    
    @ChatAudit(
        event = AuditEvent.ADMIN_ACCESS,
        level = AuditLevel.SECURITY
    )
    @ChatSecurity(
        requireAdminRole = true,
        checks = {SecurityCheck.AUTHENTICATION, SecurityCheck.AUTHORIZATION}
    )
    @ChatLogging(
        level = LogLevel.WARN,
        types = {LogType.USER_INFO, LogType.PARAMETERS}
    )
    public ResponseEntity<ResponseDto<GPTUsageStatsDto>> getUsageStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        // 관리자 기능 구현
    }
}
```

### 2. 환경별 설정 예제

**개발 환경**
```java
@ChatPerformance(
    warningThreshold = 1000,  // 더 엄격한 임계값
    errorThreshold = 3000,
    priority = Priority.HIGH
)
@ChatLogging(
    level = LogLevel.DEBUG,   // 상세한 로깅
    types = {LogType.EXECUTION_TIME, LogType.PARAMETERS, LogType.RESULT}
)
```

**운영 환경**
```java
@ChatPerformance(
    warningThreshold = 5000,  // 더 관대한 임계값
    errorThreshold = 10000,
    priority = Priority.NORMAL
)
@ChatLogging(
    level = LogLevel.INFO,    // 필수 정보만 로깅
    types = {LogType.EXECUTION_TIME, LogType.USER_INFO}
)
```

## 설정 및 커스터마이징

### 1. 성능 임계값 조정

**레이어별 기본 임계값**
```java
// application.yml
chat:
  performance:
    controller:
      warning-threshold: 5000
      error-threshold: 10000
    service:
      warning-threshold: 3000
      error-threshold: 8000
    gpt-api:
      warning-threshold: 5000
      error-threshold: 15000
```

### 2. 보안 설정

**속도 제한 설정**
```java
// application.yml
chat:
  security:
    rate-limit:
      enabled: true
      window-seconds: 60
      max-requests: 10
    token-limit:
      daily-limit: 10000
      monthly-limit: 100000
```

### 3. 감사 로그 보존 설정

**로그 보존 정책**
```java
// application.yml
chat:
  audit:
    retention-days: 365
    cost-threshold-cents: 100
    external-system:
      enabled: false
      endpoint: "https://audit-system.example.com/api/events"
```

## 모니터링 및 알림

### 1. 성능 모니터링

**메트릭 수집**
```java
// 예제 메트릭 로그
METRICS: performance.chat.chatservice_processquestion execution_time=2340 success=true user=user@example.com tokens=1250

CHAT_PERFORMANCE_REPORT - Method: ChatService.processQuestion, TotalCalls: 100, SuccessRate: 98.00%, 
AvgExecutionTime: 2456.78ms, MinExecutionTime: 1234ms, MaxExecutionTime: 5678ms, 
TotalTokens: 125000, AvgTokens: 1250.0
```

**알림 조건**
- 응답 시간이 임계값 초과
- 토큰 사용량이 임계값 초과
- 서킷 브레이커 동작
- 예외 발생률이 10% 초과

### 2. 보안 모니터링

**보안 이벤트 로그**
```java
SECURITY_AUDIT - Event: RATE_LIMIT_EXCEEDED, Method: ChatController.askQuestion, 
User: user@example.com, IP: 192.168.1.100, Args: [QuestionRequest{question='test...'}], 
Timestamp: 2024-01-15 10:30:45

ALERT[WARN]: Chat Exception - Class: ChatService, Method: processQuestion, 
User: user@example.com, Exception: ChatPermissionException
```

### 3. 비용 모니터링

**비용 추적 로그**
```java
COST_TRACKING - User: user@example.com, Tokens: 1250, EstimatedCostCents: 5, TotalCostToday: 125

FINANCIAL_AUDIT - AuditId: AUDIT_A1B2C3D4E5F6, Event: HIGH_COST_INCURRED, 
User: user@example.com, Method: ChatService.processQuestion, 
Timestamp: 2024-01-15 10:30:45.123
```

## 성능 고려사항

### 1. AOP 오버헤드 최소화

**효율적인 포인트컷 사용**
```java
// 좋은 예: 구체적인 포인트컷
@Around("@annotation(chatPerformance)")

// 피해야 할 예: 너무 광범위한 포인트컷  
@Around("execution(* com.fream.back..*(..))")
```

**조건부 실행**
```java
@ChatPerformance(enabled = false)  // 개발 시 성능 모니터링 비활성화
@ChatLogging(level = LogLevel.ERROR)  // 운영 시 최소한의 로깅
```

### 2. 메모리 관리

**정기적인 데이터 정리**
```java
private void cleanupOldEntries() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastCleanup > 3600000) { // 1시간마다 정리
        rateLimitMap.clear();
        spamDetectionMap.clear();
        lastCleanup = currentTime;
    }
}
```

**메모리 효율적인 데이터 구조 사용**
```java
private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
// vs 
private final Map<String, Long> counters = Collections.synchronizedMap(new HashMap<>());
```

### 3. 동시성 처리

**스레드 안전한 구현**
```java
private final ConcurrentHashMap<String, UserRateLimit> rateLimitMap = new ConcurrentHashMap<>();
private final AtomicLong eventCounter = new AtomicLong(0);
```

## 보안 고려사항

### 1. 민감한 데이터 보호

**로그에서 개인정보 마스킹**
```java
// 이메일 마스킹: user@example.com → u***r@example.com
// 전화번호 마스킹: 010-1234-5678 → ***-****-****  
// 주민등록번호 마스킹: 123456-1234567 → ******-*******
```

**질문 내용 요약**
```java
// 긴 질문 내용 → 처음 30자 + "..."
// 개인정보 포함 가능성 → 전체 마스킹
```

### 2. 접근 제어

**계층적 보안 검증**
```java
@ChatSecurity(
    requireAuthentication = true,     // 1단계: 인증 확인
    requireAdminRole = true,          // 2단계: 권한 확인  
    checks = {SecurityCheck.IP_RESTRICTION}  // 3단계: IP 검증
)
```

**감사 로그 무결성**
```java
// 감사 로그는 별도 저장소에 저장
// 변조 방지를 위한 체크섬 적용
// 정기적인 백업 및 아카이빙
```

### 3. API 키 보호

**환경 변수 사용**
```java
// application.yml
gpt:
  api-key: ${GPT_API_KEY}  # 환경 변수에서 로드
  
// 로그에서 API 키 마스킹
log.info("GPT API 호출 - Model: {}, Key: ****", model);
```

## 결론

이 AOP 시스템을 통해 ChatQuestion 도메인에서는:

1. **운영 안정성**: 예외 처리, 재시도, 서킷 브레이커를 통한 장애 복구력 확보
2. **성능 최적화**: 실시간 모니터링과 병목 지점 식별을 통한 지속적 개선
3. **보안 강화**: 다층 보안 검증과 악용 방지를 통한 안전한 서비스 제공
4. **규정 준수**: 완전한 감사 추적을 통한 규정 준수 및 비즈니스 인텔리전스
5. **개발 생산성**: 선언적 프로그래밍을 통한 유지보수성 향상

각 AOP 구현체는 독립적으로 동작하면서도 상호 보완적인 역할을 수행하여, 전체적으로 견고하고 신뢰할 수 있는 AI 서비스 플랫폼을 구축할 수 있습니다.