package com.fream.back.global.exception.websocket;

import com.fream.back.global.exception.GlobalException;

/**
 * 웹소켓 관련 예외의 기본 클래스
 * 모든 웹소켓 관련 예외는 이 클래스를 상속받음
 */
public abstract class WebSocketException extends GlobalException {
    /**
     * WebSocketErrorCode로 예외 생성
     *
     * @param errorCode 웹소켓 관련 에러 코드
     */
    public WebSocketException(WebSocketErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * WebSocketErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 웹소켓 관련 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public WebSocketException(WebSocketErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * WebSocketErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 웹소켓 관련 에러 코드
     * @param cause 원인이 되는 예외
     */
    public WebSocketException(WebSocketErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}