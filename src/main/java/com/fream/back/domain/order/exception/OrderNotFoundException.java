package com.fream.back.domain.order.exception;

/**
 * 주문을 찾을 수 없을 때 발생하는 예외
 */
public class OrderNotFoundException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderNotFoundException() {
        super(OrderErrorCode.ORDER_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderNotFoundException(String message) {
        super(OrderErrorCode.ORDER_NOT_FOUND, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderNotFoundException(Throwable cause) {
        super(OrderErrorCode.ORDER_NOT_FOUND, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderNotFoundException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_NOT_FOUND, message, cause);
    }
}