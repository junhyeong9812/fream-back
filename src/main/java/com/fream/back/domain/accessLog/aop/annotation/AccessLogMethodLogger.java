package com.fream.back.domain.accessLog.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 전후 로깅을 위한 AOP 어노테이션
 * 메서드 진입/종료 시점에 로그를 자동으로 남김
 *
 * @Before, @AfterReturning, @AfterThrowing 어드바이스와 연동하여
 * 메서드의 전체 실행 과정을 추적할 수 있습니다.
 *
 * 사용 예시:
 * @AccessLogMethodLogger(
 *     level = LogLevel.INFO,
 *     logParameters = true,
 *     logReturnValue = false,
 *     customMessage = "사용자 접근 로그 처리"
 * )
 * public void processAccessLog() { ... }
 */
@Target(ElementType.METHOD) // 메서드에만 적용
@Retention(RetentionPolicy.RUNTIME) // 런타임 유지 (AOP에서 접근 필요)
public @interface AccessLogMethodLogger {

    /**
     * 로그 레벨 설정
     * 메서드의 중요도에 따라 적절한 레벨 선택
     *
     * TRACE: 가장 상세한 로깅 (개발 단계)
     * DEBUG: 디버깅용 정보 (개발/테스트 환경)
     * INFO: 일반적인 정보 (운영 환경의 주요 흐름)
     * WARN: 경고성 정보 (주의가 필요한 상황)
     * ERROR: 오류 정보 (예외 상황)
     *
     * @return 로그 레벨 (기본값: INFO)
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 메서드 파라미터 로깅 여부
     * 개인정보나 민감한 데이터가 포함된 경우 false로 설정
     *
     * true: 메서드 호출 시 전달된 모든 파라미터를 로깅
     * false: 파라미터 정보 제외하고 로깅
     *
     * @return 파라미터 로깅 여부 (기본값: true)
     */
    boolean logParameters() default true;

    /**
     * 메서드 반환값 로깅 여부
     * 반환값이 크거나 민감한 정보인 경우 false로 설정
     *
     * true: 메서드 정상 종료 시 반환값을 로깅 (200자 제한)
     * false: 반환값 정보 제외하고 로깅
     *
     * @return 반환값 로깅 여부 (기본값: false)
     */
    boolean logReturnValue() default false;

    /**
     * 실행 시간 측정 여부
     * 성능 분석이 필요한 메서드에서 true로 설정
     *
     * true: 메서드 실행 시간을 측정하여 로깅
     * false: 실행 시간 측정하지 않음
     *
     * @return 실행 시간 측정 여부 (기본값: true)
     */
    boolean measureExecutionTime() default true;

    /**
     * 커스텀 로그 메시지
     * 메서드의 역할을 명확히 설명하는 메시지
     *
     * 빈 문자열인 경우: "[메서드명] 메서드 실행 시작/완료" 형태의 기본 메시지 사용
     * 값이 있는 경우: 지정된 커스텀 메시지 사용
     *
     * @return 커스텀 메시지 (기본값: 빈 문자열)
     */
    String customMessage() default "";

    /**
     * 로그 레벨 열거형
     * SLF4J 로깅 프레임워크의 표준 레벨과 일치
     */
    enum LogLevel {
        /**
         * 오류 레벨 - 시스템 오류나 예외 상황
         * 일반적으로 예외 발생 시에만 사용
         */
        ERROR,

        /**
         * 경고 레벨 - 주의가 필요한 상황
         * 성능 저하, 비정상적인 데이터 등
         */
        WARN,

        /**
         * 정보 레벨 - 애플리케이션의 주요 흐름
         * 비즈니스 로직의 중요한 단계, 외부 시스템 호출 등
         */
        INFO,

        /**
         * 디버그 레벨 - 개발 시 상세 정보
         * 메서드 내부 로직, 조건 분기 등
         */
        DEBUG,

        /**
         * 추적 레벨 - 가장 상세한 정보
         * 루프 내부, 세부 계산 과정 등
         */
        TRACE
    }
}