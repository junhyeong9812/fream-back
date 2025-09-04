package com.fream.back.domain.faq.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FAQ 검색 최적화 어노테이션
 * 검색어 정규화, 동의어 처리, 검색 결과 캐싱 등
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FAQSearchOptimization {

    /**
     * 검색 최적화 활성화
     */
    boolean enabled() default true;

    /**
     * 검색어 정규화
     */
    boolean normalizeKeyword() default true;

    /**
     * 한글 정규화 (자모 분리 등)
     */
    boolean normalizeKorean() default true;

    /**
     * 특수문자 제거
     */
    boolean removeSpecialChars() default true;

    /**
     * 동의어 확장 사용
     */
    boolean enableSynonymExpansion() default true;

    /**
     * 초성 검색 지원
     */
    boolean supportChosungSearch() default false;

    /**
     * 검색 결과 캐싱
     */
    boolean enableCaching() default true;

    /**
     * 캐시 TTL (초)
     */
    int cacheTTLSeconds() default 300; // 5분

    /**
     * 검색 로그 저장
     */
    boolean logSearchQueries() default true;

    /**
     * 인기 검색어 추적
     */
    boolean trackPopularKeywords() default true;

    /**
     * 검색 결과 없을 시 추천
     */
    boolean suggestOnNoResult() default true;

    /**
     * 추천 결과 최대 개수
     */
    int maxSuggestions() default 5;

    /**
     * 자동 완성 지원
     */
    boolean enableAutocomplete() default false;

    /**
     * 오타 교정
     */
    boolean enableTypoCorrection() default false;

    /**
     * 검색 점수 임계값
     */
    double scoreThreshold() default 0.5;

    /**
     * 부분 일치 허용
     */
    boolean allowPartialMatch() default true;

    /**
     * 검색 전략
     */
    SearchStrategy strategy() default SearchStrategy.BALANCED;

    /**
     * 검색 필드
     */
    SearchField[] searchFields() default {SearchField.QUESTION, SearchField.ANSWER};

    /**
     * 검색 전략
     */
    enum SearchStrategy {
        EXACT,          // 정확한 일치
        FUZZY,          // 퍼지 매칭
        BALANCED,       // 균형잡힌 검색
        COMPREHENSIVE   // 포괄적 검색
    }

    /**
     * 검색 대상 필드
     */
    enum SearchField {
        QUESTION,       // 질문
        ANSWER,         // 답변
        CATEGORY,       // 카테고리
        ALL            // 전체
    }

    /**
     * 검색 결과 정렬
     */
    SortBy sortBy() default SortBy.RELEVANCE;

    /**
     * 정렬 기준
     */
    enum SortBy {
        RELEVANCE,      // 관련성
        POPULARITY,     // 인기도
        RECENT,         // 최신순
        ALPHABETICAL    // 알파벳순
    }
}