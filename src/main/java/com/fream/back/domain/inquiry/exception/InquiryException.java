package com.fream.back.domain.inquiry.exception;

import com.fream.back.global.exception.GlobalException;
import lombok.Getter;

/**
 * 1대1 문의 관련 커스텀 예외 클래스
 */
@Getter
public class InquiryException extends GlobalException {

    public InquiryException(InquiryErrorCode errorCode) {
        super(errorCode);
    }

    public InquiryException(InquiryErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InquiryException(InquiryErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    @Override
    public InquiryErrorCode getErrorCode() {
        return (InquiryErrorCode) super.getErrorCode();
    }
}