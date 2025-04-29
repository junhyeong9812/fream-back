package com.fream.back.domain.product.exception;

import com.fream.back.global.exception.GlobalException;
import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 도메인 관련 예외를 처리하는 클래스
 */
@Getter
@Slf4j
public class ProductException extends GlobalException {

    /**
     * 에러 코드를 포함한 기본 생성자
     *
     * @param errorCode 에러 코드
     */
    public ProductException(ErrorCode errorCode) {
        super(errorCode);
        logError(errorCode.getMessage());
    }

    /**
     * 에러 코드와 메시지를 포함한 생성자
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public ProductException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        logError(message);
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
        logError(message, cause);
    }

    /**
     * 에러 로깅 (원인 예외 없음)
     *
     * @param message 로그 메시지
     */
    protected void logError(String message) {
        log.error("[상품 도메인 예외] 코드: {}, 상태: {}, 메시지: {}",
                getErrorCode().getCode(),
                getErrorCode().getStatus(),
                message);
    }

    /**
     * 에러 로깅 (원인 예외 포함)
     *
     * @param message 로그 메시지
     * @param cause 원인 예외
     */
    protected void logError(String message, Throwable cause) {
        log.error("[상품 도메인 예외] 코드: {}, 상태: {}, 메시지: {}",
                getErrorCode().getCode(),
                getErrorCode().getStatus(),
                message,
                cause);
    }
}