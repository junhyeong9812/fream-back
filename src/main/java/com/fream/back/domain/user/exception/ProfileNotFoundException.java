package com.fream.back.domain.user.exception;

/**
 * 프로필을 찾을 수 없을 때 발생하는 예외
 */
public class ProfileNotFoundException extends UserException {

    /**
     * 기본 생성자
     */
    public ProfileNotFoundException() {
        super(UserErrorCode.PROFILE_NOT_FOUND);
    }

    /**
     * 프로필 ID를 기반으로 한 생성자
     *
     * @param profileId 찾을 수 없는 프로필의 ID
     */
    public ProfileNotFoundException(Long profileId) {
        super(UserErrorCode.PROFILE_NOT_FOUND, "ID가 " + profileId + "인 프로필을 찾을 수 없습니다.");
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public ProfileNotFoundException(String message) {
        super(UserErrorCode.PROFILE_NOT_FOUND, message);
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public ProfileNotFoundException(String message, Throwable cause) {
        super(UserErrorCode.PROFILE_NOT_FOUND, message, cause);
    }
}