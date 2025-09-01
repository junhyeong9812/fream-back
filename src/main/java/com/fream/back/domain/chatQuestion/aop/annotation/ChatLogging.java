package com.fream.back.domain.chatQuestion.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ChatQuestion 도메인 로깅 어노테이션
 * GPT API 호출, 채팅 질문 처리 등의 로깅을 세밀하게 제어
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatLogging {

    /**
     * 로깅 레벨 설정
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 로깅할 정보 타입들
     */
    LogType[] types() default {LogType.EXECUTION_TIME, LogType.USER_INFO, LogType.GPT_USAGE};

    /**
     * 메서드 실행 전 로깅 여부
     */
    boolean logBefore() default true;

    /**
     * 메서드 실행 후 로깅 여부
     */
    boolean logAfter() default true;

    /**
     * 예외 발생 시 로깅 여부
     */
    boolean logException() default true;

    /**
     * 질문 내용 마스킹 여부 (개인정보 보호)
     */
    boolean maskQuestionContent() default true;

    /**
     * GPT 응답 내용 로깅 여부
     */
    boolean logGPTResponse() default false;

    /**
     * 토큰 사용량 로깅 여부
     */
    boolean logTokenUsage() default true;

    /**
     * 커스텀 로그 메시지 (비어있으면 기본 메시지 사용)
     */
    String message() default "";

    /**
     * GPT API 호출 추적 여부
     */
    boolean trackGPTApiCall() default false;

    /**
     * 사용자 세션 추적 여부
     */
    boolean trackUserSession() default false;

    /**
     * 로깅 레벨 열거형
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /**
     * 로깅 타입 열거형
     */
    enum LogType {
        EXECUTION_TIME,    // 실행 시간
        PARAMETERS,        // 메서드 파라미터
        RESULT,           // 반환값
        USER_INFO,        // 사용자 정보
        REQUEST_ID,       // 요청 ID
        GPT_USAGE,        // GPT 토큰 사용량
        QUESTION_SUMMARY, // 질문 요약
        RESPONSE_SUMMARY  // 응답 요약
    }
}