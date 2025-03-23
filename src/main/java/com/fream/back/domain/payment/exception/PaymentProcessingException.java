package com.fream.back.domain.payment.exception;

/**
 * 결제 처리 중 오류가 발생했을 때 발생하는 예외
 */
public class PaymentProcessingException extends PaymentException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public PaymentProcessingException() {
        super(PaymentErrorCode.PAYMENT_PROCESSING_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public PaymentProcessingException(String message) {
        super(PaymentErrorCode.PAYMENT_PROCESSING_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public PaymentProcessingException(Throwable cause) {
        super(PaymentErrorCode.PAYMENT_PROCESSING_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public PaymentProcessingException(String message, Throwable cause) {
        super(PaymentErrorCode.PAYMENT_PROCESSING_FAILED, message, cause);
    }
}