package com.fream.back.domain.user.exception;

/**
 * OAuth 인증 실패 시 발생하는 예외
 */
public class OAuthAuthenticationFailedException extends UserException {

    private final String provider;
    private final String reason;

    /**
     * 기본 생성자
     */
    public OAuthAuthenticationFailedException() {
        super(UserErrorCode.OAUTH_AUTHENTICATION_FAILED);
        this.provider = null;
        this.reason = null;
    }

    /**
     * OAuth 제공자와 실패 사유를 포함한 생성자
     *
     * @param provider OAuth 제공자
     * @param reason 실패 사유
     */
    public OAuthAuthenticationFailedException(String provider, String reason) {
        super(UserErrorCode.OAUTH_AUTHENTICATION_FAILED,
                String.format("OAuth 인증에 실패했습니다. 제공자: %s, 사유: %s", provider, reason));
        this.provider = provider;
        this.reason = reason;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public OAuthAuthenticationFailedException(String message, Throwable cause) {
        super(UserErrorCode.OAUTH_AUTHENTICATION_FAILED, message, cause);
        this.provider = null;
        this.reason = null;
    }

    /**
     * OAuth 제공자 반환
     *
     * @return OAuth 제공자
     */
    public String getProvider() {
        return provider;
    }

    /**
     * 인증 실패 사유 반환
     *
     * @return 인증 실패 사유
     */
    public String getReason() {
        return reason;
    }
}