package com.fream.back.global.exception.websocket;

/**
 * 웹소켓 연결 예외
 * 웹소켓 연결 중 오류가 발생한 경우 발생
 */
public class WebSocketConnectionException extends WebSocketException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "웹소켓 연결 중 오류가 발생했습니다."
     */
    public WebSocketConnectionException() {
        super(WebSocketErrorCode.CONNECTION_ERROR);
    }

    /**
     * 원인 예외로 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WebSocketConnectionException(Throwable cause) {
        super(WebSocketErrorCode.CONNECTION_ERROR, cause);
    }
}
