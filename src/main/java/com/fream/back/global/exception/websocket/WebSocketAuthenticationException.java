package com.fream.back.global.exception.websocket;
/**
 * 웹소켓 인증 예외
 * 웹소켓 연결 시 인증에 실패한 경우 발생
 */
public class WebSocketAuthenticationException extends WebSocketException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "웹소켓 인증에 실패했습니다."
     */
    public WebSocketAuthenticationException() {
        super(WebSocketErrorCode.AUTHENTICATION_ERROR);
    }

    /**
     * 사용자 정의 메시지로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WebSocketAuthenticationException(String message) {
        super(WebSocketErrorCode.AUTHENTICATION_ERROR, message);
    }
}
