package com.fream.back.domain.order.exception;

/**
 * 주문에 대한 접근 권한이 없을 때 발생하는 예외
 */
public class OrderAccessDeniedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderAccessDeniedException() {
        super(OrderErrorCode.ORDER_ACCESS_DENIED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderAccessDeniedException(String message) {
        super(OrderErrorCode.ORDER_ACCESS_DENIED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderAccessDeniedException(Throwable cause) {
        super(OrderErrorCode.ORDER_ACCESS_DENIED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderAccessDeniedException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_ACCESS_DENIED, message, cause);
    }
}