package com.fream.back.domain.accessLog.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 접근 로그 도메인에서 사용하는 에러 코드
 * 접두사 'AL'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum AccessLogErrorCode implements ErrorCode {

    // API 관련 에러
    /**
     * 접근 로그 저장 실패 (500)
     * 접근 로그 데이터를 저장하는 과정에서 발생하는 오류
     */
    ACCESS_LOG_SAVE_ERROR("AL001", "접근 로그 저장 중 오류가 발생했습니다.", 500),

    /**
     * Kafka 전송 실패 (500)
     * 접근 로그를 Kafka로 전송하는 과정에서 발생하는 오류
     */
    KAFKA_SEND_ERROR("AL002", "로그 데이터 전송 중 오류가 발생했습니다.", 500),

    /**
     * Kafka 수신 실패 (500)
     * Kafka로부터 접근 로그를 수신하는 과정에서 발생하는 오류
     */
    KAFKA_RECEIVE_ERROR("AL003", "로그 데이터 수신 중 오류가 발생했습니다.", 500),

    // 조회 관련 에러
    /**
     * 접근 로그 조회 실패 (500)
     * 접근 로그 데이터를 조회하는 과정에서 발생하는 오류
     */
    ACCESS_LOG_QUERY_ERROR("AL101", "접근 로그 조회 중 오류가 발생했습니다.", 500),

    /**
     * 통계 데이터 조회 실패 (500)
     * 접근 로그 통계 데이터를 조회하는 과정에서 발생하는 오류
     */
    STATISTICS_QUERY_ERROR("AL102", "접근 통계 조회 중 오류가 발생했습니다.", 500),

    // 지리정보 관련 에러
    /**
     * IP 위치정보 조회 실패 (500)
     * IP 주소로부터 위치 정보를 조회하는 과정에서 발생하는 오류
     */
    GEO_IP_LOOKUP_ERROR("AL201", "IP 위치정보 조회 중 오류가 발생했습니다.", 500),

    /**
     * GeoIP 데이터베이스 오류 (500)
     * GeoIP 데이터베이스 초기화 또는 사용 중 발생하는 오류
     */
    GEO_IP_DATABASE_ERROR("AL202", "위치정보 데이터베이스 오류가 발생했습니다.", 500),

    // 파라미터 관련 에러
    /**
     * 유효하지 않은 접근 로그 데이터 (400)
     * 접근 로그 데이터가 유효하지 않은 경우
     */
    INVALID_ACCESS_LOG_DATA("AL301", "유효하지 않은 접근 로그 데이터입니다.", 400),

    /**
     * 유효하지 않은 IP 주소 (400)
     * IP 주소 형식이 올바르지 않은 경우
     */
    INVALID_IP_ADDRESS("AL302", "유효하지 않은 IP 주소입니다.", 400),

    /**
     * 날짜 범위 오류 (400)
     * 통계 조회 시 유효하지 않은 날짜 범위가 지정된 경우
     */
    INVALID_DATE_RANGE("AL303", "유효하지 않은 날짜 범위입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}