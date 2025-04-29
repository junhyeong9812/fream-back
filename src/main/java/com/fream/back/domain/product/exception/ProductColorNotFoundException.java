package com.fream.back.domain.product.exception;

/**
 * 상품 색상을 찾을 수 없을 때 발생하는 예외
 */
public class ProductColorNotFoundException extends ProductException {

    /**
     * 기본 생성자
     */
    public ProductColorNotFoundException() {
        super(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND);
    }

    /**
     * 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public ProductColorNotFoundException(String message) {
        super(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND, message);
    }

    /**
     * productColorId를 기반으로 한 생성자
     *
     * @param productColorId 찾을 수 없는 상품 색상 ID
     */
    public ProductColorNotFoundException(Long productColorId) {
        super(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND, "ID가 " + productColorId + "인 상품 색상을 찾을 수 없습니다.");
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public ProductColorNotFoundException(String message, Throwable cause) {
        super(ProductErrorCode.PRODUCT_COLOR_NOT_FOUND, message, cause);
    }
}