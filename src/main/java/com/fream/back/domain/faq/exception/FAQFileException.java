package com.fream.back.domain.faq.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * FAQ 파일 처리 과정에서 발생하는 예외
 */
public class FAQFileException extends FAQException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public FAQFileException() {
        super(FAQErrorCode.FAQ_FILE_SAVE_ERROR);
    }

    /**
     * 사용자 정의 에러 코드와 함께 예외 생성
     *
     * @param errorCode FAQ 도메인 에러 코드
     */
    public FAQFileException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 사용자 정의 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode FAQ 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public FAQFileException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 사용자 정의 에러 코드와 원인 예외로 예외 생성
     *
     * @param errorCode FAQ 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public FAQFileException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 사용자 정의 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode FAQ 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public FAQFileException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}