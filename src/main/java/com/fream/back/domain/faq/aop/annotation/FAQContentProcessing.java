package com.fream.back.domain.faq.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FAQ 컨텐츠 처리 어노테이션
 * HTML 컨텐츠 정제, 이미지 최적화, XSS 방지 등
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FAQContentProcessing {

    /**
     * HTML 컨텐츠 정제 활성화
     */
    boolean sanitizeHtml() default true;

    /**
     * XSS 방지
     */
    boolean preventXSS() default true;

    /**
     * 이미지 최적화 활성화
     */
    boolean optimizeImages() default true;

    /**
     * 이미지 최대 너비 (픽셀)
     */
    int maxImageWidth() default 1200;

    /**
     * 이미지 품질 (1-100)
     */
    float imageQuality() default 0.85f;

    /**
     * 이미지 태그 최적화
     */
    boolean optimizeImageTags() default true;

    /**
     * 이미지 lazy loading 활성화
     */
    boolean enableLazyLoading() default true;

    /**
     * 링크 검증 및 수정
     */
    boolean validateLinks() default true;

    /**
     * 외부 링크 새 창에서 열기
     */
    boolean externalLinksInNewWindow() default true;

    /**
     * HTML 태그 화이트리스트 사용
     */
    boolean useWhitelist() default true;

    /**
     * 허용된 HTML 태그들
     */
    String[] allowedTags() default {
            "p", "div", "span", "br", "hr",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "strong", "em", "u", "s", "mark",
            "ul", "ol", "li", "dl", "dt", "dd",
            "table", "thead", "tbody", "tr", "th", "td",
            "img", "a", "blockquote", "pre", "code"
    };

    /**
     * 금지된 HTML 속성들
     */
    String[] prohibitedAttributes() default {
            "onclick", "onload", "onerror", "onmouseover",
            "javascript:", "data:text/html"
    };

    /**
     * 출력 데이터 정제
     */
    boolean sanitizeOutput() default true;

    /**
     * 컨텐츠 압축 (긴 텍스트)
     */
    boolean compressContent() default false;

    /**
     * 컨텐츠 최대 길이
     */
    int maxContentLength() default 50000;

    /**
     * 자동 요약 생성
     */
    boolean generateSummary() default false;

    /**
     * 요약 최대 길이
     */
    int summaryMaxLength() default 200;

    /**
     * 메타데이터 추출
     */
    boolean extractMetadata() default false;

    /**
     * 처리 실패 시 동작
     */
    FailureAction onFailure() default FailureAction.LOG_AND_CONTINUE;

    /**
     * 실패 처리 방식
     */
    enum FailureAction {
        THROW_EXCEPTION,    // 예외 발생
        LOG_AND_CONTINUE,   // 로그 후 계속
        USE_ORIGINAL,       // 원본 사용
        RETURN_EMPTY        // 빈 값 반환
    }
}