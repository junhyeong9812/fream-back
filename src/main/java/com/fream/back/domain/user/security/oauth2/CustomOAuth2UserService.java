package com.fream.back.domain.user.security.oauth2;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.profile.ProfileCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final ProfileCommandService profileCommandService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        log.info("OAuth2 Provider: {}", registrationId);
        log.info("Full OAuth2 Attributes: {}", oAuth2User.getAttributes());

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        try {
            OAuthAttributes attributes = OAuthAttributes.of(
                    registrationId, userNameAttributeName, oAuth2User.getAttributes());

            log.info("OAuth2 로그인 시도: 제공자={}, 이메일={}", registrationId, attributes.getEmail());

            User user = saveOrUpdate(attributes, registrationId);

            // 사용자의 데이터베이스 ID를 사용
            Map<String, Object> modifiedAttributes = new HashMap<>(attributes.getAttributes());
            modifiedAttributes.put("id", user.getId().toString());

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                    modifiedAttributes,
                    registrationId.equals("naver") || registrationId.equals("kakao") ? "id" : attributes.getNameAttributeKey());
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("OAuth2 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 유저 정보 저장 또는 업데이트
     */
    @Transactional
    private User saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        Optional<User> existingUser = userRepository.findByEmail(attributes.getEmail());

        // 프로바이더 ID 추출
        String providerId = extractProviderId(registrationId, attributes);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // 현재 OAuth 제공자와 아직 연결되어 있지 않다면 새로 연결
            if (!user.hasOAuthConnection(registrationId)) {
                user.addOAuthConnection(registrationId, providerId);
            }

            log.info("기존 사용자 OAuth2 정보 업데이트: {}, 제공자: {}", attributes.getEmail(), registrationId);
            return user;
        } else {
            log.info("새 OAuth2 사용자 등록: {}, 제공자: {}", attributes.getEmail(), registrationId);

            // 비밀번호 암호화
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

            // 새 사용자 생성 (기본 정보만 설정)
            User user = User.builder()
                    .email(attributes.getEmail())
                    .password(randomPassword)
                    .referralCode(generateUniqueReferralCode())
                    .phoneNumber("") // OAuth 로그인에서는 전화번호를 제공하지 않음
                    .termsAgreement(true) // 기본 이용약관 동의
                    .role(Role.USER)
                    .isVerified(false) // 아직 추가 정보 입력이 필요함
                    .build();

            User savedUser = userRepository.save(user);

            // OAuth 연결 추가
            savedUser.addOAuthConnection(registrationId, providerId);

            // 기본 프로필 생성
            profileCommandService.createDefaultProfile(savedUser);

            return savedUser;
        }
    }

    private String extractProviderId(String registrationId, OAuthAttributes attributes) {
        try {
            if ("google".equals(registrationId)) {
                return attributes.getAttributes().get(attributes.getNameAttributeKey()).toString();
            } else if ("naver".equals(registrationId)) {
                Map<String, Object> response = (Map<String, Object>) attributes.getAttributes().get("response");
                return response.get("id").toString();
            } else if ("kakao".equals(registrationId)) {
                // 카카오는 ID가 최상위 속성으로 제공됨
                return attributes.getAttributes().get("id").toString();
            } else {
                return UUID.randomUUID().toString(); // 기본값 또는 다른 로직
            }
        } catch (Exception e) {
            log.error("제공자 ID 추출 중 오류 발생: {}, 제공자: {}", e.getMessage(), registrationId, e);
            // 에러 발생 시 무작위 UUID 반환
            return UUID.randomUUID().toString();
        }
    }

    private String generateUniqueReferralCode() {
        String referralCode;
        do {
            // 8자리 랜덤 문자열 생성
            referralCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.findByReferralCode(referralCode).isPresent()); // 중복 체크
        return referralCode;
    }
}