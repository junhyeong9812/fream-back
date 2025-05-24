package com.fream.back.domain.user.exception;

/**
 * 팔로우하지 않은 사용자를 언팔로우하려 할 때 발생하는 예외
 */
public class NotFollowingException extends UserException {

    private final Long targetProfileId;
    private final String targetEmail;

    /**
     * 기본 생성자
     */
    public NotFollowingException() {
        super(UserErrorCode.NOT_FOLLOWING);
        this.targetProfileId = null;
        this.targetEmail = null;
    }

    /**
     * 대상 프로필 ID를 기반으로 한 생성자
     *
     * @param targetProfileId 팔로우하지 않은 프로필 ID
     */
    public NotFollowingException(Long targetProfileId) {
        super(UserErrorCode.NOT_FOLLOWING, "팔로우 중이 아닌 사용자입니다. 프로필 ID: " + targetProfileId);
        this.targetProfileId = targetProfileId;
        this.targetEmail = null;
    }

    /**
     * 프로필 ID와 이메일을 모두 포함한 생성자
     *
     * @param targetProfileId 대상 프로필 ID
     * @param targetEmail 대상 이메일
     */
    public NotFollowingException(Long targetProfileId, String targetEmail) {
        super(UserErrorCode.NOT_FOLLOWING,
                String.format("팔로우 중이 아닌 사용자입니다. 프로필 ID: %d, 이메일: %s", targetProfileId, targetEmail));
        this.targetProfileId = targetProfileId;
        this.targetEmail = targetEmail;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public NotFollowingException(String message) {
        super(UserErrorCode.NOT_FOLLOWING, message);
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