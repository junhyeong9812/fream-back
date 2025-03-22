package com.fream.back.domain.faq.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * FAQ 관리 권한 관련 예외
 */
public class FAQPermissionException extends FAQException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public FAQPermissionException() {
        super(FAQErrorCode.FAQ_ADMIN_PERMISSION_REQUIRED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public FAQPermissionException(String message) {
        super(FAQErrorCode.FAQ_ADMIN_PERMISSION_REQUIRED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public FAQPermissionException(Throwable cause) {
        super(FAQErrorCode.FAQ_ADMIN_PERMISSION_REQUIRED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public FAQPermissionException(String message, Throwable cause) {
        super(FAQErrorCode.FAQ_ADMIN_PERMISSION_REQUIRED, message, cause);
    }
}