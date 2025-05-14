package com.fream.back.domain.inquiry.exception;

/**
 * 1대1 문의 파일 처리 관련 예외
 */
public class InquiryFileException extends InquiryException {

    public InquiryFileException(InquiryErrorCode errorCode) {
        super(errorCode);
    }

    public InquiryFileException(InquiryErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InquiryFileException(InquiryErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}