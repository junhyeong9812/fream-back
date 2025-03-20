package com.fream.back.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 전체에서 공통으로 사용되는 에러 코드
 * 접두사 'G'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
    // 서버 에러
    /**
     * 서버 내부 오류 (500)
     * 서버에서 처리 중 예상치 못한 오류가 발생한 경우
     */
    INTERNAL_SERVER_ERROR("G001", "서버 내부 오류가 발생했습니다.", 500),

    // 입력값 검증 에러
    /**
     * 잘못된 입력값 (400)
     * 요청 파라미터나 요청 바디의 값이 유효하지 않은 경우
     */
    INVALID_INPUT_VALUE("G002", "유효하지 않은 입력값입니다.", 400),

    /**
     * 잘못된 타입 (400)
     * 요청 파라미터의 타입이 예상과 다른 경우 (예: 문자열 대신 숫자)
     */
    INVALID_TYPE_VALUE("G003", "유효하지 않은 타입입니다.", 400),

    // 리소스 접근 에러
    /**
     * 리소스 찾을 수 없음 (404)
     * 요청한 리소스(데이터, 페이지 등)가 존재하지 않는 경우
     */
    RESOURCE_NOT_FOUND("G004", "요청한 리소스를 찾을 수 없습니다.", 404),

    /**
     * 지원하지 않는 HTTP 메소드 (405)
     * 지원하지 않는 HTTP 메소드로 요청한 경우 (예: GET 대신 POST)
     */
    METHOD_NOT_ALLOWED("G005", "지원하지 않는 HTTP 메소드입니다.", 405),

    // 인증/인가 에러
    /**
     * 인증 필요 (401)
     * 인증되지 않은 사용자가 보호된 리소스에 접근 시도한 경우
     */
    UNAUTHORIZED("G006", "인증이 필요합니다.", 401),

    /**
     * 접근 권한 없음 (403)
     * 인증된 사용자지만 해당 리소스에 접근 권한이 없는 경우
     */
    ACCESS_DENIED("G007", "접근 권한이 없습니다.", 403),

    /**
     * 유효하지 않은 토큰 (401)
     * JWT 토큰이 유효하지 않은 경우 (서명 오류 등)
     */
    INVALID_TOKEN("G008", "유효하지 않은 토큰입니다.", 401),

    /**
     * 만료된 토큰 (401)
     * JWT 토큰이 만료된 경우
     */
    EXPIRED_TOKEN("G009", "만료된 토큰입니다.", 401),

    // 보안 관련 에러
    /**
     * 보안 컨텍스트 찾을 수 없음 (401)
     * SecurityContextHolder에서 Authentication을 찾을 수 없는 경우
     */
    SECURITY_CONTEXT_NOT_FOUND("G010", "보안 컨텍스트를 찾을 수 없습니다.", 401),

    // 파일 관련 에러
    /**
     * 파일 업로드 오류 (500)
     * 파일 업로드 중 오류가 발생한 경우
     */
    FILE_UPLOAD_ERROR("G020", "파일 업로드 중 오류가 발생했습니다.", 500),

    /**
     * 파일 다운로드 오류 (500)
     * 파일 다운로드 중 오류가 발생한 경우
     */
    FILE_DOWNLOAD_ERROR("G021", "파일 다운로드 중 오류가 발생했습니다.", 500),

    /**
     * 지원되지 않는 파일 형식 (400)
     * 허용되지 않는 파일 형식을 업로드한 경우
     */
    UNSUPPORTED_FILE_TYPE("G022", "지원되지 않는 파일 형식입니다.", 400),

    /**
     * 파일 찾을 수 없음 (404)
     * 요청한 파일이 서버에 존재하지 않는 경우
     */
    FILE_NOT_FOUND("G023", "파일을 찾을 수 없습니다.", 404),

    // 웹소켓 관련 에러
    /**
     * 웹소켓 연결 오류 (500)
     * 웹소켓 연결 중 오류가 발생한 경우
     */
    WEBSOCKET_CONNECTION_ERROR("G030", "웹소켓 연결 중 오류가 발생했습니다.", 500),

    /**
     * 웹소켓 인증 오류 (401)
     * 웹소켓 연결 시 인증에 실패한 경우
     */
    WEBSOCKET_AUTHENTICATION_ERROR("G031", "웹소켓 인증에 실패했습니다.", 401);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}