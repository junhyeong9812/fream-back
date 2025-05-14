package com.fream.back.domain.inquiry.exception;

/**
 * 1대1 문의를 찾을 수 없을 때 발생하는 예외
 */
public class InquiryNotFoundException extends InquiryException {

    public InquiryNotFoundException(String message) {
        super(InquiryErrorCode.INQUIRY_NOT_FOUND, message);
    }

    public InquiryNotFoundException(String message, Throwable cause) {
        super(InquiryErrorCode.INQUIRY_NOT_FOUND, message, cause);
    }
}