package com.fream.back.domain.order.exception;

/**
 * 주문 입찰 매칭 과정에서 발생하는 예외
 */
public class OrderBidMatchingFailedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderBidMatchingFailedException() {
        super(OrderErrorCode.ORDER_BID_MATCHING_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderBidMatchingFailedException(String message) {
        super(OrderErrorCode.ORDER_BID_MATCHING_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderBidMatchingFailedException(Throwable cause) {
        super(OrderErrorCode.ORDER_BID_MATCHING_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderBidMatchingFailedException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_BID_MATCHING_FAILED, message, cause);
    }
}