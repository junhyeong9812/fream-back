package com.fream.back.domain.event.aop;

import com.fream.back.domain.event.aop.annotation.EventMonitoring;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event 도메인 모니터링 AOP
 * 이벤트 생성/수정/삭제, 상태 변경, 파일 작업 등의 모니터링
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(3)
public class EventMonitoringAspect {

    // 메트릭 수집 맵
    private final ConcurrentHashMap<String, MethodMetrics> methodMetricsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BrandEventMetrics> brandMetricsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserActivityMetrics> userActivityMap = new ConcurrentHashMap<>();

    // 이벤트 라이프사이클 추적
    private final ConcurrentHashMap<Long, EventLifecycle> eventLifecycleMap = new ConcurrentHashMap<>();

    // 캐시 성능 메트릭
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // 스케줄러로 주기적 메트릭 리포트
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 메서드별 메트릭
     */
    private static class MethodMetrics {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successCalls = new AtomicLong(0);
        private final AtomicLong failureCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = Long.MIN_VALUE;
        private final AtomicLong totalMemoryUsed = new AtomicLong(0);
        private final AtomicLong totalCpuTime = new AtomicLong(0);
    }

    /**
     * 브랜드별 이벤트 메트릭
     */
    private static class BrandEventMetrics {
        private final AtomicInteger totalEvents = new AtomicInteger(0);
        private final AtomicInteger activeEvents = new AtomicInteger(0);
        private final AtomicInteger upcomingEvents = new AtomicInteger(0);
        private final AtomicInteger endedEvents = new AtomicInteger(0);
        private final AtomicLong totalFileSize = new AtomicLong(0);
        private LocalDateTime lastEventCreation;
        private final AtomicInteger creationFrequency = new AtomicInteger(0);
    }

    /**
     * 사용자 활동 메트릭
     */
    private static class UserActivityMetrics {
        private final AtomicInteger createCount = new AtomicInteger(0);
        private final AtomicInteger updateCount = new AtomicInteger(0);
        private final AtomicInteger deleteCount = new AtomicInteger(0);
        private final AtomicInteger viewCount = new AtomicInteger(0);
        private LocalDateTime lastActivity;
        private final List<String> recentActions = new CopyOnWriteArrayList<>();
    }

    /**
     * 이벤트 라이프사이클
     */
    private static class EventLifecycle {
        private final Long eventId;
        private final LocalDateTime createdAt;
        private EventStatus currentStatus;
        private final List<StatusChange> statusHistory = new CopyOnWriteArrayList<>();
        private final AtomicInteger updateCount = new AtomicInteger(0);
        private LocalDateTime lastModified;

        public EventLifecycle(Long eventId) {
            this.eventId = eventId;
            this.createdAt = LocalDateTime.now();
            this.lastModified = LocalDateTime.now();
        }
    }

    /**
     * 상태 변경 기록
     */
    private static class StatusChange {
        private final EventStatus fromStatus;
        private final EventStatus toStatus;
        private final LocalDateTime changedAt;
        private final String changedBy;

        public StatusChange(EventStatus from, EventStatus to, String user) {
            this.fromStatus = from;
            this.toStatus = to;
            this.changedAt = LocalDateTime.now();
            this.changedBy = user;
        }
    }

    @Around("@annotation(eventMonitoring)")
    public Object monitorEvent(ProceedingJoinPoint joinPoint, EventMonitoring eventMonitoring) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        Object[] args = joinPoint.getArgs();

        // 메트릭 초기화
        MethodMetrics metrics = methodMetricsMap.computeIfAbsent(fullMethodName, k -> new MethodMetrics());

        // 모니터링 시작
        long startTime = System.currentTimeMillis();
        long startMemory = 0;
        long startCpuTime = 0;

        if (shouldCollectMetric(EventMonitoring.MetricType.SYSTEM_RESOURCES, eventMonitoring.metrics())) {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            startMemory = memoryBean.getHeapMemoryUsage().getUsed();
            startCpuTime = threadBean.getCurrentThreadCpuTime();
        }

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String userEmail = extractUserEmail();

        log.debug("EVENT_MONITORING_START - RequestId: {}, Method: {}, User: {}",
                requestId, fullMethodName, userEmail);

        try {
            // 사용자 활동 추적
            if (eventMonitoring.trackUserActivity()) {
                trackUserActivity(userEmail, methodName, args);
            }

            // 이벤트 라이프사이클 추적
            if (eventMonitoring.trackLifecycle()) {
                trackEventLifecycle(methodName, args);
            }

            // 메서드 실행
            Object result = joinPoint.proceed();

            // 성공 메트릭 기록
            long executionTime = System.currentTimeMillis() - startTime;
            recordSuccessMetrics(metrics, executionTime, startMemory, startCpuTime);

            // 브랜드 통계 수집
            if (eventMonitoring.collectBrandStatistics()) {
                collectBrandStatistics(methodName, args, result);
            }

            // 파일 작업 모니터링
            if (eventMonitoring.monitorFileOperations()) {
                monitorFileOperations(args);
            }

            // 상태 변경 알림
            if (eventMonitoring.alertOnStatusChange()) {
                checkAndAlertStatusChange(methodName, args, result);
            }

            // 임계값 체크 및 알림
            checkThresholdsAndAlert(eventMonitoring, args, executionTime);

            log.info("EVENT_MONITORING_SUCCESS - RequestId: {}, Method: {}, ExecutionTime: {}ms",
                    requestId, fullMethodName, executionTime);

            return result;

        } catch (Exception e) {
            metrics.failureCalls.incrementAndGet();
            log.error("EVENT_MONITORING_ERROR - RequestId: {}, Method: {}, Error: {}",
                    requestId, fullMethodName, e.getMessage());
            throw e;

        } finally {
            // 주기적 메트릭 리포트 스케줄링
            scheduleMetricsReport(eventMonitoring.collectionIntervalSeconds());
        }
    }

    /**
     * 성공 메트릭 기록
     */
    private void recordSuccessMetrics(MethodMetrics metrics, long executionTime, long startMemory, long startCpuTime) {
        metrics.totalCalls.incrementAndGet();
        metrics.successCalls.incrementAndGet();
        metrics.totalExecutionTime.addAndGet(executionTime);

        // 최소/최대 실행 시간 업데이트
        metrics.minExecutionTime = Math.min(metrics.minExecutionTime, executionTime);
        metrics.maxExecutionTime = Math.max(metrics.maxExecutionTime, executionTime);

        // 시스템 리소스 메트릭
        if (startMemory > 0) {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long memoryUsed = memoryBean.getHeapMemoryUsage().getUsed() - startMemory;
            metrics.totalMemoryUsed.addAndGet(Math.max(0, memoryUsed));
        }

        if (startCpuTime > 0) {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long cpuTime = threadBean.getCurrentThreadCpuTime() - startCpuTime;
            metrics.totalCpuTime.addAndGet(cpuTime);
        }
    }

    /**
     * 사용자 활동 추적
     */
    private void trackUserActivity(String userEmail, String methodName, Object[] args) {
        if (userEmail == null) return;

        UserActivityMetrics userMetrics = userActivityMap.computeIfAbsent(userEmail, k -> new UserActivityMetrics());
        userMetrics.lastActivity = LocalDateTime.now();

        // 메서드명에 따른 활동 분류
        if (methodName.contains("create")) {
            userMetrics.createCount.incrementAndGet();
            userMetrics.recentActions.add("CREATE_EVENT");
        } else if (methodName.contains("update")) {
            userMetrics.updateCount.incrementAndGet();
            userMetrics.recentActions.add("UPDATE_EVENT");
        } else if (methodName.contains("delete")) {
            userMetrics.deleteCount.incrementAndGet();
            userMetrics.recentActions.add("DELETE_EVENT");
        } else if (methodName.contains("find") || methodName.contains("get")) {
            userMetrics.viewCount.incrementAndGet();
            userMetrics.recentActions.add("VIEW_EVENT");
        }

        // 최근 활동 10개만 유지
        if (userMetrics.recentActions.size() > 10) {
            userMetrics.recentActions.remove(0);
        }

        log.debug("USER_ACTIVITY - User: {}, Action: {}, TotalActions: {}",
                userEmail, methodName,
                userMetrics.createCount.get() + userMetrics.updateCount.get() +
                        userMetrics.deleteCount.get() + userMetrics.viewCount.get());
    }

    /**
     * 이벤트 라이프사이클 추적
     */
    private void trackEventLifecycle(String methodName, Object[] args) {
        Long eventId = extractEventId(args);
        if (eventId == null) return;

        EventLifecycle lifecycle = eventLifecycleMap.computeIfAbsent(eventId, EventLifecycle::new);

        if (methodName.contains("create")) {
            lifecycle.currentStatus = EventStatus.UPCOMING;
            log.info("EVENT_LIFECYCLE - Event created: {}", eventId);
        } else if (methodName.contains("update")) {
            lifecycle.updateCount.incrementAndGet();
            lifecycle.lastModified = LocalDateTime.now();
            log.info("EVENT_LIFECYCLE - Event updated: {}, UpdateCount: {}",
                    eventId, lifecycle.updateCount.get());
        } else if (methodName.contains("Status")) {
            // 상태 변경 추적
            EventStatus newStatus = extractEventStatus(args);
            if (newStatus != null && newStatus != lifecycle.currentStatus) {
                String user = extractUserEmail();
                lifecycle.statusHistory.add(new StatusChange(lifecycle.currentStatus, newStatus, user));
                lifecycle.currentStatus = newStatus;
                log.info("EVENT_LIFECYCLE - Status changed: {} -> {} for Event: {}",
                        lifecycle.currentStatus, newStatus, eventId);
            }
        }
    }

    /**
     * 브랜드 통계 수집
     */
    private void collectBrandStatistics(String methodName, Object[] args, Object result) {
        Long brandId = extractBrandId(args);
        if (brandId == null) return;

        BrandEventMetrics brandMetrics = brandMetricsMap.computeIfAbsent(brandId, k -> new BrandEventMetrics());

        if (methodName.contains("create")) {
            brandMetrics.totalEvents.incrementAndGet();
            brandMetrics.lastEventCreation = LocalDateTime.now();
            brandMetrics.creationFrequency.incrementAndGet();

            // 생성 빈도 리셋 (1시간 기준)
            scheduler.schedule(() -> brandMetrics.creationFrequency.set(0), 1, TimeUnit.HOURS);
        }

        // 파일 크기 통계
        long fileSize = calculateTotalFileSize(args);
        if (fileSize > 0) {
            brandMetrics.totalFileSize.addAndGet(fileSize);
        }

        log.debug("BRAND_STATISTICS - BrandId: {}, TotalEvents: {}, TotalFileSize: {}MB",
                brandId, brandMetrics.totalEvents.get(),
                brandMetrics.totalFileSize.get() / 1024 / 1024);
    }

    /**
     * 파일 작업 모니터링
     */
    private void monitorFileOperations(Object[] args) {
        int fileCount = 0;
        long totalSize = 0;

        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                if (!file.isEmpty()) {
                    fileCount++;
                    totalSize += file.getSize();
                }
            } else if (arg instanceof List<?>) {
                List<?> list = (List<?>) arg;
                for (Object item : list) {
                    if (item instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) item;
                        if (!file.isEmpty()) {
                            fileCount++;
                            totalSize += file.getSize();
                        }
                    }
                }
            }
        }

        if (fileCount > 0) {
            log.info("FILE_OPERATION_MONITORING - FileCount: {}, TotalSize: {}MB",
                    fileCount, totalSize / 1024 / 1024);
        }
    }

    /**
     * 상태 변경 확인 및 알림
     */
    private void checkAndAlertStatusChange(String methodName, Object[] args, Object result) {
        if (!methodName.contains("Status")) return;

        Long eventId = extractEventId(args);
        EventStatus newStatus = extractEventStatus(args);

        if (eventId != null && newStatus != null) {
            log.warn("STATUS_CHANGE_ALERT - EventId: {}, NewStatus: {}", eventId, newStatus);

            // 실제 환경에서는 이메일, 슬랙 등으로 알림 발송
            sendStatusChangeNotification(eventId, newStatus);
        }
    }

    /**
     * 임계값 체크 및 알림
     */
    private void checkThresholdsAndAlert(EventMonitoring annotation, Object[] args, long executionTime) {
        // 대용량 파일 업로드 알림
        if (annotation.alertOnLargeFileUpload()) {
            long fileSize = calculateTotalFileSize(args);
            long thresholdBytes = annotation.fileUploadThresholdMB() * 1024 * 1024;

            if (fileSize > thresholdBytes) {
                log.warn("LARGE_FILE_ALERT - FileSize: {}MB exceeds threshold: {}MB",
                        fileSize / 1024 / 1024, annotation.fileUploadThresholdMB());
            }
        }

        // 이벤트 생성 빈도 알림
        if (annotation.monitorCreationFrequency()) {
            checkCreationFrequency(annotation.eventCreationThresholdPerHour());
        }

        // 캐시 히트율 알림
        if (annotation.monitorCachePerformance()) {
            double hitRate = calculateCacheHitRate();
            if (hitRate < annotation.cacheHitRateThreshold()) {
                log.warn("CACHE_PERFORMANCE_ALERT - HitRate: {:.2f} below threshold: {}",
                        hitRate, annotation.cacheHitRateThreshold());
            }
        }
    }

    /**
     * 생성 빈도 체크
     */
    private void checkCreationFrequency(int threshold) {
        for (Map.Entry<Long, BrandEventMetrics> entry : brandMetricsMap.entrySet()) {
            if (entry.getValue().creationFrequency.get() > threshold) {
                log.warn("CREATION_FREQUENCY_ALERT - BrandId: {} created {} events in last hour",
                        entry.getKey(), entry.getValue().creationFrequency.get());
            }
        }
    }

    /**
     * 캐시 히트율 계산
     */
    private double calculateCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;

        return total > 0 ? (double) hits / total : 0.0;
    }

    /**
     * 메트릭 리포트 스케줄링
     */
    private void scheduleMetricsReport(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::generateMetricsReport,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * 메트릭 리포트 생성
     */
    private void generateMetricsReport() {
        log.info("=== EVENT MONITORING METRICS REPORT ===");

        // 메서드별 메트릭
        methodMetricsMap.forEach((method, metrics) -> {
            long totalCalls = metrics.totalCalls.get();
            if (totalCalls > 0) {
                double avgExecutionTime = (double) metrics.totalExecutionTime.get() / totalCalls;
                double successRate = (double) metrics.successCalls.get() / totalCalls * 100;

                log.info("Method: {}, Calls: {}, SuccessRate: {:.1f}%, " +
                                "AvgTime: {:.1f}ms, MinTime: {}ms, MaxTime: {}ms",
                        method, totalCalls, successRate, avgExecutionTime,
                        metrics.minExecutionTime, metrics.maxExecutionTime);
            }
        });

        // 브랜드별 통계
        brandMetricsMap.forEach((brandId, metrics) -> {
            log.info("BrandId: {}, TotalEvents: {}, ActiveEvents: {}, " +
                            "TotalFileSize: {}MB, CreationFreq: {}/hour",
                    brandId, metrics.totalEvents.get(), metrics.activeEvents.get(),
                    metrics.totalFileSize.get() / 1024 / 1024,
                    metrics.creationFrequency.get());
        });

        // 캐시 성능
        double cacheHitRate = calculateCacheHitRate();
        log.info("Cache Performance - Hits: {}, Misses: {}, HitRate: {:.1f}%",
                cacheHits.get(), cacheMisses.get(), cacheHitRate * 100);

        log.info("=== END OF METRICS REPORT ===");
    }

    /**
     * 헬퍼 메서드들
     */
    private String extractUserEmail() {
        try {
            return SecurityUtils.extractEmailFromSecurityContext();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private Long extractEventId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            } else if (arg instanceof Event) {
                return ((Event) arg).getId();
            }
        }
        return null;
    }

    private Long extractBrandId(Object[] args) {
        // CreateEventRequest나 다른 DTO에서 brandId 추출
        return null; // 실제 구현 필요
    }

    private EventStatus extractEventStatus(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof EventStatus) {
                return (EventStatus) arg;
            }
        }
        return null;
    }

    private long calculateTotalFileSize(Object[] args) {
        long totalSize = 0;
        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                totalSize += ((MultipartFile) arg).getSize();
            } else if (arg instanceof List<?>) {
                List<?> list = (List<?>) arg;
                for (Object item : list) {
                    if (item instanceof MultipartFile) {
                        totalSize += ((MultipartFile) item).getSize();
                    }
                }
            }
        }
        return totalSize;
    }

    private boolean shouldCollectMetric(EventMonitoring.MetricType type, EventMonitoring.MetricType[] metrics) {
        for (EventMonitoring.MetricType metric : metrics) {
            if (metric == type) {
                return true;
            }
        }
        return false;
    }

    private void sendStatusChangeNotification(Long eventId, EventStatus status) {
        // 실제 알림 발송 로직
        log.info("Sending status change notification for event: {}, status: {}", eventId, status);
    }
}