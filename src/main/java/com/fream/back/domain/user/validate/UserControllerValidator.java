package com.fream.back.domain.user.validate;

import com.fream.back.domain.user.dto.*;

import java.util.regex.Pattern;


public class UserControllerValidator {

    //1) 정규식 등 공통 상수
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$");

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{}|;:,.<>?/";
    private static final int MIN_PASSWORD_LENGTH = 8;

    // 2) 회원가입 검증
    public static void validateUserRegistrationDto(UserRegistrationDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("회원가입 정보를 입력해주세요.");
        }

        // 1) 이메일
        if (dto.getEmail() == null || dto.getEmail().isBlank()
                || !EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new IllegalArgumentException("유효한 이메일 주소가 아닙니다.");
        }

        // 2) 비밀번호
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        if (!isValidPassword(dto.getPassword())) {
            throw new IllegalArgumentException(
                    "비밀번호는 8자 이상, 영문 대소문자/숫자/특수문자를 모두 포함해야 합니다."
            );
        }

        // 3) 전화번호(선택사항)
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
                throw new IllegalArgumentException("전화번호는 010-XXXX-XXXX 형식이어야 합니다.");
            }
        }

        // 4) 신발 사이즈 (Enum)
        if (dto.getShoeSize() == null) {
            throw new IllegalArgumentException("신발 사이즈를 선택해주세요.");
        }
        // (Enum 변환 에러는 Jackson 파싱 단계에서 발생하므로 여기서는 null 여부만 확인)

        // 5) 만14세 이상, 필수약관 동의
        if (Boolean.FALSE.equals(dto.getIsOver14())) {
            throw new IllegalArgumentException("만 14세 이상이어야 가입할 수 있습니다.");
        }
        if (Boolean.FALSE.equals(dto.getTermsAgreement())) {
            throw new IllegalArgumentException("이용약관에 동의가 필요합니다.");
        }
        if (Boolean.FALSE.equals(dto.getPrivacyAgreement())) {
            throw new IllegalArgumentException("개인정보 수집 및 이용(필수)에 동의가 필요합니다.");
        }
    }

    // 3) [로그인] LoginRequestDto 검증 (추가)
    public static void validateLoginRequestDto(LoginRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("로그인 정보를 입력해주세요.");
        }

        // (1) 이메일 검증
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }

        // (2) 비밀번호 검증
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        // 로그인 시에는 별도의 복잡한 규칙(대소문자/특수문자)까지는 보통 확인하지 않는 경우도 많습니다.
        // 하지만 필요하다면 isValidPassword(dto.getPassword()) 호출도 가능.
    }

    // 4) [이메일 찾기] EmailFindRequestDto 검증 (추가)
    public static void validateEmailFindRequestDto(EmailFindRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("이메일 찾기에 필요한 정보를 입력해주세요.");
        }

        // (1) 휴대폰 번호 검증
        // 전화번호를 반드시 입력받는지, 선택인지에 따라 로직 분기
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해주세요.");
        }
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            throw new IllegalArgumentException("전화번호는 010-XXXX-XXXX 형식이어야 합니다.");
        }
    }

    // 5) [비밀번호 재설정 요청] PasswordResetRequestDto 검증
    public static void validatePasswordResetEligibilityDto(PasswordResetRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("비밀번호 재설정 확인을 위한 정보가 없습니다.");
        }

        // (이메일, 폰번호만 체크)
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new IllegalArgumentException("유효한 이메일 주소가 아닙니다.");
        }

        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해주세요.");
        }
        if (!PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            throw new IllegalArgumentException("전화번호는 010-XXXX-XXXX 형식이어야 합니다.");
        }
    }

    // 6) [비밀번호 재설정 - 실제 변경 시] PasswordResetRequestDto 검증
    public static void validatePasswordResetDto(PasswordResetRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("비밀번호 재설정 정보를 입력해주세요.");
        }

        // (1) 새 비밀번호
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("새로운 비밀번호를 입력해주세요.");
        }
        if (!isValidPassword(dto.getNewPassword())) {
            throw new IllegalArgumentException(
                    "비밀번호는 영문 대소문자/숫자/특수문자를 모두 포함한 8자 이상이어야 합니다."
            );
        }

        // (2) 비밀번호 확인
        if (dto.getConfirmPassword() == null || dto.getConfirmPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호 확인란을 입력해주세요.");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
    }


    // 7) [로그인 정보 변경] LoginInfoUpdateDto 검증 (이미 있음)
    public static void validateLoginInfoUpdateDto(LoginInfoUpdateDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("로그인 정보 변경 DTO가 비어있습니다.");
        }

        // 현재 비밀번호 필수
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호가 필요합니다.");
        }

        // 새 이메일
        if (dto.getNewEmail() != null && !dto.getNewEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(dto.getNewEmail()).matches()) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
            }
        }

        // 새 비밀번호
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (!isValidPassword(dto.getNewPassword())) {
                throw new IllegalArgumentException(
                        "새 비밀번호는 영문 대소문자/숫자/특수문자를 포함한 8자 이상이어야 합니다."
                );
            }
        }

        // 새 휴대폰 번호
        if (dto.getNewPhoneNumber() != null && !dto.getNewPhoneNumber().isBlank()) {
            if (!PHONE_PATTERN.matcher(dto.getNewPhoneNumber()).matches()) {
                throw new IllegalArgumentException("휴대폰 번호는 010-XXXX-XXXX 형식이어야 합니다.");
            }
        }
        // 광고수신동의 / 개인정보동의 등은 옵션이므로 추가 검증 필요시 로직 추가
    }


    //  공통 사용 메서드: 비밀번호 복잡도 체크
    // 비밀번호 검증 (영문 대소문자 + 숫자 + 특수문자 1개 이상 & 8자 이상)
    private static boolean isValidPassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;

        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (SPECIAL_CHARACTERS.indexOf(ch) != -1) {
                hasSpecial = true;
            }
        }
        return (hasUpper && hasLower && hasDigit && hasSpecial);
    }


}
