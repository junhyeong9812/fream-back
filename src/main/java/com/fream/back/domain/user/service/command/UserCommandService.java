package com.fream.back.domain.user.service.command;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.dto.VerifiedCustomerDto;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.*;
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
import java.time.format.DateTimeParseException;
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
        log.info("회원가입 시작: email={}, identityVerificationId={}", dto.getEmail(), dto.getIdentityVerificationId());

        try {
            // 필수 동의 확인
            if (!dto.getIsOver14() || !dto.getTermsAgreement() || !dto.getPrivacyAgreement()) {
                log.warn("필수 동의 조건 미충족: email={}, isOver14={}, termsAgreement={}, privacyAgreement={}",
                        dto.getEmail(), dto.getIsOver14(), dto.getTermsAgreement(), dto.getPrivacyAgreement());
                throw new UserException(UserErrorCode.INVALID_VERIFICATION_CODE, "필수 동의 조건을 만족하지 않았습니다.");
            }

            // 이메일 중복 확인
            if (userRepository.existsByEmail(dto.getEmail())) {
                log.warn("이메일 중복: email={}", dto.getEmail());
                throw new DuplicateEmailException(dto.getEmail());
            }

            // 본인인증 정보 검증
            if (dto.getIdentityVerificationId() == null || dto.getIdentityVerificationId().isEmpty()) {
                log.warn("본인인증 ID 누락: email={}", dto.getEmail());
                throw new IdentityVerificationFailedException();
            }

            // PortOne API를 통해 본인인증 정보 검증
            VerifiedCustomerDto verifiedCustomer;
            try {
                verifiedCustomer = identityVerificationService.verifyIdentity(dto.getIdentityVerificationId());
                log.info("본인인증 검증 완료: email={}, verificationId={}", dto.getEmail(), dto.getIdentityVerificationId());
            } catch (Exception e) {
                log.error("본인인증 검증 실패: email={}, verificationId={}", dto.getEmail(), dto.getIdentityVerificationId(), e);
                throw new IdentityVerificationFailedException(dto.getIdentityVerificationId(), "본인인증 검증에 실패했습니다.");
            }

            // CI 값이 있는 경우 중복 가입 확인 (선택적)
            if (verifiedCustomer.getCi() != null && !verifiedCustomer.getCi().isEmpty()) {
                userRepository.findByCi(verifiedCustomer.getCi())
                        .ifPresent(existingUser -> {
                            log.warn("CI 중복 가입 시도: email={}, ci={}", dto.getEmail(), verifiedCustomer.getCi());
                            throw new DuplicateEmailException(existingUser.getEmail(), "이미 가입된 사용자입니다. 다른 계정으로 로그인해주세요.");
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
                        .orElseThrow(() -> {
                            log.warn("유효하지 않은 추천인 코드: email={}, referralCode={}", dto.getEmail(), dto.getReferralCode());
                            return new UserException(UserErrorCode.USER_NOT_FOUND, "유효하지 않은 추천인 코드입니다.");
                        });
                log.info("추천인 코드 확인 완료: email={}, referrerEmail={}", dto.getEmail(), referrer.getEmail());
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
                log.info("추천인 연결 완료: newUserEmail={}, referrerEmail={}", dto.getEmail(), referrer.getEmail());
            }

            // 사용자 저장
            userRepository.save(user);
            log.info("사용자 정보 저장 완료: email={}, userId={}", user.getEmail(), user.getId());

            // 프로필 생성 (프로필 서비스 호출)
            try {
                profileService.createDefaultProfile(user);
                log.info("기본 프로필 생성 완료: email={}, userId={}", user.getEmail(), user.getId());
            } catch (Exception e) {
                log.error("기본 프로필 생성 실패: email={}, userId={}", user.getEmail(), user.getId(), e);
                throw new ProfileNotFoundException("기본 프로필 생성에 실패했습니다.");
            }

            log.info("사용자 회원가입 완료: email={}, phoneNumber={}, verified={}",
                    user.getEmail(), user.getPhoneNumber(), user.isVerified());

            return user;

        } catch (UserException e) {
            log.warn("회원가입 실패 - 비즈니스 로직 오류: email={}, error={}", dto.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("회원가입 실패 - 시스템 오류: email={}", dto.getEmail(), e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "회원가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public void deleteAccount(String email) {
        log.info("계정 삭제 시작: email={}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("계정 삭제 실패 - 사용자 없음: email={}", email);
                        return new UserNotFoundException(email);
                    });

            // 사용자 알림 삭제
            try {
                notificationCommandService.deleteNotificationsByUser(email);
                log.info("사용자 알림 삭제 완료: email={}", email);
            } catch (Exception e) {
                log.warn("사용자 알림 삭제 실패: email={}", email, e);
                // 알림 삭제 실패는 계정 삭제를 중단시키지 않음
            }

            // 프로필 이미지 삭제
            try {
                if (user.getProfile() != null && user.getProfile().getProfileImageUrl() != null) {
                    fileUtils.deleteFile("profile_images", user.getProfile().getProfileImageUrl());
                    log.info("프로필 이미지 삭제 완료: email={}, imageUrl={}", email, user.getProfile().getProfileImageUrl());
                }
            } catch (Exception e) {
                log.warn("프로필 이미지 삭제 실패: email={}", email, e);
                // 이미지 삭제 실패는 계정 삭제를 중단시키지 않음
            }

            // 사용자 정보 삭제
            userRepository.delete(user);
            log.info("계정 삭제 완료: email={}, userId={}", email, user.getId());

        } catch (UserException e) {
            log.warn("계정 삭제 실패 - 비즈니스 로직 오류: email={}, error={}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("계정 삭제 실패 - 시스템 오류: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "계정 삭제 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 생년월일로 만 14세 이상인지 검증
     */
    private void validateAge(String birthDate) {
        try {
            LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE);
            LocalDate minAgeDate = LocalDate.now().minusYears(14);

            if (birth.isAfter(minAgeDate)) {
                log.warn("연령 제한 위반: birthDate={}", birthDate);
                throw new UserException(UserErrorCode.INVALID_VERIFICATION_CODE, "만 14세 이상만 가입 가능합니다.");
            }
        } catch (DateTimeParseException e) {
            log.error("생년월일 파싱 오류: birthDate={}", birthDate, e);
            throw new UserException(UserErrorCode.INVALID_VERIFICATION_CODE, "올바르지 않은 생년월일 형식입니다.");
        }
    }

    /**
     * 생년월일로 나이 계산
     */
    private Integer calculateAge(String birthDate) {
        try {
            LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE);
            return Period.between(birth, LocalDate.now()).getYears();
        } catch (DateTimeParseException e) {
            log.error("나이 계산 실패: birthDate={}", birthDate, e);
            return null;
        }
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

        log.warn("알 수 없는 성별 정보: gender={}", genderStr);
        return null;
    }

    //추천인 코드 생성
    private String generateUniqueReferralCode() {
        String referralCode;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            // 8자리 랜덤 문자열 생성
            referralCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("추천인 코드 생성 실패: 최대 시도 횟수 초과");
                throw new UserException(UserErrorCode.USER_NOT_FOUND, "추천인 코드 생성에 실패했습니다.");
            }
        } while (userRepository.findByReferralCode(referralCode).isPresent()); // 중복 체크

        log.debug("추천인 코드 생성 완료: code={}, attempts={}", referralCode, attempts);
        return referralCode;
    }
}