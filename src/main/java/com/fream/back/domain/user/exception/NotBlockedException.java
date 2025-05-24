package com.fream.back.domain.user.exception;

/**
 * 차단되지 않은 사용자를 차단 해제하려 할 때 발생하는 예외
 */
public class NotBlockedException extends UserException {

    private final Long targetProfileId;

    /**
     * 기본 생성자
     */
    public NotBlockedException() {
        super(UserErrorCode.NOT_BLOCKED);
        this.targetProfileId = null;
    }

    /**
     * 대상 프로필 ID를 기반으로 한 생성자
     *
     * @param targetProfileId 차단되지 않은 프로필 ID
     */
    public NotBlockedException(Long targetProfileId) {
        super(UserErrorCode.NOT_BLOCKED, "차단되지 않은 사용자입니다. 프로필 ID: " + targetProfileId);
        this.targetProfileId = targetProfileId;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public NotBlockedException(String message) {
        super(UserErrorCode.NOT_BLOCKED, message);
        this.targetProfileId = null;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public NotBlockedException(String message, Throwable cause) {
        super(UserErrorCode.NOT_BLOCKED, message, cause);
        this.targetProfileId = null;
    }

    /**
     * 대상 프로필 ID 반환
     *
     * @return 대상 프로필 ID
     */
    public Long getTargetProfileId() {
        return targetProfileId;
    }
}