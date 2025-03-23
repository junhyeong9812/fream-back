package com.fream.back.domain.order.exception;

/**
 * 주문 입찰 생성 시 발생하는 예외
 */
public class OrderBidCreationFailedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderBidCreationFailedException() {
        super(OrderErrorCode.ORDER_BID_CREATION_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderBidCreationFailedException(String message) {
        super(OrderErrorCode.ORDER_BID_CREATION_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderBidCreationFailedException(Throwable cause) {
        super(OrderErrorCode.ORDER_BID_CREATION_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderBidCreationFailedException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_BID_CREATION_FAILED, message, cause);
    }
}
