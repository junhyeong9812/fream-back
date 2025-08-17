# AccessLog AOP 기반 API 설계 문서

## 개요

본 문서는 Spring AOP를 활용한 AccessLog 도메인의 관심사 분리 및 자동화 시스템에 대한 설계 및 사용 가이드입니다. 이 시스템은 어노테이션 기반으로 예외 처리, 로깅, 성능 모니터링을 자동화하여 코드의 가독성과 유지보수성을 크게 향상시킵니다.

## 시스템 아키텍처

### 핵심 구성 요소

1. **어노테이션 계층 (Annotation Layer)**
    - `@AccessLogExceptionHandler`: 예외 처리 자동화
    - `@AccessLogMethodLogger`: 메서드 로깅 자동화
    - `@AccessLogPerformanceMonitor`: 성능 모니터링 자동화

2. **Aspect 계층 (Aspect Layer)**
    - `AccessLogExceptionAspect`: 예외 처리 및 변환
    - `AccessLogMethodLoggingAspect`: 메서드 로깅
    - `AccessLogPerformanceAspect`: 성능 측정 및 분석

3. **실행 순서 (Order)**
    - Order(1): 예외 처리 (최우선)
    - Order(2): 메서드 로깅
    - Order(3): 성능 모니터링 (최내부)

## API 상세 가이드

### 1. @AccessLogExceptionHandler

#### 기본 사용법

```java
@AccessLogExceptionHandler
public void basicExample() {
    // 예외 발생 시 자동으로 AccessLogException으로 변환
    throw new RuntimeException("원본 예외");
}
```

#### 고급 사용법

```java
@AccessLogExceptionHandler(
    defaultType = ExceptionType.SAVE,
    message = "사용자 접근 로그 저장 실패",
    logLevel = LogLevel.ERROR,
    retry = true,
    retryCount = 3
)
public void saveAccessLog(AccessLogData data) {
    // 저장 실패 시 3번까지 재시도
    // 실패하면 AccessLogSaveException으로 변환
    accessLogRepository.save(data);
}
```

#### 예외 타입별 사용 사례

```java
// 데이터베이스 저장 관련
@AccessLogExceptionHandler(defaultType = ExceptionType.SAVE)
public void saveAccessData() { }

// 데이터 조회 관련
@AccessLogExceptionHandler(defaultType = ExceptionType.QUERY)
public List<AccessLog> findAccessLogs() { }

// Kafka 메시징 관련
@AccessLogExceptionHandler(defaultType = ExceptionType.KAFKA)
public void sendToKafka() { }

// IP 위치 조회 관련
@AccessLogExceptionHandler(defaultType = ExceptionType.GEO_IP)
public LocationInfo getLocationFromIP() { }

// 파라미터 검증 관련
@AccessLogExceptionHandler(defaultType = ExceptionType.VALIDATION)
public void validateAccessData() { }
```

#### 재시도 전략

```java
@AccessLogExceptionHandler(
    retry = true,
    retryCount = 5,
    defaultType = ExceptionType.KAFKA
)
public void sendKafkaMessage() {
    // 네트워크 오류 시 지수 백오프로 재시도
    // 1초 -> 2초 -> 4초 -> 8초 -> 10초 (최대)
}
```

### 2. @AccessLogMethodLogger

#### 기본 로깅

```java
@AccessLogMethodLogger
public void basicLogging() {
    // [진입] 메서드 시작, [종료] 메서드 완료 로그 자동 생성
}
```

#### 상세 로깅 설정

```java
@AccessLogMethodLogger(
    level = LogLevel.INFO,
    logParameters = true,
    logReturnValue = false,
    measureExecutionTime = true,
    customMessage = "사용자 접근 로그 처리"
)
public AccessLogResponse processAccessLog(String userId, String requestPath) {
    // 파라미터와 실행 시간이 로깅됨
    // 반환값은 로깅되지 않음 (민감한 정보 보호)
    return new AccessLogResponse();
}
```

#### 보안이 중요한 메서드

```java
@AccessLogMethodLogger(
    logParameters = false,  // 개인정보 포함된 파라미터 숨김
    logReturnValue = false, // 민감한 반환값 숨김
    level = LogLevel.WARN
)
public UserProfile getUserProfile(String personalId) {
    // 개인정보는 로깅하지 않음
    return userService.getProfile(personalId);
}
```

#### 디버깅용 상세 로깅

```java
@AccessLogMethodLogger(
    level = LogLevel.DEBUG,
    logParameters = true,
    logReturnValue = true,
    customMessage = "디버깅: 접근 로그 데이터 변환"
)
public ProcessedData debugDataProcessing(RawData input) {
    // 개발 환경에서만 상세 정보 로깅
    return processData(input);
}
```

### 3. @AccessLogPerformanceMonitor

#### 기본 성능 모니터링

```java
@AccessLogPerformanceMonitor
public void basicPerformanceMonitoring() {
    // 1초 이상 실행시 경고 로그 생성
}
```

#### 맞춤형 성능 모니터링

```java
@AccessLogPerformanceMonitor(
    thresholdMs = 500L,           // 500ms 초과시 경고
    measureMemory = true,         // 메모리 사용량 측정
    collectMetrics = true,        // 통계 수집
    slowQueryThresholdMs = 2000L, // 2초 초과시 슬로우 쿼리
    enablePerformanceGrading = true // 성능 등급 분류
)
public List<AccessLog> heavyDataProcessing() {
    // 메모리 집약적인 작업의 성능 모니터링
    return processLargeDataSet();
}
```

#### 외부 API 호출 모니터링

```java
@AccessLogPerformanceMonitor(
    thresholdMs = 3000L,          // 외부 API는 3초 임계값
    logPerformance = true,        // 모든 호출 로깅
    slowQueryThresholdMs = 10000L // 10초 초과시 슬로우 쿼리
)
public ExternalApiResponse callExternalService() {
    // 외부 서비스 호출 성능 모니터링
    return externalServiceClient.getData();
}
```

## 조합 사용 패턴

### 1. 완전한 모니터링 (Full Monitoring)

```java
@AccessLogExceptionHandler(
    defaultType = ExceptionType.SAVE,
    retry = true,
    retryCount = 3
)
@AccessLogMethodLogger(
    level = LogLevel.INFO,
    logParameters = true,
    measureExecutionTime = true
)
@AccessLogPerformanceMonitor(
    thresholdMs = 1000L,
    measureMemory = true,
    collectMetrics = true
)
public void fullMonitoringExample(AccessLogData data) {
    // 예외 처리 + 로깅 + 성능 모니터링 모두 적용
    processAndSaveData(data);
}
```

### 2. 프로덕션 환경 최적화

```java
@AccessLogExceptionHandler(
    defaultType = ExceptionType.QUERY,
    logLevel = LogLevel.WARN
)
@AccessLogMethodLogger(
    level = LogLevel.INFO,
    logParameters = false,      // 프로덕션에서는 파라미터 숨김
    logReturnValue = false,     // 반환값도 숨김
    measureExecutionTime = true
)
@AccessLogPerformanceMonitor(
    thresholdMs = 500L,
    measureMemory = false,      // 프로덕션에서는 메모리 측정 비활성화
    logPerformance = false      // 임계값 초과시에만 로깅
)
public List<AccessLog> productionQuery() {
    // 프로덕션 환경에 최적화된 설정
    return queryAccessLogs();
}
```

### 3. 개발 환경 디버깅

```java
@AccessLogExceptionHandler(
    defaultType = ExceptionType.GENERAL,
    logLevel = LogLevel.DEBUG,
    retry = false
)
@AccessLogMethodLogger(
    level = LogLevel.DEBUG,
    logParameters = true,       // 개발에서는 모든 파라미터 로깅
    logReturnValue = true,      // 반환값도 로깅
    measureExecutionTime = true
)
@AccessLogPerformanceMonitor(
    thresholdMs = 100L,         // 개발에서는 낮은 임계값
    measureMemory = true,       // 메모리 누수 감지
    collectMetrics = true,
    enablePerformanceGrading = true
)
public DebugResult developmentDebugging(ComplexInput input) {
    // 개발 환경에서 상세한 디버깅 정보 수집
    return complexProcessing(input);
}
```

## 도입 효과 및 장점

### 1. 코드 품질 향상

**기존 방식**
```java
public void saveAccessLog(AccessLogData data) {
    long startTime = System.currentTimeMillis();
    log.info("접근 로그 저장 시작: {}", data.getUserId());
    
    try {
        // 실제 비즈니스 로직
        accessLogRepository.save(data);
        
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("접근 로그 저장 완료: {}ms", executionTime);
        
    } catch (DataAccessException e) {
        log.error("접근 로그 저장 실패: {}", e.getMessage());
        throw new AccessLogSaveException("저장 중 오류 발생", e);
    } catch (Exception e) {
        log.error("예상치 못한 오류: {}", e.getMessage());
        throw new AccessLogException("시스템 오류", e);
    }
}
```

**AOP 적용 후**
```java
@AccessLogExceptionHandler(defaultType = ExceptionType.SAVE)
@AccessLogMethodLogger(logParameters = true)
@AccessLogPerformanceMonitor(thresholdMs = 500L)
public void saveAccessLog(AccessLogData data) {
    // 순수한 비즈니스 로직만 남음
    accessLogRepository.save(data);
}
```

### 2. 일관성 확보

- **예외 처리**: 모든 AccessLog 관련 메서드에서 일관된 예외 변환
- **로깅 형식**: 표준화된 로그 포맷으로 분석 도구 연동 용이
- **성능 측정**: 동일한 기준으로 모든 메서드의 성능 비교 가능

### 3. 유지보수성 향상

- **관심사 분리**: 비즈니스 로직과 횡단 관심사 완전 분리
- **설정 변경**: 어노테이션 파라미터만 수정하면 동작 변경 가능
- **코드 재사용**: 공통 로직의 중복 제거

### 4. 모니터링 강화

- **실시간 통계**: 메서드별 호출 횟수, 평균 실행 시간 자동 수집
- **성능 이상 감지**: 임계값 기반 자동 알림
- **메모리 추적**: 메모리 누수 및 사용량 증가 패턴 감지

## 성능 고려사항

### 1. 메모리 측정 비용

```java
// 성능에 민감한 메서드는 메모리 측정 비활성화
@AccessLogPerformanceMonitor(
    measureMemory = false,  // GC 호출 비용 절약
    collectMetrics = true
)
public void performanceCriticalMethod() {
    // 빈번하게 호출되는 메서드
}
```

### 2. 로깅 레벨 조절

```java
// 프로덕션에서는 필요한 정보만 로깅
@AccessLogMethodLogger(
    level = LogLevel.WARN,      // 경고 이상만 로깅
    logParameters = false,      // 파라미터 로깅 비활성화
    measureExecutionTime = true // 성능 측정은 유지
)
public void highVolumeMethod() {
    // 대용량 처리 메서드
}
```

### 3. 재시도 정책

```java
// 네트워크 의존적인 작업에만 재시도 적용
@AccessLogExceptionHandler(
    retry = true,
    retryCount = 2,  // 재시도 횟수 최소화
    defaultType = ExceptionType.KAFKA
)
public void networkDependentOperation() {
    // 외부 시스템 연동
}
```

## 모니터링 및 운영

### 1. 통계 정보 조회

```java
@RestController
public class MonitoringController {
    
    @Autowired
    private AccessLogExceptionAspect exceptionAspect;
    
    @Autowired
    private AccessLogPerformanceAspect performanceAspect;
    
    @GetMapping("/monitoring/exception-stats")
    public ExceptionStats getExceptionStats() {
        return exceptionAspect.getExceptionStats();
    }
    
    @GetMapping("/monitoring/performance-stats")
    public PerformanceStats getPerformanceStats() {
        return performanceAspect.getPerformanceStats();
    }
    
    @GetMapping("/monitoring/method-stats/{methodName}")
    public MethodPerformanceStats getMethodStats(@PathVariable String methodName) {
        return performanceAspect.getMethodStats(methodName);
    }
}
```

### 2. 알림 설정

```yaml
# application.yml
logging:
  level:
    com.fream.back.domain.accessLog.aop: INFO
    
monitoring:
  performance:
    alert-threshold: 5000ms
    slow-query-threshold: 10000ms
  exception:
    alert-retry-rate: 10%
```

### 3. 대시보드 연동

```java
// Micrometer 메트릭 연동 예시
@Component
public class AccessLogMetrics {
    
    private final MeterRegistry meterRegistry;
    private final AccessLogPerformanceAspect performanceAspect;
    
    @Scheduled(fixedRate = 60000) // 1분마다 수집
    public void collectMetrics() {
        PerformanceStats stats = performanceAspect.getPerformanceStats();
        
        Gauge.builder("accesslog.performance.total_checks")
             .register(meterRegistry, stats, PerformanceStats::getTotalChecks);
             
        Gauge.builder("accesslog.performance.threshold_exceeded_rate")
             .register(meterRegistry, stats, PerformanceStats::getThresholdExceededRate);
    }
}
```

## 마이그레이션 가이드

### 1. 단계적 적용

```java
// 1단계: 예외 처리만 적용
@AccessLogExceptionHandler
public void step1_ExceptionOnly() {
    // 기존 코드에서 try-catch 제거
}

// 2단계: 로깅 추가
@AccessLogExceptionHandler
@AccessLogMethodLogger
public void step2_WithLogging() {
    // 기존 로깅 코드 제거
}

// 3단계: 성능 모니터링 추가
@AccessLogExceptionHandler
@AccessLogMethodLogger
@AccessLogPerformanceMonitor
public void step3_FullMonitoring() {
    // 완전한 AOP 기반 모니터링
}
```

### 2. 기존 코드 리팩토링

```java
// 리팩토링 전
public void oldStyleMethod() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    log.info("메서드 시작");
    
    try {
        // 비즈니스 로직
        doSomething();
        
        stopWatch.stop();
        log.info("메서드 완료: {}ms", stopWatch.getTotalTimeMillis());
        
    } catch (Exception e) {
        log.error("오류 발생", e);
        throw new AccessLogException("처리 실패", e);
    }
}

// 리팩토링 후
@AccessLogExceptionHandler
@AccessLogMethodLogger
@AccessLogPerformanceMonitor
public void newStyleMethod() {
    // 순수 비즈니스 로직만
    doSomething();
}
```

## 베스트 프랙티스

### 1. 어노테이션 조합 원칙

- **예외 처리**: 모든 public 메서드에 기본 적용
- **로깅**: 중요한 비즈니스 로직에 적용
- **성능 모니터링**: 성능이 중요한 메서드에 적용

### 2. 환경별 설정 분리

```java
@Profile("development")
@AccessLogMethodLogger(level = LogLevel.DEBUG, logParameters = true)

@Profile("production")
@AccessLogMethodLogger(level = LogLevel.INFO, logParameters = false)
```

### 3. 보안 고려사항

```java
// 개인정보 처리 메서드
@AccessLogMethodLogger(
    logParameters = false,  // 개인정보 로깅 방지
    logReturnValue = false  // 민감한 반환값 보호
)
public PersonalData getPersonalInfo(String personalId) {
    return personalDataService.getData(personalId);
}
```

이 AOP 기반 시스템을 통해 AccessLog 도메인의 코드 품질, 모니터링 수준, 유지보수성을 대폭 향상시킬 수 있으며, 개발자는 비즈니스 로직에만 집중할 수 있게 됩니다.