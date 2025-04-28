package com.fream.back.domain.product.exception;

import com.fream.back.global.exception.GlobalException;
import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;

/**
 * 상품 도메인 관련 예외를 처리하는 클래스
 */
@Getter
public class ProductException extends GlobalException {

    /**
     * 에러 코드를 포함한 기본 생성자
     *
     * @param errorCode 에러 코드
     */
    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 메시지를 포함한 생성자
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public ProductException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 에러 코드, 메시지, 원인 예외를 포함한 생성자
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public ProductException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}