package com.fream.back.domain.user.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends UserException {

    /**
     * 기본 생성자
     */
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }

    /**
     * 이메일을 기반으로 한 생성자
     *
     * @param email 찾을 수 없는 사용자의 이메일
     */
    public UserNotFoundException(String email) {
        super(UserErrorCode.USER_NOT_FOUND, "이메일이 " + email + "인 사용자를 찾을 수 없습니다.");
    }

    /**
     * 사용자 ID를 기반으로 한 생성자
     *
     * @param userId 찾을 수 없는 사용자의 ID
     */
    public UserNotFoundException(Long userId) {
        super(UserErrorCode.USER_NOT_FOUND, "ID가 " + userId + "인 사용자를 찾을 수 없습니다.");
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(UserErrorCode.USER_NOT_FOUND, message, cause);
    }
}