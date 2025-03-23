package com.fream.back.domain.order.exception;

/**
 * 주문 입찰 정보가 유효하지 않을 때 발생하는 예외
 */
public class InvalidOrderBidException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InvalidOrderBidException() {
        super(OrderErrorCode.INVALID_ORDER_BID_DATA);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidOrderBidException(String message) {
        super(OrderErrorCode.INVALID_ORDER_BID_DATA, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InvalidOrderBidException(Throwable cause) {
        super(OrderErrorCode.INVALID_ORDER_BID_DATA, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InvalidOrderBidException(String message, Throwable cause) {
        super(OrderErrorCode.INVALID_ORDER_BID_DATA, message, cause);
    }
}