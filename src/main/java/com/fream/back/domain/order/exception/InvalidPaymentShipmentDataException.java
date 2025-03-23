package com.fream.back.domain.order.exception;

/**
 * 결제 및 배송 정보가 유효하지 않을 때 발생하는 예외
 */
public class InvalidPaymentShipmentDataException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InvalidPaymentShipmentDataException() {
        super(OrderErrorCode.INVALID_PAYMENT_SHIPMENT_DATA);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidPaymentShipmentDataException(String message) {
        super(OrderErrorCode.INVALID_PAYMENT_SHIPMENT_DATA, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InvalidPaymentShipmentDataException(Throwable cause) {
        super(OrderErrorCode.INVALID_PAYMENT_SHIPMENT_DATA, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InvalidPaymentShipmentDataException(String message, Throwable cause) {
        super(OrderErrorCode.INVALID_PAYMENT_SHIPMENT_DATA, message, cause);
    }
}