package com.fream.back.domain.product.exception;

/**
 * 브랜드를 찾을 수 없을 때 발생하는 예외
 */
public class BrandNotFoundException extends ProductException {

    /**
     * 기본 생성자
     */
    public BrandNotFoundException() {
        super(ProductErrorCode.BRAND_NOT_FOUND);
    }

    /**
     * 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public BrandNotFoundException(String message) {
        super(ProductErrorCode.BRAND_NOT_FOUND, message);
    }

    /**
     * brandId를 기반으로 한 생성자
     *
     * @param brandId 찾을 수 없는 브랜드 ID
     */
    public BrandNotFoundException(Long brandId) {
        super(ProductErrorCode.BRAND_NOT_FOUND, "ID가 " + brandId + "인 브랜드를 찾을 수 없습니다.");
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public BrandNotFoundException(String message, Throwable cause) {
        super(ProductErrorCode.BRAND_NOT_FOUND, message, cause);
    }
}