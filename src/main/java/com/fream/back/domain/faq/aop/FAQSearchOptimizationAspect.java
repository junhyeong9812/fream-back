package com.fream.back.domain.faq.aop;

import com.fream.back.domain.faq.aop.annotation.FAQSearchOptimization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * FAQ 검색 최적화 AOP
 * 검색어 정규화, 동의어 처리, 검색 결과 캐싱 등
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(3)
public class FAQSearchOptimizationAspect {

    // 검색 통계
    private final Map<String, SearchStatistics> searchStats = new ConcurrentHashMap<>();

    // 인기 검색어
    private final Map<String, AtomicInteger> popularKeywords = new ConcurrentHashMap<>();

    // 동의어 사전
    private final Map<String, Set<String>> synonymDictionary = new ConcurrentHashMap<>();

    // 검색 결과 캐시
    private final Map<String, CachedSearchResult> searchCache = new ConcurrentHashMap<>();

    // 검색 히스토리 (최근 100개)
    private final LinkedList<SearchHistory> searchHistories = new LinkedList<>();

    // 초성 매핑
    private static final Map<Character, Character> CHOSUNG_MAP = new HashMap<>();

    static {
        // 초성 매핑 초기화
        CHOSUNG_MAP.put('ㄱ', '가');
        CHOSUNG_MAP.put('ㄴ', '나');
        CHOSUNG_MAP.put('ㄷ', '다');
        CHOSUNG_MAP.put('ㄹ', '라');
        CHOSUNG_MAP.put('ㅁ', '마');
        CHOSUNG_MAP.put('ㅂ', '바');
        CHOSUNG_MAP.put('ㅅ', '사');
        CHOSUNG_MAP.put('ㅇ', '아');
        CHOSUNG_MAP.put('ㅈ', '자');
        CHOSUNG_MAP.put('ㅊ', '차');
        CHOSUNG_MAP.put('ㅋ', '카');
        CHOSUNG_MAP.put('ㅌ', '타');
        CHOSUNG_MAP.put('ㅍ', '파');
        CHOSUNG_MAP.put('ㅎ', '하');
    }

    private static class SearchStatistics {
        private int totalSearches = 0;
        private int successfulSearches = 0;
        private int noResultSearches = 0;
        private double averageResponseTime = 0;
        private final Map<String, Integer> keywordFrequency = new HashMap<>();
        private LocalDateTime lastSearchTime = LocalDateTime.now();

        public double getSuccessRate() {
            return totalSearches > 0 ? (double) successfulSearches / totalSearches * 100 : 0;
        }
    }

    private static class CachedSearchResult {
        private final Object result;
        private final long timestamp;
        private int hitCount;

        public CachedSearchResult(Object result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
            this.hitCount = 1;
        }

        public boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }

        public void incrementHitCount() {
            this.hitCount++;
        }
    }

    private static class SearchHistory {
        private final String keyword;
        private final String normalizedKeyword;
        private final LocalDateTime searchTime;
        private final boolean hasResult;
        private final String userEmail;

        public SearchHistory(String keyword, String normalizedKeyword, boolean hasResult, String userEmail) {
            this.keyword = keyword;
            this.normalizedKeyword = normalizedKeyword;
            this.searchTime = LocalDateTime.now();
            this.hasResult = hasResult;
            this.userEmail = userEmail;
        }
    }

    @PostConstruct
    public void init() {
        initializeSynonymDictionary();
    }

    @Around("@annotation(searchOptimization)")
    public Object optimizeSearch(ProceedingJoinPoint joinPoint, FAQSearchOptimization searchOptimization) throws Throwable {
        if (!searchOptimization.enabled()) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();

        // 검색어 추출
        String keyword = extractKeyword(args);

        if (keyword == null || keyword.isEmpty()) {
            return joinPoint.proceed();
        }

        log.debug("FAQ_SEARCH_START - Keyword: {}", keyword);

        // 검색어 정규화
        String normalizedKeyword = normalizeKeyword(keyword, searchOptimization);

        // 동의어 확장
        Set<String> expandedKeywords = new HashSet<>();
        expandedKeywords.add(normalizedKeyword);

        if (searchOptimization.enableSynonymExpansion()) {
            expandedKeywords.addAll(expandWithSynonyms(normalizedKeyword));
            log.debug("SEARCH_EXPANDED - Original: {}, Expanded: {}", keyword, expandedKeywords);
        }

        // 검색어 업데이트
        String finalSearchKeyword = String.join(" OR ", expandedKeywords);
        updateKeywordInArgs(args, finalSearchKeyword);

        // 캐시 확인
        if (searchOptimization.enableCaching()) {
            String cacheKey = buildCacheKey(normalizedKeyword, args);
            CachedSearchResult cached = searchCache.get(cacheKey);

            if (cached != null && !cached.isExpired(searchOptimization.cacheTTLSeconds() * 1000L)) {
                cached.incrementHitCount();
                log.debug("SEARCH_CACHE_HIT - Keyword: {}, HitCount: {}", normalizedKeyword, cached.hitCount);
                updateSearchStatistics(normalizedKeyword, 0, true);
                return cached.result;
            }
        }

        // 검색 실행
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed(args);
        long responseTime = System.currentTimeMillis() - startTime;

        // 결과 검증
        boolean hasResult = validateSearchResult(result);

        // 결과 캐싱
        if (searchOptimization.enableCaching() && result != null) {
            String cacheKey = buildCacheKey(normalizedKeyword, args);
            searchCache.put(cacheKey, new CachedSearchResult(result));
        }

        // 통계 업데이트
        updateSearchStatistics(normalizedKeyword, responseTime, hasResult);

        // 검색 히스토리 저장
        if (searchOptimization.logSearchQueries()) {
            saveSearchHistory(keyword, normalizedKeyword, hasResult);
        }

        // 인기 검색어 업데이트
        if (searchOptimization.trackPopularKeywords()) {
            updatePopularKeywords(normalizedKeyword);
        }

        // 검색 결과 없을 시 추천
        if (!hasResult && searchOptimization.suggestOnNoResult()) {
            result = addSuggestions(result, normalizedKeyword, searchOptimization.maxSuggestions());
        }

        log.info("FAQ_SEARCH_COMPLETE - Keyword: {}, NormalizedKeyword: {}, HasResult: {}, ResponseTime: {}ms",
                keyword, normalizedKeyword, hasResult, responseTime);

        return result;
    }

    /**
     * 검색어 정규화
     */
    private String normalizeKeyword(String keyword, FAQSearchOptimization annotation) {
        if (keyword == null) return "";

        String normalized = keyword.trim();

        // 대소문자 통일
        normalized = normalized.toLowerCase();

        // 한글 정규화
        if (annotation.normalizeKorean()) {
            normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC);
        }

        // 특수문자 제거
        if (annotation.removeSpecialChars()) {
            normalized = normalized.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");
        }

        // 중복 공백 제거
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // 초성 검색 지원
        if (annotation.supportChosungSearch()) {
            normalized = expandChosung(normalized);
        }

        return normalized;
    }

    /**
     * 동의어 확장
     */
    private Set<String> expandWithSynonyms(String keyword) {
        Set<String> expanded = new HashSet<>();
        expanded.add(keyword);

        // 전체 키워드에 대한 동의어
        Set<String> keywordSynonyms = synonymDictionary.get(keyword);
        if (keywordSynonyms != null) {
            expanded.addAll(keywordSynonyms);
        }

        // 단어별로 동의어 확장
        String[] words = keyword.split("\\s+");
        for (String word : words) {
            Set<String> synonyms = synonymDictionary.get(word);
            if (synonyms != null) {
                for (String synonym : synonyms) {
                    expanded.add(keyword.replace(word, synonym));
                }
            }
        }

        return expanded;
    }

    /**
     * 동의어 사전 초기화
     */
    private void initializeSynonymDictionary() {
        // FAQ 도메인 특화 동의어
        addSynonyms("구매", "구입", "매입", "사기", "buy", "purchase");
        addSynonyms("판매", "판매하기", "팔기", "sell", "selling");
        addSynonyms("배송", "배달", "택배", "delivery", "shipping");
        addSynonyms("환불", "반품", "취소", "refund", "return", "cancel");
        addSynonyms("가격", "비용", "금액", "price", "cost", "fee");
        addSynonyms("결제", "지불", "payment", "pay");
        addSynonyms("회원", "멤버", "member", "user", "사용자");
        addSynonyms("로그인", "signin", "login", "sign in", "log in");
        addSynonyms("비밀번호", "패스워드", "password", "pw", "pwd");
        addSynonyms("가입", "회원가입", "signup", "sign up", "register");
        addSynonyms("탈퇴", "회원탈퇴", "withdrawal", "leave");
        addSynonyms("상품", "제품", "product", "item", "goods");
        addSynonyms("주문", "오더", "order", "purchase order");
        addSynonyms("배송지", "주소", "address", "delivery address");
        addSynonyms("쿠폰", "할인", "discount", "coupon");

        log.info("SYNONYM_DICTIONARY_INITIALIZED - Total entries: {}", synonymDictionary.size());
    }

    /**
     * 동의어 추가
     */
    private void addSynonyms(String... words) {
        Set<String> synonymSet = new HashSet<>(Arrays.asList(words));
        for (String word : words) {
            synonymDictionary.put(word.toLowerCase(), synonymSet);
        }
    }

    /**
     * 초성 확장
     */
    private String expandChosung(String keyword) {
        StringBuilder expanded = new StringBuilder();

        for (char ch : keyword.toCharArray()) {
            Character replacement = CHOSUNG_MAP.get(ch);
            if (replacement != null) {
                expanded.append(replacement);
            } else {
                expanded.append(ch);
            }
        }

        return expanded.toString();
    }

    /**
     * 검색 결과 검증
     */
    private boolean validateSearchResult(Object result) {
        if (result == null) {
            return false;
        }

        if (result instanceof Page<?>) {
            Page<?> page = (Page<?>) result;
            return page.hasContent();
        }

        if (result instanceof List<?>) {
            List<?> list = (List<?>) result;
            return !list.isEmpty();
        }

        return true;
    }

    /**
     * 검색 통계 업데이트
     */
    private void updateSearchStatistics(String keyword, long responseTime, boolean successful) {
        SearchStatistics stats = searchStats.computeIfAbsent(keyword, k -> new SearchStatistics());

        stats.totalSearches++;
        if (successful) {
            stats.successfulSearches++;
        } else {
            stats.noResultSearches++;
        }

        // 이동 평균으로 응답 시간 계산
        stats.averageResponseTime = (stats.averageResponseTime * 0.9) + (responseTime * 0.1);

        // 키워드 빈도 업데이트
        stats.keywordFrequency.merge(keyword, 1, Integer::sum);

        stats.lastSearchTime = LocalDateTime.now();

        // 주기적 로깅
        if (stats.totalSearches % 100 == 0) {
            log.info("SEARCH_STATS - Keyword: {}, Total: {}, SuccessRate: {:.1f}%, NoResult: {}, AvgTime: {:.0f}ms",
                    keyword, stats.totalSearches, stats.getSuccessRate(),
                    stats.noResultSearches, stats.averageResponseTime);
        }
    }

    /**
     * 검색 히스토리 저장
     */
    private void saveSearchHistory(String keyword, String normalizedKeyword, boolean hasResult) {
        String userEmail = extractUserEmail();
        SearchHistory history = new SearchHistory(keyword, normalizedKeyword, hasResult, userEmail);

        synchronized (searchHistories) {
            searchHistories.addLast(history);
            if (searchHistories.size() > 100) {
                searchHistories.removeFirst();
            }
        }
    }

    /**
     * 인기 검색어 업데이트
     */
    private void updatePopularKeywords(String keyword) {
        popularKeywords.computeIfAbsent(keyword, k -> new AtomicInteger(0)).incrementAndGet();

        // 상위 10개 인기 검색어 주기적 로깅
        if (getTotalSearchCount() % 500 == 0) {
            logPopularKeywords();
        }
    }

    /**
     * 검색 제안 추가
     */
    private Object addSuggestions(Object result, String keyword, int maxSuggestions) {
        List<String> suggestions = generateSuggestions(keyword, maxSuggestions);

        // 실제 구현에서는 result 타입에 따라 suggestions를 추가
        log.info("SEARCH_SUGGESTIONS - Keyword: {}, Suggestions: {}", keyword, suggestions);

        return result;
    }

    /**
     * 검색 제안 생성
     */
    private List<String> generateSuggestions(String keyword, int maxSuggestions) {
        List<String> suggestions = new ArrayList<>();

        // 1. 유사한 인기 검색어 추천
        suggestions.addAll(findSimilarPopularKeywords(keyword, maxSuggestions / 2));

        // 2. 동의어 기반 추천
        Set<String> synonyms = synonymDictionary.get(keyword);
        if (synonyms != null) {
            suggestions.addAll(synonyms.stream()
                    .limit(maxSuggestions / 2)
                    .collect(Collectors.toList()));
        }

        return suggestions.stream()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }

    /**
     * 유사한 인기 검색어 찾기
     */
    private List<String> findSimilarPopularKeywords(String keyword, int limit) {
        return popularKeywords.entrySet().stream()
                .filter(entry -> calculateSimilarity(entry.getKey(), keyword) > 0.5)
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 문자열 유사도 계산 (간단한 구현)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.contains(s2) || s2.contains(s1)) return 0.7;

        // 간단한 자카드 유사도
        Set<String> set1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /**
     * 캐시 키 생성
     */
    private String buildCacheKey(String keyword, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder("search:");
        keyBuilder.append(keyword);

        // Pageable 정보 추가
        for (Object arg : args) {
            if (arg instanceof Pageable) {
                Pageable pageable = (Pageable) arg;
                keyBuilder.append(":p").append(pageable.getPageNumber());
                keyBuilder.append(":s").append(pageable.getPageSize());
            }
        }

        return keyBuilder.toString();
    }

    /**
     * 인기 검색어 조회
     */
    public List<Map.Entry<String, Integer>> getPopularKeywords(int limit) {
        return popularKeywords.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                        (a, b) -> b.get() - a.get()))
                .limit(limit)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
    }

    /**
     * 검색 캐시 정리 (스케줄링)
     */
    @Scheduled(fixedDelay = 300000) // 5분마다
    public void cleanupExpiredCache() {
        int beforeSize = searchCache.size();

        searchCache.entrySet().removeIf(entry ->
                entry.getValue().isExpired(300000) // 5분 후 만료
        );

        int removed = beforeSize - searchCache.size();
        if (removed > 0) {
            log.info("SEARCH_CACHE_CLEANUP - Removed: {}, Remaining: {}",
                    removed, searchCache.size());
        }
    }

    /**
     * 인기 검색어 로깅
     */
    private void logPopularKeywords() {
        List<Map.Entry<String, Integer>> topKeywords = getPopularKeywords(10);
        log.info("POPULAR_KEYWORDS - Top 10: {}", topKeywords);
    }

    /**
     * 전체 검색 횟수
     */
    private int getTotalSearchCount() {
        return popularKeywords.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
    }

    // 헬퍼 메서드들
    private String extractKeyword(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return null;
    }

    private void updateKeywordInArgs(Object[] args, String newKeyword) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                args[i] = newKeyword;
                break;
            }
        }
    }

    private String extractUserEmail() {
        // SecurityContext에서 사용자 정보 추출
        return "anonymous"; // 실제 구현 필요
    }

    private LocalDateTime LocalDateTime.now() {
        return java.time.LocalDateTime.now();
    }
}