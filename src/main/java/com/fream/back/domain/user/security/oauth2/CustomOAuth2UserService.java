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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

        // OAuth2 서비스 ID (google, naver, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // OAuth2 로그인 진행 시 키가 되는 필드 값 (Primary Key)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2UserService를 통해 가져온 OAuth2User의 attribute를 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId, userNameAttributeName, oAuth2User.getAttributes());

        log.info("OAuth2 로그인 시도: 제공자={}, 이메일={}", registrationId, attributes.getEmail());

        User user = saveOrUpdate(attributes, registrationId);

        // DefaultOAuth2User 객체 생성 및 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
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
        if ("google".equals(registrationId)) {
            return attributes.getAttributes().get(attributes.getNameAttributeKey()).toString();
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.getAttributes().get("response");
            return response.get("id").toString();
        } else {
            return UUID.randomUUID().toString(); // 기본값 또는 다른 로직
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