package com.fream.back.domain.user.security.oauth2;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
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
        log.info("Naver OAuth Attributes: {}", attributes);

        // attributes에서 response 객체 추출
        Object responseObj = attributes.get("response");
        if (!(responseObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Naver OAuth response structure");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) responseObj;

        return OAuthAttributes.builder()
                .name(response.containsKey("name") ? (String) response.get("name") : null)
                .email(response.containsKey("email") ? (String) response.get("email") : null)
                .picture(response.containsKey("profile_image") ? (String) response.get("profile_image") : null)
                .attributes(attributes)  // 전체 원본 attributes 유지
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        log.info("Kakao OAuth Attributes: {}", attributes);

        // attributes에서 kakao_account 객체 추출 (이메일, 성별 등의 정보가 있음)
        Object kakaoAccountObj = attributes.get("kakao_account");
        if (!(kakaoAccountObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Kakao OAuth response structure - kakao_account missing");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoAccountObj;

        // kakao_account에서 profile 객체 추출 (닉네임, 프로필 이미지 등의 정보가 있음)
        Object profileObj = kakaoAccount.get("profile");
        if (!(profileObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Kakao OAuth response structure - profile missing");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) profileObj;

        String email = kakaoAccount.containsKey("email") ? (String) kakaoAccount.get("email") : null;
        String nickname = profile.containsKey("nickname") ? (String) profile.get("nickname") : null;
        String profileImage = profile.containsKey("profile_image_url") ? (String) profile.get("profile_image_url") : null;

        return OAuthAttributes.builder()
                .name(nickname)
                .email(email)
                .picture(profileImage)
                .attributes(attributes)  // 전체 원본 attributes 유지
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private String extractProviderId(String registrationId, OAuthAttributes attributes) {
        log.info("Extracting Provider ID - Registration ID: {}", registrationId);
        log.info("Attributes for extraction: {}", attributes.getAttributes());

        try {
            if ("google".equals(registrationId)) {
                return attributes.getAttributes().get(attributes.getNameAttributeKey()).toString();
            } else if ("naver".equals(registrationId)) {
                // 네이버는 response 객체 안에 정보가 있음
                Object responseObj = attributes.getAttributes().get("response");
                if (responseObj instanceof Map) {
                    Map<String, Object> response = (Map<String, Object>) responseObj;
                    return response.get("id").toString();
                } else {
                    log.error("Naver response is not a Map: {}", responseObj);
                    throw new IllegalArgumentException("Invalid Naver OAuth response");
                }
            } else {
                return UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            log.error("Error extracting provider ID", e);
            throw e;
        }
    }
}