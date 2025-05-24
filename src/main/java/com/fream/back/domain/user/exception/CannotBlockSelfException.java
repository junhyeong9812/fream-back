package com.fream.back.domain.user.exception;

/**
 * 자기 자신을 차단하려 할 때 발생하는 예외
 */
public class CannotBlockSelfException extends UserException {

    /**
     * 기본 생성자
     */
    public CannotBlockSelfException() {
        super(UserErrorCode.CANNOT_BLOCK_SELF, "자기 자신을 차단할 수 없습니다.");
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public CannotBlockSelfException(String message) {
        super(UserErrorCode.CANNOT_BLOCK_SELF, message);
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public CannotBlockSelfException(String message, Throwable cause) {
        super(UserErrorCode.CANNOT_BLOCK_SELF, message, cause);
    }
}