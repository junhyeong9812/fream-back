package com.fream.back.domain.faq.aop;

import com.fream.back.domain.faq.aop.annotation.FAQCache;
import com.fream.back.domain.faq.entity.FAQCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * FAQ 도메인 캐싱 AOP
 * FAQ는 자주 조회되지만 변경이 적은 특성을 고려한 적극적 캐싱 전략
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class FAQCacheAspect {

    private final CacheManager cacheManager;

    // 캐시 통계
    private final Map<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();

    // 인기 FAQ 추적 (조회수 기반)
    private final Map<Long, AtomicLong> faqHitCount = new ConcurrentHashMap<>();

    // 카테고리별 캐시 무효화 시간
    private final Map<FAQCategory, LocalDateTime> categoryInvalidationTime = new ConcurrentHashMap<>();

    private static class CacheStatistics {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private volatile LocalDateTime lastAccess;
        private volatile double averageLoadTime = 0;

        public double getHitRatio() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total * 100 : 0;
        }
    }

    @Around("@annotation(faqCache)")
    public Object manageFAQCache(ProceedingJoinPoint joinPoint, FAQCache faqCache) throws Throwable {
        if (!faqCache.enabled()) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 캐시 키 생성
        String cacheKey = generateCacheKey(faqCache, methodName, args);
        String cacheName = determineCacheName(faqCache, args);

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("FAQ_CACHE_NOT_FOUND - CacheName: {}", cacheName);
            return joinPoint.proceed();
        }

        CacheStatistics stats = cacheStats.computeIfAbsent(cacheName, k -> new CacheStatistics());

        // 캐시 조회
        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        if (wrapper != null && wrapper.get() != null) {
            // 캐시 히트
            stats.hits.incrementAndGet();
            stats.lastAccess = LocalDateTime.now();

            // FAQ ID 추출 및 조회수 증가
            if (faqCache.trackHitCount()) {
                Long faqId = extractFaqId(args);
                if (faqId != null) {
                    faqHitCount.computeIfAbsent(faqId, k -> new AtomicLong(0)).incrementAndGet();
                }
            }

            log.debug("FAQ_CACHE_HIT - Key: {}, HitRatio: {:.1f}%",
                    cacheKey, stats.getHitRatio());

            return wrapper.get();
        }

        // 캐시 미스 - 실행 후 캐싱
        stats.misses.incrementAndGet();
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long loadTime = System.currentTimeMillis() - startTime;
        updateAverageLoadTime(stats, loadTime);

        // 결과 캐싱
        if (result != null) {
            cache.put(cacheKey, result);
            log.debug("FAQ_CACHE_PUT - Key: {}, LoadTime: {}ms", cacheKey, loadTime);
        }

        // 캐시 통계 로깅
        if (faqCache.logStatistics() && stats.hits.get() % 100 == 0) {
            logCacheStatistics(cacheName, stats);
        }

        return result;
    }

    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(FAQCache cache, String methodName, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder(methodName);

        switch (cache.keyStrategy()) {
            case ID_BASED:
                Long id = extractFaqId(args);
                if (id != null) {
                    keyBuilder.append(":").append(id);
                }
                break;

            case CATEGORY_BASED:
                FAQCategory category = extractCategory(args);
                if (category != null) {
                    keyBuilder.append(":").append(category);
                }
                Pageable pageable = extractPageable(args);
                if (pageable != null) {
                    keyBuilder.append(":p").append(pageable.getPageNumber())
                            .append(":s").append(pageable.getPageSize());
                }
                break;

            case SEARCH_BASED:
                String keyword = extractKeyword(args);
                if (keyword != null && !keyword.isEmpty()) {
                    keyBuilder.append(":").append(keyword.hashCode());
                }
                pageable = extractPageable(args);
                if (pageable != null) {
                    keyBuilder.append(":p").append(pageable.getPageNumber())
                            .append(":s").append(pageable.getPageSize());
                }
                break;

            case COMPOSITE:
                keyBuilder.append(":").append(Arrays.hashCode(args));
                break;

            case CUSTOM:
                // 커스텀 키 생성 로직
                keyBuilder.append(":custom");
                break;

            default:
                keyBuilder.append(":default");
        }

        return keyBuilder.toString();
    }

    /**
     * 캐시 이름 결정
     */
    private String determineCacheName(FAQCache cache, Object[] args) {
        if (cache.useMultipleCaches()) {
            FAQCategory category = extractCategory(args);
            if (category != null) {
                return "faqCategory_" + category.name();
            }
        }
        return cache.cacheName();
    }

    /**
     * 카테고리별 캐시 무효화
     */
    public void invalidateCategoryCache(FAQCategory category) {
        String cacheName = "faqCategory_" + category.name();
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            categoryInvalidationTime.put(category, LocalDateTime.now());

            CacheStatistics stats = cacheStats.get(cacheName);
            if (stats != null) {
                stats.evictions.incrementAndGet();
            }

            log.info("FAQ_CACHE_INVALIDATE - Category: {}", category);
        }
    }

    /**
     * 전체 캐시 무효화
     */
    public void invalidateAllCaches() {
        cacheManager.getCacheNames().stream()
                .filter(name -> name.startsWith("faq"))
                .forEach(name -> {
                    Cache cache = cacheManager.getCache(name);
                    if (cache != null) {
                        cache.clear();
                        CacheStatistics stats = cacheStats.get(name);
                        if (stats != null) {
                            stats.evictions.incrementAndGet();
                        }
                    }
                });
        log.info("FAQ_CACHE_INVALIDATE_ALL - All FAQ caches cleared");
    }

    /**
     * 특정 FAQ 캐시 무효화
     */
    public void invalidateFaqCache(Long faqId) {
        cacheManager.getCacheNames().stream()
                .filter(name -> name.startsWith("faq"))
                .forEach(name -> {
                    Cache cache = cacheManager.getCache(name);
                    if (cache != null) {
                        // ID 기반 키로 무효화
                        cache.evict("getFAQ:" + faqId);
                        cache.evict("findById:" + faqId);
                    }
                });
        log.info("FAQ_CACHE_INVALIDATE - FAQ ID: {}", faqId);
    }

    /**
     * 인기 FAQ 조회
     */
    public Map<Long, Long> getPopularFAQs(int limit) {
        return faqHitCount.entrySet().stream()
                .sorted(Map.Entry.<Long, AtomicLong>comparingByValue(
                        (a, b) -> Long.compare(b.get(), a.get()))
                )
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 캐시 통계 조회
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        cacheStats.forEach((cacheName, cacheStats) -> {
            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("hits", cacheStats.hits.get());
            cacheInfo.put("misses", cacheStats.misses.get());
            cacheInfo.put("hitRatio", String.format("%.1f%%", cacheStats.getHitRatio()));
            cacheInfo.put("evictions", cacheStats.evictions.get());
            cacheInfo.put("lastAccess", cacheStats.lastAccess);
            cacheInfo.put("avgLoadTime", String.format("%.2fms", cacheStats.averageLoadTime));

            stats.put(cacheName, cacheInfo);
        });

        // 인기 FAQ Top 10
        stats.put("popularFAQs", getPopularFAQs(10));

        return stats;
    }

    /**
     * 캐시 통계 로깅
     */
    private void logCacheStatistics(String cacheName, CacheStatistics stats) {
        log.info("FAQ_CACHE_STATS - Cache: {}, Hits: {}, Misses: {}, " +
                        "HitRatio: {:.1f}%, AvgLoadTime: {:.0f}ms, LastAccess: {}",
                cacheName, stats.hits.get(), stats.misses.get(),
                stats.getHitRatio(), stats.averageLoadTime, stats.lastAccess);
    }

    /**
     * 평균 로드 시간 업데이트 (이동 평균)
     */
    private void updateAverageLoadTime(CacheStatistics stats, long newTime) {
        stats.averageLoadTime = (stats.averageLoadTime * 0.9) + (newTime * 0.1);
    }

    // 헬퍼 메서드들
    private Long extractFaqId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private FAQCategory extractCategory(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof FAQCategory) {
                return (FAQCategory) arg;
            }
        }
        return null;
    }

    private String extractKeyword(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return null;
    }

    private Pageable extractPageable(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Pageable) {
                return (Pageable) arg;
            }
        }
        return null;
    }

    /**
     * 캐시 워밍업 - 자주 조회되는 FAQ 미리 로드
     */
    public void warmUpCache() {
        log.info("FAQ_CACHE_WARMUP_START");

        // 인기 FAQ Top 20을 미리 로드
        Map<Long, Long> popularFaqs = getPopularFAQs(20);

        // 실제 구현에서는 Service를 통해 FAQ 조회하여 캐시에 로드
        popularFaqs.keySet().forEach(faqId -> {
            // faqService.getFAQ(faqId); // 이렇게 하면 자동으로 캐싱됨
            log.debug("FAQ_CACHE_WARMUP - Loaded FAQ ID: {}", faqId);
        });

        log.info("FAQ_CACHE_WARMUP_COMPLETE - Loaded {} FAQs", popularFaqs.size());
    }
}