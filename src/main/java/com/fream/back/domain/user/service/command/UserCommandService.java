package com.fream.back.domain.user.service.command;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.dto.VerifiedCustomerDto;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.profile.ProfileCommandService;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileCommandService profileService;
    private final FileUtils fileUtils;
    private final NotificationCommandService notificationCommandService;
    private final IdentityVerificationService identityVerificationService;

    /**
     * 본인인증을 통한 회원가입 처리
     * @param dto 회원가입 정보
     * @return 생성된 사용자 정보
     */
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        // 필수 동의 확인
        if (!dto.getIsOver14() || !dto.getTermsAgreement() || !dto.getPrivacyAgreement()) {
            throw new IllegalArgumentException("필수 동의 조건을 만족하지 않았습니다.");
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 본인인증 정보 검증
        if (dto.getIdentityVerificationId() == null || dto.getIdentityVerificationId().isEmpty()) {
            throw new IllegalArgumentException("본인인증이 필요합니다.");
        }

        // PortOne API를 통해 본인인증 정보 검증
        VerifiedCustomerDto verifiedCustomer = identityVerificationService.verifyIdentity(dto.getIdentityVerificationId());

        // CI 값이 있는 경우 중복 가입 확인 (선택적)
        if (verifiedCustomer.getCi() != null && !verifiedCustomer.getCi().isEmpty()) {
            userRepository.findByCi(verifiedCustomer.getCi())
                    .ifPresent(existingUser -> {
                        throw new IllegalArgumentException("이미 가입된 사용자입니다. 다른 계정으로 로그인해주세요.");
                    });
        }

        // 만 14세 이상 검증 (생년월일로 계산)
        if (dto.getIsOver14()) {
            validateAge(verifiedCustomer.getBirthDate());
        }

        // 추천인 코드가 존재할 경우 확인
        User referrer = null;
        if (dto.getReferralCode() != null && !dto.getReferralCode().isEmpty()) {
            referrer = userRepository.findByReferralCode(dto.getReferralCode())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 추천인 코드입니다."));
        }

        // 본인인증을 통해 얻은 성별 정보 Gender 타입으로 변환
        Gender gender = convertToGender(verifiedCustomer.getGender());

        // 나이 계산
        Integer age = calculateAge(verifiedCustomer.getBirthDate());

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 사용자 생성 (본인인증 정보 사용)
        User user = User.builder()
                .email(dto.getEmail())
                .password(encodedPassword)
                .referralCode(generateUniqueReferralCode())
                .shoeSize(dto.getShoeSize())
                .phoneNumber(verifiedCustomer.getPhoneNumber()) // 본인인증으로 검증된 전화번호 사용
                .age(age) // 계산된 나이 저장
                .gender(gender) // 본인인증으로 검증된 성별 정보
                .termsAgreement(dto.getTermsAgreement())
                .phoneNotificationConsent(dto.getAdConsent() != null ? dto.getAdConsent() : false)
                .emailNotificationConsent(dto.getOptionalPrivacyAgreement() != null ? dto.getOptionalPrivacyAgreement() : false)
                .optionalPrivacyAgreement(dto.getOptionalPrivacyAgreement() != null ? dto.getOptionalPrivacyAgreement() : false)
                .isVerified(true) // 본인인증 완료 상태 설정
                .ci(verifiedCustomer.getCi()) // CI 저장
                .di(verifiedCustomer.getDi()) // DI 저장
                .referrer(referrer)
                .build();

        // 개인정보 동의 설정
        user.updateConsent(dto.getAdConsent(), dto.getOptionalPrivacyAgreement());

        // 추천인 연결
        if (referrer != null) {
            user.addReferrer(referrer);
        }

        // 사용자 저장
        userRepository.save(user);

        // 프로필 생성 (프로필 서비스 호출)
        profileService.createDefaultProfile(user);

        log.info("사용자 회원가입 완료: {}, 전화번호: {}, 본인인증: 완료", user.getEmail(), user.getPhoneNumber());

        return user;
    }

    /**
     * 생년월일로 만 14세 이상인지 검증
     */
    private void validateAge(String birthDate) {
        LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE);
        LocalDate minAgeDate = LocalDate.now().minusYears(14);

        if (birth.isAfter(minAgeDate)) {
            throw new IllegalArgumentException("만 14세 이상만 가입 가능합니다.");
        }
    }

    /**
     * 생년월일로 나이 계산
     */
    private Integer calculateAge(String birthDate) {
        LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE);
        return Period.between(birth, LocalDate.now()).getYears();
    }

    /**
     * 본인인증 성별 정보 변환
     */
    private Gender convertToGender(String genderStr) {
        if (genderStr == null || genderStr.isEmpty()) {
            return null;
        }

        // 성별 정보 매핑 (이니시스 값에 따라 조정 필요)
        if ("male".equalsIgnoreCase(genderStr) || "M".equalsIgnoreCase(genderStr)) {
            return Gender.MALE;
        } else if ("female".equalsIgnoreCase(genderStr) || "F".equalsIgnoreCase(genderStr)) {
            return Gender.FEMALE;
        }

        return null;
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