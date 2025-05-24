package com.fream.back.domain.user.exception;

/**
 * 본인인증 실패 시 발생하는 예외
 */
public class IdentityVerificationFailedException extends UserException {

    private final String verificationId;

    /**
     * 기본 생성자
     */
    public IdentityVerificationFailedException() {
        super(UserErrorCode.IDENTITY_VERIFICATION_FAILED);
        this.verificationId = null;
    }

    /**
     * 인증 ID를 기반으로 한 생성자
     *
     * @param verificationId 실패한 본인인증 ID
     */
    public IdentityVerificationFailedException(String verificationId) {
        super(UserErrorCode.IDENTITY_VERIFICATION_FAILED, "본인인증에 실패했습니다: " + verificationId);
        this.verificationId = verificationId;
    }

    /**
     * 인증 ID와 커스텀 메시지를 포함한 생성자
     *
     * @param verificationId 실패한 본인인증 ID
     * @param message 커스텀 메시지
     */
    public IdentityVerificationFailedException(String verificationId, String message) {
        super(UserErrorCode.IDENTITY_VERIFICATION_FAILED, message);
        this.verificationId = verificationId;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public IdentityVerificationFailedException(String message, Throwable cause) {
        super(UserErrorCode.IDENTITY_VERIFICATION_FAILED, message, cause);
        this.verificationId = null;
    }

    /**
     * 본인인증 ID 반환
     *
     * @return 본인인증 ID
     */
    public String getVerificationId() {
        return verificationId;
    }
}