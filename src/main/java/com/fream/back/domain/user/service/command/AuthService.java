package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.InvalidPasswordException;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import com.fream.back.domain.user.exception.UserNotFoundException;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenDto login(LoginRequestDto loginRequestDto, String ip) {
        String email = loginRequestDto.getEmail();

        log.info("로그인 시도: email={}, ip={}", email, ip);

        try {
            // 사용자 조회
            User user = userRepository.findByEmail(loginRequestDto.getEmail())
                    .orElseThrow(() -> {
                        log.warn("로그인 실패 - 사용자 없음: email={}, ip={}", email, ip);
                        return new UserNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다.");
                    });

            // 비밀번호 검증
            if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
                log.warn("로그인 실패 - 비밀번호 불일치: email={}, ip={}", email, ip);
                throw new InvalidPasswordException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // 계정 상태 검증
            if (!user.isActive()) {
                log.warn("로그인 실패 - 비활성 계정: email={}, ip={}, isActive={}", email, ip, user.isActive());
                throw new UserException(UserErrorCode.ACCOUNT_INACTIVE);
            }

            // (원한다면 role, 나이, 성별 등)
            Integer age = user.getAge();
            Gender gender = user.getGender();
            Role role = user.getRole();

            // 토큰 발급
            TokenDto tokenDto;
            try {
                tokenDto = jwtTokenProvider.generateTokenPair(user.getEmail(), age, gender, ip, role);
                log.debug("토큰 생성 완료: email={}, role={}", email, role);
            } catch (Exception e) {
                log.error("토큰 생성 실패: email={}, ip={}", email, ip, e);
                throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 생성 중 오류가 발생했습니다.", e);
            }

            log.info("로그인 성공: email={}, userId={}, role={}, ip={}",
                    email, user.getId(), role, ip);

            return tokenDto;

        } catch (UserException e) {
            log.warn("로그인 실패 - 비즈니스 로직 오류: email={}, ip={}, error={}", email, ip, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("로그인 실패 - 시스템 오류: email={}, ip={}", email, ip, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "로그인 처리 중 오류가 발생했습니다.", e);
        }
    }
}