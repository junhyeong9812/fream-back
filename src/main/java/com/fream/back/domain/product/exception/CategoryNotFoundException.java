package com.fream.back.domain.product.exception;

/**
 * 카테고리를 찾을 수 없을 때 발생하는 예외
 */
public class CategoryNotFoundException extends ProductException {

    /**
     * 기본 생성자
     */
    public CategoryNotFoundException() {
        super(ProductErrorCode.CATEGORY_NOT_FOUND);
    }

    /**
     * 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public CategoryNotFoundException(String message) {
        super(ProductErrorCode.CATEGORY_NOT_FOUND, message);
    }

    /**
     * categoryId를 기반으로 한 생성자
     *
     * @param categoryId 찾을 수 없는 카테고리 ID
     */
    public CategoryNotFoundException(Long categoryId) {
        super(ProductErrorCode.CATEGORY_NOT_FOUND, "ID가 " + categoryId + "인 카테고리를 찾을 수 없습니다.");
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public CategoryNotFoundException(String message, Throwable cause) {
        super(ProductErrorCode.CATEGORY_NOT_FOUND, message, cause);
    }
}