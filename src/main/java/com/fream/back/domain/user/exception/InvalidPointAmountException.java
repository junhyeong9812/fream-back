package com.fream.back.domain.user.exception;

/**
 * 잘못된 포인트 금액일 때 발생하는 예외
 */
public class InvalidPointAmountException extends UserException {

    private final Integer amount;

    /**
     * 기본 생성자
     */
    public InvalidPointAmountException() {
        super(UserErrorCode.INVALID_POINT_AMOUNT);
        this.amount = null;
    }

    /**
     * 포인트 금액을 기반으로 한 생성자
     *
     * @param amount 잘못된 포인트 금액
     */
    public InvalidPointAmountException(Integer amount) {
        super(UserErrorCode.INVALID_POINT_AMOUNT, "잘못된 포인트 금액입니다: " + amount);
        this.amount = amount;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public InvalidPointAmountException(String message) {
        super(UserErrorCode.INVALID_POINT_AMOUNT, message);
        this.amount = null;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidPointAmountException(String message, Throwable cause) {
        super(UserErrorCode.INVALID_POINT_AMOUNT, message, cause);
        this.amount = null;
    }

    /**
     * 포인트 금액 반환
     *
     * @return 포인트 금액
     */
    public Integer getAmount() {
        return amount;
    }
}