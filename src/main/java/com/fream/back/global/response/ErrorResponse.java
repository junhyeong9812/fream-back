package com.fream.back.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 에러 응답을 위한 DTO 클래스
 * 클라이언트에게 일관된 형식의 에러 응답을 제공합니다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    /**
     * 에러 코드 (예: "G001", "S001")
     */
    private final String code;

    /**
     * 에러 메시지
     */
    private final String message;

    /**
     * HTTP 상태 코드 (400, 401, 404, 500 등)
     */
    private final int status;

    /**
     * 에러 발생 시간
     */
    private final LocalDateTime timestamp;

    /**
     * 에러가 발생한 요청 경로
     */
    private final String path;

    /**
     * 에러 응답 객체 생성 메소드
     *
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param status HTTP 상태 코드
     * @param path 요청 경로
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(String code, String message, int status, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}