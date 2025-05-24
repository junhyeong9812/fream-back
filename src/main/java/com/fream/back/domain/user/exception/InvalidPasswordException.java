package com.fream.back.domain.user.exception;

/**
 * 잘못된 비밀번호일 때 발생하는 예외
 */
public class InvalidPasswordException extends UserException {

    /**
     * 기본 생성자
     */
    public InvalidPasswordException() {
        super(UserErrorCode.INVALID_PASSWORD);
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public InvalidPasswordException(String message) {
        super(UserErrorCode.INVALID_PASSWORD, message);
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidPasswordException(String message, Throwable cause) {
        super(UserErrorCode.INVALID_PASSWORD, message, cause);
    }
}