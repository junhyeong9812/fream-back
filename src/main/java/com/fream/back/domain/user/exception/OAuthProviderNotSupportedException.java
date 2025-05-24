package com.fream.back.domain.user.exception;

import java.util.List;

/**
 * 지원하지 않는 OAuth 제공자를 사용하려 할 때 발생하는 예외
 */
public class OAuthProviderNotSupportedException extends UserException {

    private final String requestedProvider;
    private final List<String> supportedProviders;

    /**
     * 기본 생성자
     */
    public OAuthProviderNotSupportedException() {
        super(UserErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        this.requestedProvider = null;
        this.supportedProviders = null;
    }

    /**
     * 요청된 제공자와 지원되는 제공자 목록을 포함한 생성자
     *
     * @param requestedProvider 요청된 OAuth 제공자
     * @param supportedProviders 지원되는 OAuth 제공자 목록
     */
    public OAuthProviderNotSupportedException(String requestedProvider, List<String> supportedProviders) {
        super(UserErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED,
                String.format("지원하지 않는 OAuth 제공자입니다: %s (지원 제공자: %s)",
                        requestedProvider,
                        supportedProviders != null ? String.join(", ", supportedProviders) : "없음"));
        this.requestedProvider = requestedProvider;
        this.supportedProviders = supportedProviders;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public OAuthProviderNotSupportedException(String message, Throwable cause) {
        super(UserErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, message, cause);
        this.requestedProvider = null;
        this.supportedProviders = null;
    }

    /**
     * 요청된 OAuth 제공자 반환
     *
     * @return 요청된 OAuth 제공자
     */
    public String getRequestedProvider() {
        return requestedProvider;
    }

    /**
     * 지원되는 OAuth 제공자 목록 반환
     *
     * @return 지원되는 OAuth 제공자 목록
     */
    public List<String> getSupportedProviders() {
        return supportedProviders;
    }
}