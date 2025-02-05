package com.fream.back.domain.user.service.command;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.profile.ProfileCommandService;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileCommandService profileService;
    private final FileUtils fileUtils;
    private final NotificationCommandService notificationCommandService;

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        // 필수 동의 확인
        if (!dto.getIsOver14() || !dto.getTermsAgreement() || !dto.getPrivacyAgreement()) {
            throw new IllegalArgumentException("필수 동의 조건을 만족하지 않았습니다.");
        }

        // 추천인 코드가 존재할 경우 확인
        User referrer = null;
        if (dto.getReferralCode() != null) {
            referrer = userRepository.findByReferralCode(dto.getReferralCode())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 추천인 코드입니다."));
        }
        // 이메일 중복 확인
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 사용자 생성
        User user = User.builder()
                .email(dto.getEmail())
                .password(encodedPassword)
                .referralCode(generateUniqueReferralCode())
                .shoeSize(dto.getShoeSize())
                .phoneNumber(dto.getPhoneNumber())
                .referrer(referrer)
                .termsAgreement(dto.getTermsAgreement())
                .phoneNotificationConsent(dto.getAdConsent() != null ? dto.getAdConsent() : false)
                .emailNotificationConsent(dto.getOptionalPrivacyAgreement() != null ? dto.getOptionalPrivacyAgreement() : false)
                .build();

        // 개인정보 동의 설정
        user.updateConsent(dto.getAdConsent(), dto.getOptionalPrivacyAgreement());

        // 사용자 저장
        userRepository.save(user);

        // 프로필 생성 (프로필 서비스 호출)
        profileService.createDefaultProfile(user);

        return user;
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        // 사용자 알림 삭제
        notificationCommandService.deleteNotificationsByUser(email);


        // 프로필 이미지 삭제
        if (user.getProfile() != null && user.getProfile().getProfileImageUrl() != null) {
            fileUtils.deleteFile("profile_images", user.getProfile().getProfileImageUrl());
        }

        // 사용자 정보 삭제
        userRepository.delete(user);
    }

    //추천인 코드 생성
    private String generateUniqueReferralCode() {
        String referralCode;
        do {
            // 8자리 랜덤 문자열 생성
            referralCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.findByReferralCode(referralCode).isPresent()); // 중복 체크
        return referralCode;
    }
}


