package com.fream.back.domain.product.exception;

/**
 * 유효하지 않은 사이즈 타입일 때 발생하는 예외
 */
public class InvalidSizeTypeException extends ProductException {

    /**
     * 기본 생성자
     */
    public InvalidSizeTypeException() {
        super(ProductErrorCode.INVALID_SIZE_TYPE);
    }

    /**
     * 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public InvalidSizeTypeException(String message) {
        super(ProductErrorCode.INVALID_SIZE_TYPE, message);
    }

    /**
     * 카테고리 이름과 사이즈를 기반으로 한 생성자
     *
     * @param categoryName 카테고리 이름
     * @param size 잘못된 사이즈
     */
    public InvalidSizeTypeException(String categoryName, String size) {
        super(ProductErrorCode.INVALID_SIZE_TYPE,
                "카테고리 '" + categoryName + "'에 대해 '" + size + "'는 유효하지 않은 사이즈입니다.");
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidSizeTypeException(String message, Throwable cause) {
        super(ProductErrorCode.INVALID_SIZE_TYPE, message, cause);
    }
}