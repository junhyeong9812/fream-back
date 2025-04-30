package com.fream.back.domain.sale.exception;

import com.fream.back.global.exception.ErrorCode;
import com.fream.back.global.exception.GlobalException;

/**
 * Sale 도메인의 모든 예외의 기본이 되는 클래스
 */
public class SaleException extends GlobalException {

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     */
    public SaleException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     * @param message 사용자 정의 에러 메시지
     */
    public SaleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     * @param cause 원인이 되는 예외
     */
    public SaleException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * ErrorCode, 사용자 정의 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public SaleException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}