# Event Domain AOP Implementation Documentation

## 📋 목차

1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [AOP 구현체 상세](#aop-구현체-상세)
4. [사용 가이드](#사용-가이드)
5. [설정 및 커스터마이징](#설정-및-커스터마이징)
6. [모니터링 및 메트릭](#모니터링-및-메트릭)
7. [성능 고려사항](#성능-고려사항)
8. [트러블슈팅](#트러블슈팅)
9. [ChatQuestion AOP와의 비교](#chatquestion-aop와의-비교)

## 개요

### 구현 배경

Event 도메인은 FREAM 프로젝트의 핵심 기능 중 하나로, 브랜드별 이벤트 관리, 이미지 처리, 상태 관리 등 복잡한 비즈니스 로직을 포함합니다. AOP를 통해 다음과 같은 횡단 관심사를 효과적으로 관리합니다:

- **이미지 파일 처리**: 대용량 이미지 업로드, 리사이징, 최적화
- **이벤트 상태 관리**: UPCOMING → ACTIVE → ENDED 자동 전환
- **브랜드별 제약사항**: 동시 활성 이벤트 수 제한, 중복 방지
- **성능 최적화**: 캐싱, 비동기 처리, 리소스 모니터링
- **비즈니스 규칙 강제**: 날짜 검증, 권한 확인, 생성 빈도 제한

### 구현 목표

1. **파일 처리 안정성**: 이미지 업로드/삭제 시 트랜잭션 보장
2. **비즈니스 규칙 강제**: 브랜드당 이벤트 제한, 날짜 검증
3. **성능 최적화**: 조회 캐싱, 이미지 최적화
4. **실시간 모니터링**: 이벤트 라이프사이클, 사용자 활동 추적
5. **유지보수성**: 선언적 프로그래밍으로 관심사 분리

## 아키텍처

### 전체 구조

```
┌─────────────────────────────────────────────────────────────┐
│                      Event Domain                           │
├─────────────────────────────────────────────────────────────┤
│  Controllers         │    Services       │   Repositories   │
│  - EventQueryController │  - EventQueryService │ - EventRepository    │
│  - EventCommandController│  - EventCommandService│ - SimpleImageRepository│
├─────────────────────────────────────────────────────────────┤
│                       AOP Layer                             │
├─────────────────────────────────────────────────────────────┤
│ Validation │FileManagement│ Monitoring │ Caching │BusinessRule│
│            │              │            │         │            │
│ • 날짜검증  │ • 이미지처리   │ • 성능추적  │ • 조회캐싱│ • 중복방지  │
│ • 파일검증  │ • 리사이징    │ • 라이프사이클│ • TTL관리 │ • 브랜드제한 │
│ • 권한확인  │ • 백업/롤백   │ • 사용자활동 │ • 무효화  │ • 상태전환  │
└─────────────────────────────────────────────────────────────┘
```

### AOP 실행 순서

```java
@Order(1) EventValidationAspect    // 유효성 검증 (가장 먼저)
@Order(2) EventFileManagementAspect // 파일 전처리
@Order(3) EventMonitoringAspect    // 모니터링 시작
@Order(4) EventCachingAspect       // 캐시 조회
@Order(5) EventBusinessRuleAspect  // 비즈니스 규칙 적용
```

### 데이터 흐름

```
Request → [Validation] → [File Processing] → [Monitoring Start] 
         → [Cache Check] → [Business Rules] → Service Method
         → [Cache Update] → [Monitoring End] → Response
```

## AOP 구현체 상세

### 1. EventValidationAspect

**목적**: 입력 데이터 유효성 검증 및 권한 확인

**주요 기능**:
- 날짜 범위 검증 (최소 1시간, 최대 365일)
- 파일 형식 및 크기 검증
- 브랜드 존재 여부 확인
- 관리자 권한 검증
- 상태 전환 유효성 검사

**어노테이션 속성**:
```java
@EventValidation(
    validations = ValidationType[],      // 검증 타입들
    minDurationHours = 1,               // 최소 이벤트 기간
    maxDurationDays = 365,              // 최대 이벤트 기간
    allowPastDates = false,             // 과거 날짜 허용 여부
    maxFutureDays = 365,                // 미래 날짜 제한
    checkBrandStatus = true,            // 브랜드 상태 확인
    checkTitleDuplicate = false,        // 제목 중복 확인
    maxTotalFileSize = 52428800,        // 최대 파일 크기 (50MB)
    validateStatusTransition = true,     // 상태 전환 검증
    requireAdminPermission = true,       // 관리자 권한 필요
    failureAction = THROW_EXCEPTION     // 실패 시 처리 방식
)
```

**사용 예시**:
```java
@EventValidation(
    validations = {ValidationType.DATE_RANGE, ValidationType.FILE_VALIDATION},
    minDurationHours = 1,
    maxDurationDays = 365,
    maxTotalFileSize = 52428800, // 50MB
    requireAdminPermission = true
)
public Long createEvent(CreateEventRequest request, MultipartFile thumbnail) {
    // 메서드 실행 전 자동으로 유효성 검증
}
```

### 2. EventFileManagementAspect

**목적**: 이미지 파일 처리 및 최적화

**주요 기능**:
- 이미지 자동 리사이징 (썸네일: 800px, 일반: 1200px)
- 파일 형식 검증 (jpg, png, gif, webp)
- 업로드 실패 시 롤백
- 백업 생성 옵션
- WebP 변환 지원
- 바이러스 스캔 (옵션)

**어노테이션 속성**:
```java
@EventFileManagement(
    operations = FileOperation[],        // 수행할 작업들
    allowedExtensions = String[],        // 허용 확장자
    maxFileSize = 10485760,             // 최대 파일 크기 (10MB)
    maxFileCount = 10,                  // 최대 파일 개수
    enableImageResize = true,           // 리사이징 활성화
    thumbnailMaxSize = 800,             // 썸네일 최대 크기
    simpleImageMaxSize = 1200,          // 일반 이미지 최대 크기
    convertToWebP = false,              // WebP 변환 여부
    imageQuality = 85,                  // 이미지 품질 (1-100)
    rollbackOnFileFailure = true,       // 실패 시 롤백
    duplicateHandling = RENAME,         // 중복 파일 처리
    createBackup = false,               // 백업 생성
    asyncProcessing = false             // 비동기 처리
)
```

**이미지 처리 플로우**:
```
원본 이미지 → 유효성 검사 → 리사이징 → 품질 조정 → 저장 → 백업(선택)
```

### 3. EventMonitoringAspect

**목적**: 실시간 성능 모니터링 및 사용자 활동 추적

**주요 기능**:
- 메서드 실행 시간 측정
- 메모리/CPU 사용량 추적
- 이벤트 라이프사이클 모니터링
- 브랜드별 통계 수집
- 사용자 활동 로깅
- 임계값 초과 시 알림

**어노테이션 속성**:
```java
@EventMonitoring(
    metrics = MetricType[],              // 수집할 메트릭 타입
    trackLifecycle = true,              // 라이프사이클 추적
    monitorFileOperations = true,       // 파일 작업 모니터링
    trackUserActivity = true,           // 사용자 활동 추적
    alertOnStatusChange = true,         // 상태 변경 알림
    alertOnLargeFileUpload = true,      // 대용량 파일 알림
    monitorCreationFrequency = true,    // 생성 빈도 모니터링
    collectBrandStatistics = true,      // 브랜드 통계 수집
    monitorCachePerformance = true,     // 캐시 성능 모니터링
    collectionIntervalSeconds = 60,     // 수집 간격
    fileUploadThresholdMB = 5,          // 파일 크기 임계값
    eventCreationThresholdPerHour = 10, // 시간당 생성 임계값
    cacheHitRateThreshold = 0.8        // 캐시 히트율 임계값
)
```

**수집 메트릭 예시**:
```
=== EVENT MONITORING METRICS REPORT ===
Method: EventService.createEvent
  - Total Calls: 1,250
  - Success Rate: 98.5%
  - Avg Execution Time: 234ms
  - Min/Max Time: 120ms / 890ms
  - Memory Usage: 45MB avg
  
Brand Statistics:
  - Brand #1: 45 events (5 active)
  - Brand #2: 32 events (3 active)
  
Cache Performance:
  - Hit Rate: 85.3%
  - Total Hits: 10,234
  - Total Misses: 1,755
```

### 4. EventCachingAspect

**목적**: 조회 성능 최적화

**주요 기능**:
- 이벤트 목록/상세 캐싱
- TTL 기반 자동 만료 (기본 5분)
- 상태 변경 시 자동 무효화
- 브랜드/사용자별 캐시 분리
- 캐시 워밍업 지원
- 히트/미스 메트릭 수집

**어노테이션 속성**:
```java
@EventCaching(
    keyStrategy = CacheKeyStrategy.DEFAULT,  // 키 생성 전략
    ttlSeconds = 300,                       // TTL (5분)
    enabled = true,                         // 캐싱 활성화
    invalidateOnStatusChange = true,        // 상태 변경 시 무효화
    separateByBrand = false,                // 브랜드별 분리
    separateByUserRole = false,             // 역할별 분리
    warmUp = false,                         // 워밍업 여부
    collectMetrics = true,                  // 메트릭 수집
    conditionalCaching = false,             // 조건부 캐싱
    condition = ""                          // 캐싱 조건
)
```

**캐시 키 전략**:
- `DEFAULT`: 메서드명 + 파라미터
- `EVENT_ID`: 이벤트 ID 기반
- `BRAND_ID`: 브랜드 ID 기반
- `USER_SPECIFIC`: 사용자별
- `STATUS_BASED`: 상태별
- `TIME_BASED`: 시간 세그먼트
- `COMPOSITE`: 복합 키

### 5. EventBusinessRuleAspect

**목적**: 비즈니스 규칙 강제 적용

**주요 기능**:
- 브랜드당 활성 이벤트 제한
- 이벤트 기간 중복 방지
- 생성 쿨다운 적용
- 상태별 수정/삭제 권한 제어
- 종료 이벤트 자동 아카이빙
- 브랜드 비활성화 처리

**어노테이션 속성**:
```java
@EventBusinessRule(
    rules = BusinessRule[],                  // 적용할 규칙들
    maxActiveEventsPerBrand = 5,            // 브랜드당 최대 활성 이벤트
    allowOverlappingEvents = true,          // 중복 이벤트 허용
    creationCooldownMinutes = 5,            // 생성 쿨다운
    editableHoursBeforeStart = 24,          // 수정 가능 시간
    allowActiveEventModification = false,    // 진행중 수정 허용
    allowEndedEventModification = false,     // 종료 후 수정 허용
    deletableAfterEndDays = 30,             // 삭제 가능 기간
    enableAutoStatusChange = true,          // 자동 상태 변경
    autoArchiveOnEnd = false,               // 종료 시 아카이빙
    brandDeactivationAction = MARK_INACTIVE, // 브랜드 비활성화 처리
    violationAction = THROW_EXCEPTION       // 위반 시 처리
)
```

## 사용 가이드

### 기본 사용법

#### 1. 이벤트 생성 - 전체 AOP 적용

```java
@RestController
@RequestMapping("/events")
public class EventCommandController {

    @PostMapping
    @EventValidation(
        validations = {ValidationType.DATE_RANGE, ValidationType.BRAND_VALIDATION},
        requireAdminPermission = true
    )
    @EventFileManagement(
        operations = {FileOperation.VALIDATION, FileOperation.RESIZE},
        enableImageResize = true,
        imageQuality = 85
    )
    @EventMonitoring(
        trackLifecycle = true,
        collectBrandStatistics = true,
        alertOnLargeFileUpload = true
    )
    @EventBusinessRule(
        rules = {BusinessRule.EVENT_OVERLAP_CHECK, BusinessRule.BRAND_LIMIT_CHECK},
        maxActiveEventsPerBrand = 5
    )
    public ResponseEntity<Long> createEvent(
        @Valid @ModelAttribute CreateEventRequest request,
        @RequestParam MultipartFile thumbnailFile,
        @RequestParam List<MultipartFile> simpleImageFiles
    ) {
        Long eventId = eventCommandService.createEvent(
            request, thumbnailFile, simpleImageFiles
        );
        return ResponseEntity.ok(eventId);
    }
}
```

#### 2. 이벤트 조회 - 캐싱 적용

```java
@RestController
@RequestMapping("/events")
public class EventQueryController {

    @GetMapping("/{eventId}")
    @EventCaching(
        keyStrategy = CacheKeyStrategy.EVENT_ID,
        ttlSeconds = 600,  // 10분
        separateByUserRole = true
    )
    @EventMonitoring(
        metrics = {MetricType.CACHE_PERFORMANCE}
    )
    public ResponseEntity<EventDetailDto> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventQueryService.findEventById(eventId));
    }

    @GetMapping("/active")
    @EventCaching(
        keyStrategy = CacheKeyStrategy.STATUS_BASED,
        ttlSeconds = 300,
        invalidateOnStatusChange = true
    )
    public ResponseEntity<List<EventListDto>> getActiveEvents() {
        return ResponseEntity.ok(eventQueryService.findActiveEvents());
    }
}
```

#### 3. 이벤트 수정 - 권한 및 규칙 검증

```java
@PatchMapping("/{eventId}")
@EventValidation(
    validations = {ValidationType.DATE_RANGE, ValidationType.PERMISSION_CHECK},
    requireAdminPermission = true
)
@EventBusinessRule(
    rules = {BusinessRule.EDIT_PERMISSION, BusinessRule.STATUS_TRANSITION},
    editableHoursBeforeStart = 24,
    allowActiveEventModification = false
)
@EventMonitoring(
    trackLifecycle = true,
    alertOnStatusChange = true
)
public ResponseEntity<Long> updateEvent(
    @PathVariable Long eventId,
    @Valid @ModelAttribute UpdateEventRequest request,
    @RequestParam(required = false) MultipartFile thumbnailFile,
    @RequestParam(required = false) List<MultipartFile> simpleImageFiles
) {
    Long updatedId = eventCommandService.updateEvent(
        eventId, request, thumbnailFile, simpleImageFiles
    );
    return ResponseEntity.ok(updatedId);
}
```

#### 4. 서비스 레이어 AOP 적용

```java
@Service
@Transactional
public class EventCommandService {

    @EventBusinessRule(
        rules = {BusinessRule.CREATION_FREQUENCY},
        creationCooldownMinutes = 5
    )
    @EventMonitoring(
        trackLifecycle = true,
        monitorFileOperations = true
    )
    public Long createEvent(CreateEventRequest request, 
                           MultipartFile thumbnail,
                           List<MultipartFile> images) {
        // 비즈니스 로직
        return event.getId();
    }

    @EventCaching(
        keyStrategy = CacheKeyStrategy.EVENT_ID,
        ttlSeconds = 0  // 캐시 무효화만 수행
    )
    public Long updateEventStatus(Long eventId, EventStatus newStatus) {
        // 상태 변경 로직
        // 캐시 자동 무효화
        return eventId;
    }
}
```

### 환경별 설정

#### 개발 환경
```java
// 상세한 모니터링, 짧은 캐시 TTL
@EventMonitoring(
    metrics = {MetricType.EXECUTION_TIME, MetricType.SYSTEM_RESOURCES},
    collectionIntervalSeconds = 30
)
@EventCaching(ttlSeconds = 60)
```

#### 운영 환경
```java
// 필수 모니터링만, 긴 캐시 TTL
@EventMonitoring(
    metrics = {MetricType.EXECUTION_TIME},
    collectionIntervalSeconds = 300
)
@EventCaching(ttlSeconds = 600)
```

## 설정 및 커스터마이징

### application.yml 설정

```yaml
event:
  aop:
    # 유효성 검증 설정
    validation:
      enabled: true
      max-file-size: 52428800  # 50MB
      allowed-extensions: jpg,jpeg,png,gif,webp
      min-duration-hours: 1
      max-duration-days: 365
    
    # 파일 관리 설정
    file-management:
      enabled: true
      image-resize: true
      thumbnail-size: 800
      simple-image-size: 1200
      image-quality: 85
      backup-enabled: false
      async-processing: false
    
    # 모니터링 설정
    monitoring:
      enabled: true
      collection-interval: 60
      alert-thresholds:
        file-upload-mb: 5
        creation-per-hour: 10
        cache-hit-rate: 0.8
      metrics-export:
        enabled: true
        endpoint: http://metrics-server/api/events
    
    # 캐싱 설정
    caching:
      enabled: true
      default-ttl: 300
      max-entries: 1000
      warm-up-enabled: false
      cache-names:
        - eventCache
        - eventCache_brand
        - eventCache_admin
    
    # 비즈니스 규칙 설정
    business-rules:
      enabled: true
      max-active-events-per-brand: 5
      creation-cooldown-minutes: 5
      editable-hours-before-start: 24
      deletable-after-end-days: 30
      auto-archive: false
```

### 커스텀 설정 클래스

```java
@Configuration
@EnableAspectJAutoProxy
public class EventAopConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "event.aop.monitoring",
        name = "enabled",
        havingValue = "true"
    )
    public EventMonitoringAspect eventMonitoringAspect() {
        return new EventMonitoringAspect();
    }

    @Bean
    public CacheManager eventCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

## 모니터링 및 메트릭

### 주요 모니터링 지표

#### 1. 성능 메트릭
```
- 평균 응답 시간: 234ms
- P95 응답 시간: 890ms
- P99 응답 시간: 1,234ms
- 처리량: 450 req/min
```

#### 2. 캐시 메트릭
```
- 캐시 히트율: 85.3%
- 캐시 미스: 1,755
- 캐시 크기: 342 entries
- 평균 TTL: 4분 32초
```

#### 3. 파일 처리 메트릭
```
- 총 업로드: 2,345 files
- 총 용량: 3.4GB
- 평균 처리 시간: 456ms
- 리사이징 완료: 2,100 files
```

#### 4. 비즈니스 메트릭
```
- 일일 이벤트 생성: 45건
- 활성 이벤트: 127개
- 브랜드별 평균: 3.2개
- 규칙 위반: 12건
```

### 로그 포맷

```
2024-01-15 10:30:45.123 INFO  [EventMonitoringAspect] EVENT_MONITORING_SUCCESS 
    - RequestId: abc12345
    - Method: EventService.createEvent
    - ExecutionTime: 234ms
    - User: admin@fream.com
    - Result: SUCCESS

2024-01-15 10:30:45.456 WARN  [EventBusinessRuleAspect] BRAND_EVENT_LIMIT_EXCEEDED
    - BrandId: 123
    - ActiveEvents: 5
    - Limit: 5
    - Action: REJECTED
```

### 알림 설정

```java
@Component
public class EventAlertService {

    @EventHandler
    public void handleLargeFileUpload(LargeFileUploadEvent event) {
        // 슬랙 알림 발송
        slackClient.sendAlert(String.format(
            "대용량 파일 업로드: %s (%dMB)",
            event.getFileName(),
            event.getFileSize() / 1024 / 1024
        ));
    }

    @EventHandler
    public void handleCacheLowHitRate(CacheLowHitRateEvent event) {
        // 이메일 알림 발송
        emailService.sendAlert(
            "캐시 히트율 저하",
            String.format("현재 히트율: %.1f%%", event.getHitRate() * 100)
        );
    }
}
```

## 성능 고려사항

### 1. AOP 오버헤드 최소화

**문제점**: 모든 메서드에 AOP 적용 시 성능 저하

**해결방안**:
- 필요한 메서드에만 선택적 적용
- 조건부 실행 (`@ConditionalOnProperty`)
- 비동기 처리 활용

```java
@EventMonitoring(
    enabled = "${event.monitoring.enabled:true}",
    asyncProcessing = true
)
```

### 2. 캐시 최적화

**문제점**: 캐시 무효화 전략 부재로 인한 데이터 불일치

**해결방안**:
- 상태 변경 시 자동 무효화
- TTL 기반 만료
- 선택적 캐시 워밍업

```java
// 캐시 무효화 전략
@CacheEvict(value = "eventCache", key = "#eventId")
public void updateEventStatus(Long eventId, EventStatus status) {
    // 상태 변경 시 캐시 자동 무효화
}
```

### 3. 파일 처리 최적화

**문제점**: 대용량 이미지 처리 시 메모리 부족

**해결방안**:
- 스트리밍 처리
- 청크 단위 업로드
- 비동기 리사이징

```java
@EventFileManagement(
    asyncProcessing = true,
    maxFileSize = 10485760,  // 10MB 제한
    enableImageResize = true
)
```

### 4. 메모리 관리

**문제점**: 메트릭 데이터 누적으로 인한 메모리 누수

**해결방안**:
- 주기적 데이터 정리
- 메트릭 익스포트 후 삭제
- 순환 버퍼 사용

```java
// 1시간마다 오래된 메트릭 정리
@Scheduled(fixedDelay = 3600000)
public void cleanupOldMetrics() {
    metricsMap.entrySet().removeIf(entry -> 
        entry.getValue().getLastAccess().isBefore(
            LocalDateTime.now().minusHours(1)
        )
    );
}
```

## 트러블슈팅

### 일반적인 문제 해결

#### 1. 캐시 불일치 문제
**증상**: 업데이트 후에도 이전 데이터 표시

**해결**:
```java
// 명시적 캐시 무효화
@CacheEvict(value = "eventCache", allEntries = true)
public void clearEventCache() {
    log.info("All event caches cleared");
}
```

#### 2. 파일 업로드 실패
**증상**: 대용량 파일 업로드 시 타임아웃

**해결**:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB
server:
  tomcat:
    connection-timeout: 60000
```

#### 3. AOP 미작동
**증상**: 어노테이션 추가했지만 AOP 동작 안함

**해결**:
```java
// 1. @EnableAspectJAutoProxy 확인
@Configuration
@EnableAspectJAutoProxy
public class AppConfig {}

// 2. 프록시 모드 확인 (인터페이스 vs CGLIB)
@EnableAspectJAutoProxy(proxyTargetClass = true)

// 3. 컴포넌트 스캔 확인
@ComponentScan(basePackages = "com.fream.back.domain.event.aop")
```

#### 4. 순환 참조 문제
**증상**: AOP 적용 시 순환 참조 에러

**해결**:
```java
// Lazy 로딩 사용
@Lazy
@Autowired
private EventService eventService;

// 또는 Setter 주입
private EventService eventService;

@Autowired
public void setEventService(@Lazy EventService eventService) {
    this.eventService = eventService;
}
```

### 디버깅 팁

#### 1. AOP 실행 순서 확인
```java
// 로그 레벨 설정
logging:
  level:
    com.fream.back.domain.event.aop: DEBUG
    org.springframework.aop: TRACE
```

#### 2. 프록시 객체 확인
```java
@Autowired
private ApplicationContext context;

public void checkProxy() {
    EventService service = context.getBean(EventService.class);
    System.out.println("Is Proxy: " + AopUtils.isAopProxy(service));
    System.out.println("Is CGLIB: " + AopUtils.isCglibProxy(service));
}
```

#### 3. 메트릭 덤프
```java
// JMX를 통한 메트릭 확인
@ManagedResource
@Component
public class EventMetricsExporter {
    
    @ManagedAttribute
    public Map<String, Object> getMetrics() {
        return collectAllMetrics();
    }
}
```

## ChatQuestion AOP와의 비교

### 공통점

1. **5개 AOP 클래스 구조**
    - 예외 처리 / 검증
    - 성능 모니터링
    - 로깅 / 모니터링
    - 보안 / 비즈니스 규칙
    - 감사 / 캐싱

2. **선언적 프로그래밍**
    - 어노테이션 기반 적용
    - 관심사 분리

3. **메트릭 수집**
    - 실행 시간, 성공률
    - 사용자 활동 추적

### 차이점

| 구분 | ChatQuestion AOP | Event AOP |
|------|-----------------|-----------|
| **핵심 관심사** | GPT API 관리 | 이미지 파일 처리 |
| **비용 관리** | 토큰 사용량 추적 | 파일 스토리지 관리 |
| **성능 최적화** | API 응답 캐싱 | 이미지 리사이징/최적화 |
| **보안 초점** | API 키 보호, 속도 제한 | 파일 검증, 권한 관리 |
| **상태 관리** | 세션 추적 | 이벤트 라이프사이클 |
| **특수 기능** | 서킷 브레이커, 폴백 | 자동 아카이빙, 브랜드 제한 |

### Event AOP 특화 기능

1. **파일 처리 최적화**
    - 이미지 자동 리사이징
    - WebP 변환
    - 백업 및 롤백

2. **이벤트 상태 관리**
    - UPCOMING → ACTIVE → ENDED 자동 전환
    - 상태별 권한 제어
    - 시간 기반 검증

3. **브랜드 중심 제약**
    - 브랜드당 활성 이벤트 제한
    - 중복 이벤트 방지
    - 브랜드 비활성화 처리

4. **시각적 자원 관리**
    - 썸네일/심플이미지 구분 처리
    - 용량 제한 및 최적화
    - CDN 연동 준비

## 확장 가능성

### 향후 개선 사항

1. **AI 기반 이미지 분석**
    - 부적절한 콘텐츠 자동 감지
    - 이미지 품질 평가
    - 자동 태깅

2. **실시간 알림 시스템**
    - WebSocket 기반 상태 변경 알림
    - 사용자별 맞춤 알림
    - 푸시 알림 연동

3. **고급 캐싱 전략**
    - Redis 분산 캐시
    - 캐시 예열(Warming)
    - 지능형 무효화

4. **메트릭 시각화**
    - Grafana 대시보드
    - 실시간 모니터링
    - 이상 감지 알고리즘

## 마무리

Event 도메인 AOP는 이벤트 관리의 복잡한 요구사항을 체계적으로 해결하는 강력한 도구입니다. 선언적 프로그래밍을 통해 비즈니스 로직과 인프라 관심사를 명확히 분리하고, 유지보수성과 확장성을 크게 향상시켰습니다.

특히 이미지 처리, 상태 관리, 브랜드별 제약사항 등 Event 도메인 특유의 요구사항을 효과적으로 처리하면서도, 성능과 안정성을 보장하는 균형잡힌 설계를 구현했습니다.

### 문의 및 지원

- **프로젝트 저장소**: `github.com/fream/event-aop`
- **이슈 트래커**: `github.com/fream/event-aop/issues`
- **문서**: `docs.fream.com/event-aop`

---

*Last Updated: 2024-01-15*
*Version: 1.0.0*