package com.fream.back.domain.user.exception;

/**
 * 이미 팔로우 중인 사용자를 다시 팔로우하려 할 때 발생하는 예외
 */
public class AlreadyFollowingException extends UserException {

    private final Long targetProfileId;
    private final String targetEmail;

    /**
     * 기본 생성자
     */
    public AlreadyFollowingException() {
        super(UserErrorCode.ALREADY_FOLLOWING);
        this.targetProfileId = null;
        this.targetEmail = null;
    }

    /**
     * 대상 프로필 ID를 기반으로 한 생성자
     *
     * @param targetProfileId 이미 팔로우 중인 프로필 ID
     */
    public AlreadyFollowingException(Long targetProfileId) {
        super(UserErrorCode.ALREADY_FOLLOWING, "이미 팔로우 중인 사용자입니다. 프로필 ID: " + targetProfileId);
        this.targetProfileId = targetProfileId;
        this.targetEmail = null;
    }

    /**
     * 프로필 ID와 이메일을 모두 포함한 생성자
     *
     * @param targetProfileId 대상 프로필 ID
     * @param targetEmail 대상 이메일
     */
    public AlreadyFollowingException(Long targetProfileId, String targetEmail) {
        super(UserErrorCode.ALREADY_FOLLOWING,
                String.format("이미 팔로우 중인 사용자입니다. 프로필 ID: %d, 이메일: %s", targetProfileId, targetEmail));
        this.targetProfileId = targetProfileId;
        this.targetEmail = targetEmail;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public AlreadyFollowingException(String message) {
        super(UserErrorCode.ALREADY_FOLLOWING, message);
        this.targetProfileId = null;
        this.targetEmail = null;
    }

    /**
     * 대상 프로필 ID 반환
     *
     * @return 대상 프로필 ID
     */
    public Long getTargetProfileId() {
        return targetProfileId;
    }

    /**
     * 대상 이메일 반환
     *
     * @return 대상 이메일
     */
    public String getTargetEmail() {
        return targetEmail;
    }
}