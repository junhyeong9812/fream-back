package com.fream.back.domain.notification.exception;

import com.fream.back.global.exception.ErrorCode;
import com.fream.back.global.exception.GlobalException;

/**
 * 알림 서비스 관련 예외를 처리하는 클래스
 * 모든 알림 관련 예외는 이 클래스를 상속받아 구현합니다.
 */
public class NotificationException extends GlobalException {

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     */
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     * @param message 사용자 정의 에러 메시지
     */
    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     * @param cause 원인이 되는 예외
     */
    public NotificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * ErrorCode, 사용자 정의 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드 enum 값
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public NotificationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}