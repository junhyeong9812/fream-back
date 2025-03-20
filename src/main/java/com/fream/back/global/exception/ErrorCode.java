package com.fream.back.global.exception;

/**
 * 모든 에러 코드 Enum이 구현해야 하는 인터페이스
 * 일관된 에러 코드 관리를 위해 사용됩니다.
 */
public interface ErrorCode {
    /**
     * 에러 코드 문자열 반환 (예: "G001")
     * @return 에러 코드 문자열
     */
    String getCode();

    /**
     * 에러 메시지 반환 (예: "서버 내부 오류가 발생했습니다.")
     * @return 에러 메시지
     */
    String getMessage();

    /**
     * HTTP 상태 코드 반환 (예: 500)
     * @return HTTP 상태 코드
     */
    int getStatus();
}