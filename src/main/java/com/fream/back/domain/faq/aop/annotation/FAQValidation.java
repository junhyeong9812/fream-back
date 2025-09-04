package com.fream.back.domain.faq.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FAQ 유효성 검증 어노테이션
 * 입력 데이터 검증, 파일 검증, 권한 확인 등
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FAQValidation {

    /**
     * 검증 활성화
     */
    boolean enabled() default true;

    /**
     * 검증 타입들
     */
    ValidationType[] validations() default {
            ValidationType.CONTENT,
            ValidationType.FILE,
            ValidationType.CATEGORY
    };

    /**
     * 질문 최소 길이
     */
    int minQuestionLength() default 5;

    /**
     * 질문 최대 길이
     */
    int maxQuestionLength() default 100;

    /**
     * 답변 최소 길이
     */
    int minAnswerLength() default 10;

    /**
     * 답변 최대 길이
     */
    int maxAnswerLength() default 50000;

    /**
     * 최대 파일 크기 (바이트)
     */
    long maxFileSize() default 5242880; // 5MB

    /**
     * 최대 파일 개수
     */
    int maxFileCount() default 10;

    /**
     * 전체 파일 최대 크기 (바이트)
     */
    long maxTotalFileSize() default 52428800; // 50MB

    /**
     * 허용된 파일 확장자
     */
    String[] allowedExtensions() default {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 금지어 검사
     */
    boolean checkProhibitedWords() default true;

    /**
     * 중복 검사
     */
    boolean checkDuplicate() default false;

    /**
     * HTML 인젝션 검사
     */
    boolean checkHtmlInjection() default true;

    /**
     * SQL 인젝션 검사
     */
    boolean checkSqlInjection() default true;

    /**
     * 관리자 권한 필요
     */
    boolean requireAdmin() default false;

    /**
     * 검증 실패 시 처리
     */
    ValidationFailureAction onFailure() default ValidationFailureAction.THROW_EXCEPTION;

    /**
     * 커스텀 에러 메시지
     */
    String customErrorMessage() default "";

    /**
     * 검증 타입
     */
    enum ValidationType {
        CONTENT,            // 컨텐츠 검증
        FILE,              // 파일 검증
        CATEGORY,          // 카테고리 검증
        LENGTH,            // 길이 검증
        PROHIBITED_WORDS,  // 금지어 검증
        INJECTION,         // 인젝션 검증
        DUPLICATE,         // 중복 검증
        PERMISSION         // 권한 검증
    }

    /**
     * 검증 실패 처리 방식
     */
    enum ValidationFailureAction {
        THROW_EXCEPTION,    // 예외 발생
        LOG_AND_CONTINUE,   // 로그 후 계속
        RETURN_DEFAULT,     // 기본값 반환
        SANITIZE,          // 데이터 정제
        REQUEST_CORRECTION  // 수정 요청
    }

    /**
     * 검증 우선순위
     */
    int priority() default 0;

    /**
     * 비동기 검증
     */
    boolean async() default false;
}