package com.fream.back.domain.faq.aop;

import com.fream.back.domain.faq.aop.annotation.FAQSearchOptimization;
import com.fream.back.domain.faq.aop.annotation.FAQSearchOptimization.*;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.entity.FAQ;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FAQSearchOptimizationAspect {

    private final JdbcTemplate jdbcTemplate;
    private final CacheManager cacheManager;

    // 동의어 사전
    private final Map<String, Set<String>> synonymDictionary = new ConcurrentHashMap<>();

    // 검색 로그 및 통계
    private final Map<String, AtomicLong> searchQueryLog = new ConcurrentHashMap<>();
    private final Map<String, SearchResult> searchResultCache = new ConcurrentHashMap<>();

    // 자동완성 데이터
    private final Set<String> autocompleteData = ConcurrentHashMap.newKeySet();

    // 오타 교정 사전
    private final Map<String, String> typoCorrections = new ConcurrentHashMap<>();

    // 한글 초성 매핑
    private static final String[] CHOSUNG = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ",
            "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    @PostConstruct
    public void init() {
        initializeSynonymDictionary();
        initializeTypoCorrections();
        loadPopularKeywords();
        log.info("FAQ Search Optimization initialized");
    }

    @Around("@annotation(searchOptimization)")
    public Object optimizeSearch(ProceedingJoinPoint joinPoint,
                                 FAQSearchOptimization searchOptimization) throws Throwable {

        if (!searchOptimization.enabled()) {
            return joinPoint.proceed();
        }

        // 검색어 추출
        String originalKeyword = extractKeyword(joinPoint);
        if (originalKeyword == null || originalKeyword.trim().isEmpty()) {
            return joinPoint.proceed();
        }

        // 캐시 체크
        if (searchOptimization.enableCaching()) {
            Object cached = getCachedResult(originalKeyword, searchOptimization);
            if (cached != null) {
                return cached;
            }
        }

        // 검색어 최적화
        String optimizedKeyword = optimizeKeyword(originalKeyword, searchOptimization);

        // 검색 실행
        Object[] args = modifyArgs(joinPoint.getArgs(), optimizedKeyword);
        Object result = joinPoint.proceed(args);

        // 결과 처리
        result = processSearchResult(result, originalKeyword, optimizedKeyword, searchOptimization);

        // 로깅 및 통계
        if (searchOptimization.logSearchQueries()) {
            logSearchQuery(originalKeyword, optimizedKeyword, result);
        }

        // 캐싱
        if (searchOptimization.enableCaching()) {
            cacheSearchResult(originalKeyword, result, searchOptimization);
        }

        // 인기 검색어 추적
        if (searchOptimization.trackPopularKeywords()) {
            trackKeyword(originalKeyword);
        }

        return result;
    }

    private String optimizeKeyword(String keyword, FAQSearchOptimization optimization) {
        String processed = keyword;

        // 1. 정규화
        if (optimization.normalizeKeyword()) {
            processed = normalizeKeyword(processed);
        }

        // 2. 특수문자 제거
        if (optimization.removeSpecialChars()) {
            processed = removeSpecialCharacters(processed);
        }

        // 3. 한글 정규화
        if (optimization.normalizeKorean()) {
            processed = normalizeKorean(processed);
        }

        // 4. 오타 교정
        if (optimization.enableTypoCorrection()) {
            processed = correctTypo(processed);
        }

        // 5. 동의어 확장
        if (optimization.enableSynonymExpansion()) {
            processed = expandWithSynonyms(processed);
        }

        // 6. 초성 검색 처리
        if (optimization.supportChosungSearch() && isChosungQuery(processed)) {
            processed = convertChosungToRegex(processed);
        }

        log.debug("Keyword optimization: '{}' -> '{}'", keyword, processed);
        return processed;
    }

    private Object processSearchResult(Object result, String originalKeyword,
                                       String optimizedKeyword, FAQSearchOptimization optimization) {

        // Page 타입 처리
        if (result instanceof Page) {
            Page<?> page = (Page<?>) result;

            // 결과가 없고 추천이 활성화된 경우
            if (page.isEmpty() && optimization.suggestOnNoResult()) {
                List<FAQResponseDto> suggestions = getSuggestions(
                        originalKeyword,
                        optimization.maxSuggestions()
                );

                if (!suggestions.isEmpty()) {
                    log.info("No results found for '{}', providing {} suggestions",
                            originalKeyword, suggestions.size());
                    return new PageImpl<>(suggestions, page.getPageable(), suggestions.size());
                }
            }

            // 검색 점수 기반 필터링
            if (optimization.scoreThreshold() > 0) {
                List<?> filtered = filterByScore(page.getContent(), optimization.scoreThreshold());
                return new PageImpl<>(filtered, page.getPageable(), filtered.size());
            }

            // 정렬 처리
            if (optimization.sortBy() != SortBy.RELEVANCE) {
                List<?> sorted = sortResults(page.getContent(), optimization.sortBy());
                return new PageImpl<>(sorted, page.getPageable(), page.getTotalElements());
            }
        }

        return result;
    }

    private String normalizeKeyword(String keyword) {
        return keyword.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String removeSpecialCharacters(String keyword) {
        return keyword.replaceAll("[^가-힣a-zA-Z0-9\\s]", "");
    }

    private String normalizeKorean(String keyword) {
        // 한글 자모 분리 및 정규화
        StringBuilder normalized = new StringBuilder();

        for (char ch : keyword.toCharArray()) {
            if (ch >= '가' && ch <= '힣') {
                // 한글 분해
                int unicode = ch - 0xAC00;
                int cho = unicode / (21 * 28);
                int jung = (unicode % (21 * 28)) / 28;
                int jong = unicode % 28;

                normalized.append(CHOSUNG[cho]);
                // 중성, 종성도 필요시 추가
            } else {
                normalized.append(ch);
            }
        }

        return normalized.toString();
    }

    private String correctTypo(String keyword) {
        // 오타 교정 사전 확인
        String corrected = typoCorrections.get(keyword);
        if (corrected != null) {
            log.debug("Typo correction: '{}' -> '{}'", keyword, corrected);
            return corrected;
        }

        // 레벤슈타인 거리 기반 교정
        for (Map.Entry<String, String> entry : typoCorrections.entrySet()) {
            if (calculateLevenshteinDistance(keyword, entry.getKey()) <= 2) {
                return entry.getValue();
            }
        }

        return keyword;
    }

    private String expandWithSynonyms(String keyword) {
        Set<String> expansions = new HashSet<>();
        expansions.add(keyword);

        // 각 단어에 대해 동의어 확장
        String[] words = keyword.split("\\s+");
        for (String word : words) {
            Set<String> synonyms = synonymDictionary.get(word.toLowerCase());
            if (synonyms != null) {
                expansions.addAll(synonyms);
            }
        }

        if (expansions.size() > 1) {
            return String.join(" OR ", expansions);
        }

        return keyword;
    }

    private boolean isChosungQuery(String keyword) {
        // 초성으로만 구성되었는지 확인
        for (char ch : keyword.toCharArray()) {
            boolean isChosung = false;
            for (String cho : CHOSUNG) {
                if (cho.charAt(0) == ch) {
                    isChosung = true;
                    break;
                }
            }
            if (!isChosung && !Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    private String convertChosungToRegex(String chosung) {
        StringBuilder regex = new StringBuilder();

        for (char ch : chosung.toCharArray()) {
            int index = -1;
            for (int i = 0; i < CHOSUNG.length; i++) {
                if (CHOSUNG[i].charAt(0) == ch) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                // 해당 초성으로 시작하는 모든 한글
                char start = (char) (0xAC00 + index * 21 * 28);
                char end = (char) (start + 21 * 28 - 1);
                regex.append("[").append(start).append("-").append(end).append("]");
            }
        }

        return regex.toString();
    }

    private List<FAQResponseDto> getSuggestions(String keyword, int maxSuggestions) {
        try {
            // 인기 검색어 기반 추천
            String sql = "SELECT f.* FROM faq f " +
                    "JOIN faq_search_log l ON f.id = l.faq_id " +
                    "WHERE l.search_count > 10 " +
                    "ORDER BY l.search_count DESC " +
                    "LIMIT ?";

            // JDBC를 사용한 조회 (실제로는 Repository 사용 권장)
            List<FAQ> suggestions = new ArrayList<>();
            // ... 조회 로직

            return suggestions.stream()
                    .limit(maxSuggestions)
                    .map(faq -> FAQResponseDto.builder()
                            .id(faq.getId())
                            .question(faq.getQuestion())
                            .answer(faq.getAnswer())
                            .category(faq.getCategory().name())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting suggestions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<?> filterByScore(List<?> results, double threshold) {
        // 점수 기반 필터링 (실제 구현은 검색 엔진에 따라 다름)
        return results.stream()
                .filter(item -> calculateScore(item) >= threshold)
                .collect(Collectors.toList());
    }

    private double calculateScore(Object item) {
        // 검색 점수 계산 로직
        // 실제로는 검색 엔진의 스코어링 사용
        return 1.0;
    }

    private List<?> sortResults(List<?> results, SortBy sortBy) {
        switch (sortBy) {
            case POPULARITY:
                return sortByPopularity(results);
            case RECENT:
                return sortByRecent(results);
            case ALPHABETICAL:
                return sortByAlphabetical(results);
            default:
                return results;
        }
    }

    private List<?> sortByPopularity(List<?> results) {
        // 인기도 기반 정렬
        return results.stream()
                .sorted((a, b) -> {
                    Long viewsA = getViewCount(a);
                    Long viewsB = getViewCount(b);
                    return viewsB.compareTo(viewsA);
                })
                .collect(Collectors.toList());
    }

    private List<?> sortByRecent(List<?> results) {
        // 최신순 정렬
        return results.stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = getCreatedDate(a);
                    LocalDateTime dateB = getCreatedDate(b);
                    return dateB.compareTo(dateA);
                })
                .collect(Collectors.toList());
    }

    private List<?> sortByAlphabetical(List<?> results) {
        // 알파벳순 정렬
        return results.stream()
                .sorted(Comparator.comparing(this::getTitle))
                .collect(Collectors.toList());
    }

    private Object getCachedResult(String keyword, FAQSearchOptimization optimization) {
        String cacheKey = "search:" + keyword.hashCode();
        SearchResult cached = searchResultCache.get(cacheKey);

        if (cached != null) {
            long age = System.currentTimeMillis() - cached.timestamp;
            if (age < optimization.cacheTTLSeconds() * 1000) {
                log.debug("Search cache hit for keyword: {}", keyword);
                return cached.result;
            }
        }

        return null;
    }

    private void cacheSearchResult(String keyword, Object result, FAQSearchOptimization optimization) {
        String cacheKey = "search:" + keyword.hashCode();
        searchResultCache.put(cacheKey, new SearchResult(result, System.currentTimeMillis()));

        // 캐시 크기 제한
        if (searchResultCache.size() > 1000) {
            cleanOldCache();
        }
    }

    private void cleanOldCache() {
        long now = System.currentTimeMillis();
        searchResultCache.entrySet().removeIf(entry ->
                now - entry.getValue().timestamp > 3600000 // 1시간 이상 된 캐시 제거
        );
    }

    private void logSearchQuery(String original, String optimized, Object result) {
        try {
            int resultCount = 0;
            if (result instanceof Page) {
                resultCount = (int) ((Page<?>) result).getTotalElements();
            }

            String sql = "INSERT INTO faq_search_log (keyword, optimized_keyword, " +
                    "result_count, searched_at) VALUES (?, ?, ?, ?)";

            jdbcTemplate.update(sql, original, optimized, resultCount, LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error logging search query: {}", e.getMessage());
        }
    }

    private void trackKeyword(String keyword) {
        searchQueryLog.computeIfAbsent(keyword, k -> new AtomicLong(0)).incrementAndGet();

        // 자동완성 데이터 추가
        if (searchQueryLog.get(keyword).get() > 5) {
            autocompleteData.add(keyword);
        }
    }

    private String extractKeyword(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if ("keyword".equalsIgnoreCase(paramNames[i]) && args[i] != null) {
                return args[i].toString();
            }
        }

        return null;
    }

    private Object[] modifyArgs(Object[] args, String newKeyword) {
        Object[] modified = Arrays.copyOf(args, args.length);

        for (int i = 0; i < modified.length; i++) {
            if (modified[i] instanceof String) {
                // 키워드로 보이는 문자열 파라미터 수정
                modified[i] = newKeyword;
                break;
            }
        }

        return modified;
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // Helper methods
    private Long getViewCount(Object item) {
        // 실제 구현 필요
        return 0L;
    }

    private LocalDateTime getCreatedDate(Object item) {
        // 실제 구현 필요
        return LocalDateTime.now();
    }

    private String getTitle(Object item) {
        // 실제 구현 필요
        return "";
    }

    // 초기화 메서드들
    private void initializeSynonymDictionary() {
        // 동의어 사전 초기화
        synonymDictionary.put("구매", Set.of("구입", "매입", "쇼핑", "buying"));
        synonymDictionary.put("판매", Set.of("매매", "거래", "selling"));
        synonymDictionary.put("환불", Set.of("반품", "반환", "리턴", "refund"));
        synonymDictionary.put("배송", Set.of("택배", "운송", "배달", "delivery"));
        synonymDictionary.put("가격", Set.of("비용", "금액", "요금", "price"));
        // ... 더 많은 동의어 추가
    }

    private void initializeTypoCorrections() {
        // 오타 교정 사전 초기화
        typoCorrections.put("구메", "구매");
        typoCorrections.put("팜매", "판매");
        typoCorrections.put("환볼", "환불");
        typoCorrections.put("배숑", "배송");
        // ... 더 많은 오타 교정 추가
    }

    private void loadPopularKeywords() {
        try {
            String sql = "SELECT keyword, COUNT(*) as count FROM faq_search_log " +
                    "WHERE searched_at > ? " +
                    "GROUP BY keyword ORDER BY count DESC LIMIT 100";

            // 최근 30일 인기 검색어 로드
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

            // JDBC 조회 로직
            // ... 구현 필요

        } catch (Exception e) {
            log.error("Error loading popular keywords: {}", e.getMessage());
        }
    }

    // 정기적으로 인기 검색어 업데이트
    @Scheduled(fixedDelay = 3600000) // 1시간마다
    public void updatePopularKeywords() {
        loadPopularKeywords();

        // 로그 정리
        searchQueryLog.entrySet().removeIf(entry -> entry.getValue().get() < 2);
    }

    // 내부 클래스
    private static class SearchResult {
        final Object result;
        final long timestamp;

        SearchResult(Object result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }
}