package com.fream.back.domain.order.exception;

/**
 * 주문 입찰이 이미 매칭되어 있을 때 발생하는 예외
 */
public class OrderBidAlreadyMatchedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderBidAlreadyMatchedException() {
        super(OrderErrorCode.ORDER_BID_ALREADY_MATCHED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderBidAlreadyMatchedException(String message) {
        super(OrderErrorCode.ORDER_BID_ALREADY_MATCHED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderBidAlreadyMatchedException(Throwable cause) {
        super(OrderErrorCode.ORDER_BID_ALREADY_MATCHED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderBidAlreadyMatchedException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_BID_ALREADY_MATCHED, message, cause);
    }
}