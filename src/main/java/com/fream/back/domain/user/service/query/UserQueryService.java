package com.fream.back.domain.user.service.query;

import com.fream.back.domain.user.dto.EmailFindRequestDto;
import com.fream.back.domain.user.dto.LoginInfoDto;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import com.fream.back.domain.user.exception.UserNotFoundException;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    // 유저 이메일로 권한 확인
    @Transactional(readOnly = true)
    public void checkAdminRole(String email) {
        log.debug("관리자 권한 확인 시작: email={}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("관리자 권한 확인 실패 - 사용자 없음: email={}", email);
                        return new UserNotFoundException(email);
                    });

            if (user.getRole() != Role.ADMIN) {
                log.warn("관리자 권한 없음: email={}, role={}", email, user.getRole());
                throw new UserException(UserErrorCode.INSUFFICIENT_PERMISSIONS);
            }

            log.info("관리자 권한 확인 완료: email={}", email);

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("관리자 권한 확인 중 시스템 오류: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "권한 확인 처리 중 오류가 발생했습니다.", e);
        }
    }

    //휴대전화 번호로 이메일 정보 조회
    @Transactional(readOnly = true)
    public String findEmailByPhoneNumber(EmailFindRequestDto emailFindRequestDto) {
        String phoneNumber = emailFindRequestDto.getPhoneNumber();

        log.info("전화번호로 이메일 조회 시작: phoneNumber={}", phoneNumber);

        try {
            String email = userRepository.findByPhoneNumber(phoneNumber)
                    .map(User::getEmail)
                    .orElseThrow(() -> {
                        log.warn("전화번호로 등록된 사용자 없음: phoneNumber={}", phoneNumber);
                        return new UserNotFoundException("해당 휴대폰 번호로 등록된 사용자가 없습니다.");
                    });

            log.info("전화번호로 이메일 조회 완료: phoneNumber={}, email={}", phoneNumber, email);
            return email;

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("전화번호로 이메일 조회 중 시스템 오류: phoneNumber={}", phoneNumber, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "이메일 조회 처리 중 오류가 발생했습니다.", e);
        }
    }

    //로그인 정보 조회
    @Transactional(readOnly = true)
    public LoginInfoDto getLoginInfo(String email) {
        log.info("로그인 정보 조회 시작: email={}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("로그인 정보 조회 실패 - 사용자 없음: email={}", email);
                        return new UserNotFoundException(email);
                    });

            LoginInfoDto loginInfo = new LoginInfoDto(
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getShoeSize() != null ? user.getShoeSize().name() : null,
                    user.isOptionalPrivacyAgreement(), // 개인정보 수집 및 이용 동의 여부
                    user.isPhoneNotificationConsent(), // 문자 메시지 수신 동의 여부
                    user.isEmailNotificationConsent() // 이메일 수신 동의 여부
            );

            log.info("로그인 정보 조회 완료: email={}, userId={}", email, user.getId());
            return loginInfo;

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("로그인 정보 조회 중 시스템 오류: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "로그인 정보 조회 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        log.debug("이메일로 사용자 조회 시작: email={}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("이메일로 사용자 조회 실패: email={}", email);
                        return new UserNotFoundException(email);
                    });

            log.debug("이메일로 사용자 조회 완료: email={}, userId={}", email, user.getId());
            return user;

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("이메일로 사용자 조회 중 시스템 오류: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "사용자 조회 처리 중 오류가 발생했습니다.", e);
        }
    }
}