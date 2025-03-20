package com.fream.back.global.exception.websocket;
/**
 * 웹소켓 메시지 전송 예외
 * 웹소켓을 통한 메시지 전송 중 오류가 발생한 경우 발생
 */
public class WebSocketMessageSendException extends WebSocketException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "메시지 전송 중 오류가 발생했습니다."
     */
    public WebSocketMessageSendException() {
        super(WebSocketErrorCode.MESSAGE_SEND_ERROR);
    }

    /**
     * 원인 예외로 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WebSocketMessageSendException(Throwable cause) {
        super(WebSocketErrorCode.MESSAGE_SEND_ERROR, cause);
    }
}
