package com.fream.back.domain.order.exception;

/**
 * 입찰 가격이 유효하지 않을 때 발생하는 예외
 */
public class InvalidBidPriceException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InvalidBidPriceException() {
        super(OrderErrorCode.INVALID_BID_PRICE);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidBidPriceException(String message) {
        super(OrderErrorCode.INVALID_BID_PRICE, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InvalidBidPriceException(Throwable cause) {
        super(OrderErrorCode.INVALID_BID_PRICE, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InvalidBidPriceException(String message, Throwable cause) {
        super(OrderErrorCode.INVALID_BID_PRICE, message, cause);
    }
}