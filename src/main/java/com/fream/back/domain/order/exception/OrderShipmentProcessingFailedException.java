package com.fream.back.domain.order.exception;

/**
 * 주문 배송 처리 시 발생하는 예외
 */
public class OrderShipmentProcessingFailedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderShipmentProcessingFailedException() {
        super(OrderErrorCode.ORDER_SHIPMENT_PROCESSING_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderShipmentProcessingFailedException(String message) {
        super(OrderErrorCode.ORDER_SHIPMENT_PROCESSING_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderShipmentProcessingFailedException(Throwable cause) {
        super(OrderErrorCode.ORDER_SHIPMENT_PROCESSING_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderShipmentProcessingFailedException(String message, Throwable cause) {
        super(OrderErrorCode.ORDER_SHIPMENT_PROCESSING_FAILED, message, cause);
    }
}