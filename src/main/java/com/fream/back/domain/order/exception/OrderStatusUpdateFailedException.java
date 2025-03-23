package com.fream.back.domain.order.exception;

/**
 * 주문 상태 변경 시 발생하는 예외
 */
public class OrderStatusUpdateFailedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderStatusUpdateFailedException() {
        super(OrderErrorCode.ORDER_STATUS_UPDATE_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderStatusUpdateFailedException(String message) {
        super(OrderErrorCode.ORDER_STATUS_UPDATE_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderStatusUpdateFailedException(Throwable cause) {
        super(OrderErrorCode.ORDER_STATUS_UPDATE_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderStatusUpdateFailedException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_STATUS_UPDATE_FAILED, message, cause);
    }
}