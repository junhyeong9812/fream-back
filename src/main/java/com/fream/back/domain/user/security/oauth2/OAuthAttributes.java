package com.fream.back.domain.user.security.oauth2;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
                           String name, String email, String picture) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return ofGoogle(userNameAttributeName, attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        // 안전한 null 체크 및 타입 확인
        if (attributes == null) {
            throw new IllegalArgumentException("Google OAuth attributes are null");
        }

        return OAuthAttributes.builder()
                .name(attributes.containsKey("name") ? (String) attributes.get("name") : null)
                .email(attributes.containsKey("email") ? (String) attributes.get("email") : null)
                .picture(attributes.containsKey("picture") ? (String) attributes.get("picture") : null)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 안전한 null 체크 및 타입 확인
        if (attributes == null) {
            throw new IllegalArgumentException("Naver OAuth attributes are null");
        }

        // response가 Map인지 확인
        Object responseObj = attributes.get("response");
        if (!(responseObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Naver OAuth response structure");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) responseObj;

        // 각 필드에 대한 null 체크
        return OAuthAttributes.builder()
                .name(response.containsKey("name") ? (String) response.get("name") : null)
                .email(response.containsKey("email") ? (String) response.get("email") : null)
                .picture(response.containsKey("profile_image") ? (String) response.get("profile_image") : null)
                .attributes(attributes)  // 전체 원본 attributes 유지
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
}