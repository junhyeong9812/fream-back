package com.fream.back.domain.order.exception;

/**
 * 주문 입찰 상태가 유효하지 않을 때 발생하는 예외
 */
public class InvalidOrderBidStatusException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InvalidOrderBidStatusException() {
        super(OrderErrorCode.INVALID_ORDER_BID_STATUS);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidOrderBidStatusException(String message) {
        super(OrderErrorCode.INVALID_ORDER_BID_STATUS, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InvalidOrderBidStatusException(Throwable cause) {
        super(OrderErrorCode.INVALID_ORDER_BID_STATUS, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InvalidOrderBidStatusException(String message, Throwable cause) {
        super(OrderErrorCode.INVALID_ORDER_BID_STATUS, message, cause);
    }
}