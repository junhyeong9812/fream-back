package com.fream.back.global.exception.websocket;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 웹소켓 관련 에러 코드
 * 접두사 'W'로 시작하는 코드를 사용
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketErrorCode implements ErrorCode {
    /**
     * 웹소켓 연결 오류 (500)
     * 웹소켓 연결 중 오류가 발생한 경우
     */
    CONNECTION_ERROR("W001", "웹소켓 연결 중 오류가 발생했습니다.", 500),

    /**
     * 웹소켓 인증 오류 (401)
     * 웹소켓 연결 시 인증에 실패한 경우
     */
    AUTHENTICATION_ERROR("W002", "웹소켓 인증에 실패했습니다.", 401),

    /**
     * 메시지 전송 오류 (500)
     * 웹소켓을 통한 메시지 전송 중 오류가 발생한 경우
     */
    MESSAGE_SEND_ERROR("W003", "메시지 전송 중 오류가 발생했습니다.", 500),

    /**
     * 유효하지 않은 대상 (400)
     * 존재하지 않는 목적지나 사용자에게 메시지를 전송하려는 경우
     */
    INVALID_DESTINATION("W004", "유효하지 않은 대상입니다.", 400),

    /**
     * 세션 찾을 수 없음 (404)
     * 특정 세션을 찾을 수 없는 경우
     */
    SESSION_NOT_FOUND("W005", "세션을 찾을 수 없습니다.", 404);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}