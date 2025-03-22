package com.fream.back.domain.chatQuestion.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 채팅 기능 사용 권한 관련 예외를 처리하는 클래스
 */
public class ChatPermissionException extends ChatQuestionException {

    /**
     * ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED 기본 에러 코드로 예외 생성
     */
    public ChatPermissionException() {
        super(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public ChatPermissionException(String message) {
        super(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public ChatPermissionException(Throwable cause) {
        super(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED, cause);
    }

    /**
     * 사용자 정의 에러 코드와 함께 예외 생성
     *
     * @param errorCode 채팅 질문 도메인 에러 코드
     */
    public ChatPermissionException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 사용자 정의 에러 코드와 메시지로 예외 생성
     *
     * @param errorCode 채팅 질문 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public ChatPermissionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 사용자 정의 에러 코드와 원인 예외로 예외 생성
     *
     * @param errorCode 채팅 질문 도메인 에러 코드
     * @param cause 원인이 되는 예외
     */
    public ChatPermissionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 사용자 정의 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 채팅 질문 도메인 에러 코드
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public ChatPermissionException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 기본 에러 코드, 메시지, 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public ChatPermissionException(String message, Throwable cause) {
        super(ChatQuestionErrorCode.ADMIN_PERMISSION_REQUIRED, message, cause);
    }
}