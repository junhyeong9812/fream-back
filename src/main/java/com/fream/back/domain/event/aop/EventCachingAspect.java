package com.fream.back.domain.event.aop;

import com.fream.back.domain.event.aop.annotation.EventCaching;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event 도메인 캐싱 AOP
 * 이벤트 조회 성능 최적화를 위한 캐싱 제어
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(4)
public class EventCachingAspect {

    private final CacheManager cacheManager;

    // 캐시 메트릭
    private final ConcurrentHashMap<String, CacheMetrics> cacheMetricsMap = new ConcurrentHashMap<>();

    // 캐시 워밍업 스케줄러
    private final ScheduledExecutorService warmUpScheduler = Executors.newScheduledThreadPool(1);

    // 캐시 무효화 추적
    private final ConcurrentHashMap<String, LocalDateTime> lastInvalidation = new ConcurrentHashMap<>();

    /**
     * 캐시 메트릭
     */
    private static class CacheMetrics {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final AtomicLong puts = new AtomicLong(0);
        private volatile LocalDateTime lastAccess;
        private volatile long totalAccessTime = 0;

        public double getHitRate() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total : 0.0;
        }

        public long getAverageAccessTime() {
            long total = hits.get() + misses.get();
            return total > 0 ? totalAccessTime / total : 0;
        }
    }

    @Around("@annotation(eventCaching)")
    public Object manageCaching(ProceedingJoinPoint joinPoint, EventCaching eventCaching) throws Throwable {
        if (!eventCaching.enabled()) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        // 캐시 키 생성
        String cacheKey = generateCacheKey(eventCaching, className, methodName, args);
        String cacheName = determineCacheName(eventCaching, methodName);

        log.debug("CACHE_CHECK - CacheName: {}, Key: {}", cacheName, cacheKey);

        // 조건부 캐싱 체크
        if (eventCaching.conditionalCaching() && !evaluateCacheCondition(eventCaching.condition(), args)) {
            log.debug("CACHE_SKIP - Condition not met for key: {}", cacheKey);
            return joinPoint.proceed();
        }

        // 캐시 조회
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("CACHE_NOT_FOUND - CacheName: {}", cacheName);
            return joinPoint.proceed();
        }

        CacheMetrics metrics = cacheMetricsMap.computeIfAbsent(cacheName, k -> new CacheMetrics());
        long startTime = System.currentTimeMillis();

        try {
            // 캐시에서 값 조회
            Cache.ValueWrapper wrapper = cache.get(cacheKey);

            if (wrapper != null && wrapper.get() != null) {
                // 캐시 히트
                metrics.hits.incrementAndGet();
                metrics.lastAccess = LocalDateTime.now();

                Object cachedValue = wrapper.get();

                // TTL 체크 (Spring Cache가 자동으로 처리하지 않는 경우)
                if (isExpired(cacheKey, eventCaching.ttlSeconds())) {
                    log.debug("CACHE_EXPIRED - Key: {}", cacheKey);
                    cache.evict(cacheKey);
                    metrics.evictions.incrementAndGet();
                    return executeAndCache(joinPoint, cache, cacheKey, metrics);
                }

                long accessTime = System.currentTimeMillis() - startTime;
                metrics.totalAccessTime += accessTime;

                log.debug("CACHE_HIT - Key: {}, AccessTime: {}ms", cacheKey, accessTime);

                // 메트릭 수집
                if (eventCaching.collectMetrics()) {
                    logCacheMetrics(cacheName, metrics);
                }

                return cachedValue;

            } else {
                // 캐시 미스
                metrics.misses.incrementAndGet();
                log.debug("CACHE_MISS - Key: {}", cacheKey);

                return executeAndCache(joinPoint, cache, cacheKey, metrics);
            }

        } catch (Exception e) {
            log.error("CACHE_ERROR - Error accessing cache: {}", e.getMessage());
            return joinPoint.proceed();
        } finally {
            // 캐시 워밍업 스케줄링
            if (eventCaching.warmUp()) {
                scheduleWarmUp(cacheName, methodName, args);
            }
        }
    }

    /**
     * 메서드 실행 후 캐싱
     */
    private Object executeAndCache(ProceedingJoinPoint joinPoint, Cache cache,
                                   String cacheKey, CacheMetrics metrics) throws Throwable {
        Object result = joinPoint.proceed();

        if (result != null) {
            cache.put(cacheKey, result);
            metrics.puts.incrementAndGet();
            log.debug("CACHE_PUT - Key: {}", cacheKey);
        }

        return result;
    }

    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(EventCaching caching, String className,
                                    String methodName, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();

        switch (caching.keyStrategy()) {
            case EVENT_ID:
                keyBuilder.append("event:");
                appendEventId(keyBuilder, args);
                break;

            case BRAND_ID:
                keyBuilder.append("brand:");
                appendBrandId(keyBuilder, args);
                break;

            case USER_SPECIFIC:
                keyBuilder.append("user:");
                appendUserId(keyBuilder);
                break;

            case STATUS_BASED:
                keyBuilder.append("status:");
                appendStatus(keyBuilder, args);
                break;

            case TIME_BASED:
                keyBuilder.append("time:");
                appendTimeSegment(keyBuilder);
                break;

            case COMPOSITE:
                keyBuilder.append("composite:");
                appendCompositeKey(keyBuilder, args);
                break;

            default: // DEFAULT
                keyBuilder.append(className).append(":").append(methodName);
                if (args != null && args.length > 0) {
                    keyBuilder.append(":").append(Arrays.hashCode(args));
                }
        }

        return keyBuilder.toString();
    }

    /**
     * 캐시 이름 결정
     */
    private String determineCacheName(EventCaching caching, String methodName) {
        String baseName = "eventCache";

        if (caching.separateByBrand()) {
            baseName += "_brand";
        }

        if (caching.separateByUserRole()) {
            baseName += isAdminUser() ? "_admin" : "_user";
        }

        return baseName;
    }

    /**
     * 캐시 조건 평가
     */
    private boolean evaluateCacheCondition(String condition, Object[] args) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        // 간단한 조건 평가 (실제로는 SpEL 사용 가능)
        if (condition.contains("hasArgs") && (args == null || args.length == 0)) {
            return false;
        }

        return true;
    }

    /**
     * 캐시 만료 체크
     */
    private boolean isExpired(String cacheKey, int ttlSeconds) {
        LocalDateTime lastInvalidated = lastInvalidation.get(cacheKey);
        if (lastInvalidated == null) {
            return false;
        }

        return LocalDateTime.now().isAfter(lastInvalidated.plusSeconds(ttlSeconds));
    }

    /**
     * 캐시 워밍업 스케줄링
     */
    private void scheduleWarmUp(String cacheName, String methodName, Object[] args) {
        warmUpScheduler.schedule(() -> {
            try {
                log.info("CACHE_WARMUP_START - CacheName: {}, Method: {}", cacheName, methodName);
                // 자주 사용되는 데이터 미리 로드
                warmUpFrequentData(cacheName);
                log.info("CACHE_WARMUP_COMPLETE - CacheName: {}", cacheName);
            } catch (Exception e) {
                log.error("CACHE_WARMUP_ERROR - {}", e.getMessage());
            }
        }, 5, TimeUnit.MINUTES);
    }

    /**
     * 자주 사용되는 데이터 워밍업
     */
    private void warmUpFrequentData(String cacheName) {
        // 활성 이벤트 캐싱
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            // 워밍업 로직
            log.debug("Warming up cache: {}", cacheName);
        }
    }

    /**
     * 캐시 메트릭 로깅
     */
    private void logCacheMetrics(String cacheName, CacheMetrics metrics) {
        double hitRate = metrics.getHitRate();
        long avgAccessTime = metrics.getAverageAccessTime();

        log.info("CACHE_METRICS - Cache: {}, HitRate: {:.2f}%, Hits: {}, Misses: {}, " +
                        "Puts: {}, Evictions: {}, AvgAccessTime: {}ms",
                cacheName, hitRate * 100, metrics.hits.get(), metrics.misses.get(),
                metrics.puts.get(), metrics.evictions.get(), avgAccessTime);
    }

    /**
     * 상태 변경 시 캐시 무효화
     */
    public void invalidateCacheOnStatusChange(Long eventId, EventStatus newStatus) {
        log.info("CACHE_INVALIDATE_STATUS - EventId: {}, NewStatus: {}", eventId, newStatus);

        // 이벤트 관련 캐시 무효화
        String[] cacheNames = {"eventCache", "eventCache_brand", "eventCache_admin", "eventCache_user"};

        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // 이벤트 ID 관련 키 무효화
                cache.evict("event:" + eventId);

                // 상태별 캐시 무효화
                cache.evict("status:" + newStatus);

                CacheMetrics metrics = cacheMetricsMap.get(cacheName);
                if (metrics != null) {
                    metrics.evictions.incrementAndGet();
                }
            }
        }

        lastInvalidation.put("event:" + eventId, LocalDateTime.now());
    }

    /**
     * 브랜드별 캐시 무효화
     */
    public void invalidateBrandCache(Long brandId) {
        log.info("CACHE_INVALIDATE_BRAND - BrandId: {}", brandId);

        Cache cache = cacheManager.getCache("eventCache_brand");
        if (cache != null) {
            cache.evict("brand:" + brandId);
        }
    }

    /**
     * 전체 캐시 클리어
     */
    public void clearAllCaches() {
        log.warn("CACHE_CLEAR_ALL - Clearing all event caches");

        cacheManager.getCacheNames().forEach(cacheName -> {
            if (cacheName.startsWith("event")) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        });
    }

    /**
     * 헬퍼 메서드들
     */
    private void appendEventId(StringBuilder builder, Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                builder.append(arg);
                return;
            }
        }
    }

    private void appendBrandId(StringBuilder builder, Object[] args) {
        // CreateEventRequest 등에서 brandId 추출
        builder.append("default");
    }

    private void appendUserId(StringBuilder builder) {
        try {
            String email = SecurityUtils.extractEmailFromSecurityContext();
            builder.append(email != null ? email.hashCode() : "anonymous");
        } catch (Exception e) {
            builder.append("anonymous");
        }
    }

    private void appendStatus(StringBuilder builder, Object[] args) {
        for (Object arg : args) {
            if (arg instanceof EventStatus) {
                builder.append(arg);
                return;
            }
        }
        builder.append("all");
    }

    private void appendTimeSegment(StringBuilder builder) {
        // 5분 단위로 세그먼트
        long segment = System.currentTimeMillis() / (5 * 60 * 1000);
        builder.append(segment);
    }

    private void appendCompositeKey(StringBuilder builder, Object[] args) {
        // 복합 키 생성
        appendUserId(builder);
        builder.append(":");
        appendEventId(builder, args);
        builder.append(":");
        appendTimeSegment(builder);
    }

    private boolean isAdminUser() {
        // 관리자 여부 체크 로직
        return false;
    }

    /**
     * 캐시 통계 리포트
     */
    public void generateCacheReport() {
        log.info("=== CACHE STATISTICS REPORT ===");

        cacheMetricsMap.forEach((cacheName, metrics) -> {
            log.info("Cache: {}, HitRate: {:.1f}%, TotalHits: {}, TotalMisses: {}, " +
                            "TotalPuts: {}, TotalEvictions: {}, LastAccess: {}",
                    cacheName,
                    metrics.getHitRate() * 100,
                    metrics.hits.get(),
                    metrics.misses.get(),
                    metrics.puts.get(),
                    metrics.evictions.get(),
                    metrics.lastAccess);
        });

        log.info("=== END OF CACHE REPORT ===");
    }
}