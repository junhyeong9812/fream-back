package com.fream.back.global.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 캐시 설정
 * - 검수 기준 캐시 추가
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(
                Arrays.asList(
                        // FAQ 관련 캐시
                        "faqList", "faqDetail", "faqCategoryList", "faqSearchResults",
                        // 검수 기준 관련 캐시 추가
                        "inspectionStandards", "inspectionStandardsByCategory",
                        "inspectionStandardDetail", "inspectionStandardSearchResults"
                )
        );
        return cacheManager;
    }
}