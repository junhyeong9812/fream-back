package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.dto.LoginInfoUpdateDto;
import com.fream.back.domain.user.entity.ShoeSize;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.DuplicateEmailException;
import com.fream.back.domain.user.exception.InvalidPasswordException;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import com.fream.back.domain.user.exception.UserNotFoundException;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthRedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void updateLoginInfo(String email, LoginInfoUpdateDto dto) {
        log.info("로그인 정보 업데이트 시작: email={}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("로그인 정보 업데이트 실패 - 사용자 없음: email={}", email);
                        return new UserNotFoundException(email);
                    });

            // 이메일 변경 시 중복 확인
            if (dto.getNewEmail() != null && !dto.getNewEmail().equals(email)) {
                if (userRepository.existsByEmail(dto.getNewEmail())) {
                    log.warn("이메일 변경 시 중복 발견: currentEmail={}, newEmail={}", email, dto.getNewEmail());
                    throw new DuplicateEmailException(dto.getNewEmail());
                }
            }

            // 비밀번호 변경 로직
            if (dto.getPassword() != null && dto.getNewPassword() != null) {
                // 현재 비밀번호 확인
                if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                    log.warn("현재 비밀번호 불일치: email={}", email);
                    throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
                }
                // 새 비밀번호 설정
                user.updateLoginInfo(null, passwordEncoder.encode(dto.getNewPassword()), null, null, null, null, null, null);
                log.info("비밀번호 변경 완료: email={}", email);
            }

            // ShoeSize 변환
            ShoeSize newShoeSize = null;
            if (dto.getNewShoeSize() != null) {
                try {
                    newShoeSize = ShoeSize.valueOf(dto.getNewShoeSize());
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 신발 사이즈: email={}, shoeSize={}", email, dto.getNewShoeSize());
                    throw new UserException(UserErrorCode.INVALID_VERIFICATION_CODE, "잘못된 신발 사이즈입니다: " + dto.getNewShoeSize());
                }
            }

            // 업데이트 호출
            user.updateLoginInfo(dto.getNewEmail(),
                    dto.getNewPassword() != null ? passwordEncoder.encode(dto.getNewPassword()) : null,
                    dto.getNewPhoneNumber(),
                    newShoeSize,
                    dto.getAdConsent(),
                    dto.getPrivacyConsent(),
                    dto.getSmsConsent(),
                    dto.getEmailConsent());

            log.info("로그인 정보 업데이트 완료: email={}, userId={}", email, user.getId());

        } catch (UserException e) {
            log.warn("로그인 정보 업데이트 실패 - 비즈니스 로직 오류: email={}, error={}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("로그인 정보 업데이트 실패 - 시스템 오류: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "로그인 정보 업데이트 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public TokenDto reissueTokenAfterEmailChange(
            String oldAccessToken,
            String oldRefreshToken,
            String oldEmail,
            String newEmail,
            String ip
    ) {
        log.info("이메일 변경 후 토큰 재발급 시작: oldEmail={}, newEmail={}, ip={}",
                oldEmail, newEmail, ip);

        try {
            // 1) newEmail 로 DB에서 사용자 찾기
            User newUser = userRepository.findByEmail(newEmail)
                    .orElseThrow(() -> {
                        log.warn("토큰 재발급 실패 - 새 이메일로 사용자 없음: newEmail={}", newEmail);
                        return new UserNotFoundException(newEmail);
                    });

            // 2) Redis에서 old 토큰들 제거
            try {
                if (oldAccessToken != null) {
                    redisService.removeAccessToken(oldAccessToken);
                    log.debug("기존 Access Token 제거 완료");
                }
                if (oldRefreshToken != null) {
                    redisService.removeRefreshToken(oldRefreshToken);
                    log.debug("기존 Refresh Token 제거 완료");
                }
            } catch (Exception e) {
                log.warn("기존 토큰 제거 중 오류 발생", e);
                // 토큰 제거 실패는 새 토큰 발급을 중단시키지 않음
            }

            // 3) 새 이메일 + User 정보로 토큰 재발급
            //    User에 age, gender 필드가 있다고 가정
            TokenDto tokenDto = jwtTokenProvider.generateTokenPair(
                    newUser.getEmail(),
                    newUser.getAge(),
                    newUser.getGender(),
                    ip,
                    newUser.getRole()
            );

            log.info("이메일 변경 후 토큰 재발급 완료: oldEmail={}, newEmail={}, userId={}",
                    oldEmail, newEmail, newUser.getId());

            return tokenDto;

        } catch (UserException e) {
            log.warn("토큰 재발급 실패 - 비즈니스 로직 오류: oldEmail={}, newEmail={}, error={}",
                    oldEmail, newEmail, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("토큰 재발급 실패 - 시스템 오류: oldEmail={}, newEmail={}", oldEmail, newEmail, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 재발급 처리 중 오류가 발생했습니다.", e);
        }
    }
}