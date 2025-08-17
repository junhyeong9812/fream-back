package com.fream.back.domain.accessLog.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 접근 로그 도메인의 예외 처리를 위한 AOP 어노테이션
 * 메서드에 적용하면 해당 메서드의 예외를 자동으로 AccessLog 도메인 예외로 변환
 *
 * 사용 예시:
 * @AccessLogExceptionHandler(
 *     defaultType = ExceptionType.SAVE,
 *     message = "사용자 정의 에러 메시지",
 *     retry = true,
 *     retryCount = 3
 * )
 * public void someMethod() { ... }
 */
@Target(ElementType.METHOD) // 메서드에만 적용 가능하도록 제한
@Retention(RetentionPolicy.RUNTIME) // 런타임에 어노테이션 정보 유지 (리플렉션 접근 가능)
public @interface AccessLogExceptionHandler {

    /**
     * 기본 예외 처리 타입 지정
     * 발생한 예외를 어떤 도메인 예외로 변환할지 결정
     *
     * @return 예외 타입 (기본값: GENERAL)
     */
    ExceptionType defaultType() default ExceptionType.GENERAL;

    /**
     * 사용자 정의 에러 메시지
     * 빈 문자열인 경우 원본 예외 메시지 또는 기본 메시지 사용
     *
     * @return 커스텀 에러 메시지 (기본값: 빈 문자열)
     */
    String message() default "";

    /**
     * 로그 레벨 설정 - 예외 발생 시 어떤 레벨로 로깅할지 결정
     * ERROR: 심각한 시스템 오류
     * WARN: 경고성 오류 (서비스 계속 가능)
     * INFO: 정보성 로깅
     * DEBUG: 디버깅용 상세 정보
     *
     * @return 로그 레벨 (기본값: ERROR)
     */
    LogLevel logLevel() default LogLevel.ERROR;

    /**
     * 예외 발생 시 재시도 여부
     * 네트워크 오류, 일시적 DB 장애 등에서 유용
     *
     * @return 재시도 여부 (기본값: false)
     */
    boolean retry() default false;

    /**
     * 재시도 횟수 (retry가 true일 때만 사용)
     * 지수 백오프(exponential backoff) 방식으로 재시도 간격 증가
     *
     * @return 재시도 횟수 (기본값: 3)
     */
    int retryCount() default 3;

    /**
     * 예외 처리 타입 열거형
     * 각 타입별로 다른 도메인 예외 클래스로 변환됨
     */
    enum ExceptionType {
        /**
         * 일반적인 AccessLogException으로 변환
         * 특별한 분류가 필요 없는 일반적인 예외
         */
        GENERAL,

        /**
         * 저장 관련 예외 (AccessLogSaveException)
         * 데이터베이스 저장, 파일 저장 등의 쓰기 작업 실패
         */
        SAVE,

        /**
         * 조회 관련 예외 (AccessLogQueryException)
         * 데이터베이스 조회, 검색 등의 읽기 작업 실패
         */
        QUERY,

        /**
         * Kafka 관련 예외 (AccessLogKafkaException)
         * 메시지 큐 송수신, 브로커 연결 등의 메시징 시스템 오류
         */
        KAFKA,

        /**
         * GeoIP 관련 예외 (GeoIPException)
         * IP 위치 조회, GeoIP 데이터베이스 접근 등의 지리정보 서비스 오류
         */
        GEO_IP,

        /**
         * 파라미터 검증 관련 예외 (InvalidParameterException)
         * 입력값 검증, 데이터 형식 오류 등의 유효성 검사 실패
         */
        VALIDATION
    }

    /**
     * 로그 레벨 열거형
     * 예외 발생 시 로깅 레벨 결정
     */
    enum LogLevel {
        /**
         * 심각한 오류 - 시스템 장애, 데이터 손실 등
         * 즉시 대응이 필요한 오류
         */
        ERROR,

        /**
         * 경고 수준 - 시스템은 계속 동작하지만 주의가 필요
         * 성능 저하, 임시적 오류 등
         */
        WARN,

        /**
         * 정보성 메시지 - 정상적인 동작 과정의 중요 정보
         * 비즈니스 로직의 주요 단계 등
         */
        INFO,

        /**
         * 디버그용 상세 정보 - 개발/테스트 환경에서 문제 추적용
         * 상세한 실행 흐름, 변수 값 등
         */
        DEBUG
    }
}