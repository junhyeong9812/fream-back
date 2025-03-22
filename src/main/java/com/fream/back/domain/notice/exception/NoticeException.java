package com.fream.back.domain.notice.exception;

import com.fream.back.global.exception.ErrorCode;
import com.fream.back.global.exception.GlobalException;

/**
 * 공지사항 도메인에서 발생하는 모든 예외의 기본 클래스
 * GlobalException을 상속받아 전역 예외 처리 시스템과 통합됨
 */
public class NoticeException extends GlobalException {

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 공지사항 도메인 에러 코드
     */
    public NoticeException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 공지사항 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public NoticeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 공지사항 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public NoticeException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * ErrorCode, 사용자 정의 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 공지사항 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public NoticeException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}