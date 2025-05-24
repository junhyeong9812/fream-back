package com.fream.back.domain.user.exception;

/**
 * 자기 자신을 팔로우하려 할 때 발생하는 예외
 */
public class CannotFollowSelfException extends UserException {

    /**
     * 기본 생성자
     */
    public CannotFollowSelfException() {
        super(UserErrorCode.CANNOT_FOLLOW_SELF, "자기 자신을 팔로우할 수 없습니다.");
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public CannotFollowSelfException(String message) {
        super(UserErrorCode.CANNOT_FOLLOW_SELF, message);
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public CannotFollowSelfException(String message, Throwable cause) {
        super(UserErrorCode.CANNOT_FOLLOW_SELF, message, cause);
    }
}