package com.fream.back.domain.payment.exception;

/**
 * 외부 결제 API 연동 중 오류가 발생했을 때 발생하는 예외
 */
public class PaymentApiException extends PaymentException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public PaymentApiException() {
        super(PaymentErrorCode.PAYMENT_API_ERROR);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public PaymentApiException(String message) {
        super(PaymentErrorCode.PAYMENT_API_ERROR, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public PaymentApiException(Throwable cause) {
        super(PaymentErrorCode.PAYMENT_API_ERROR, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public PaymentApiException(String message, Throwable cause) {
        super(PaymentErrorCode.PAYMENT_API_ERROR, message, cause);
    }
}