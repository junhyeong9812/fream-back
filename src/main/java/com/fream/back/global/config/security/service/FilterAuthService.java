package com.fream.back.global.config.security.service;

import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 필터에서 사용할 인증 관련 서비스
 * 기존 AuthService와 유사하지만 필터 환경에 최적화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;

    /**
     * 필터용 로그인 처리
     * AuthService의 login 메소드와 동일한 로직이지만 예외 처리 방식이 다름
     */
    public TokenDto authenticateUser(LoginRequestDto loginRequest, String ip) {
        log.info("필터 인증 시작: email={}, ip={}", loginRequest.getEmail(), ip);

        // 1. 사용자 조회
        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());
        if (optionalUser.isEmpty()) {
            log.warn("로그인 실패 - 사용자 없음: email={}", loginRequest.getEmail());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = optionalUser.get();

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치: email={}", loginRequest.getEmail());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 3. 계정 상태 검증
        if (!user.isActive()) {
            log.warn("로그인 실패 - 비활성 계정: email={}", loginRequest.getEmail());
            throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        if (!user.isVerified()) {
            log.warn("로그인 실패 - 미인증 계정: email={}", loginRequest.getEmail());
            throw new IllegalArgumentException("이메일 인증이 완료되지 않은 계정입니다.");
        }

        // 4. JWT 토큰 생성
        try {
            TokenDto tokenDto = jwtTokenProvider.generateTokenPair(
                    user.getEmail(),
                    user.getAge(),
                    user.getGender(),
                    ip,
                    user.getRole()
            );

            log.info("필터 인증 성공: email={}, userId={}, role={}",
                    user.getEmail(), user.getId(), user.getRole());

            return tokenDto;
        } catch (Exception e) {
            log.error("토큰 생성 실패: email={}", loginRequest.getEmail(), e);
            throw new IllegalArgumentException("로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 토큰 갱신 처리
     */
    public TokenDto refreshAccessToken(String refreshToken, String ip) {
        log.info("필터 토큰 갱신 시작: ip={}", ip);

        // 1. Refresh Token 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 유효하지 않거나 만료되었습니다.");
        }

        // 2. Redis 화이트리스트 확인
        if (!authRedisService.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 화이트리스트에 없습니다.");
        }

        // 3. 토큰에서 이메일 추출 및 사용자 조회
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        User user = optionalUser.get();

        // 4. 새로운 토큰 쌍 생성
        try {
            TokenDto newTokens = jwtTokenProvider.generateTokenPair(
                    user.getEmail(),
                    user.getAge(),
                    user.getGender(),
                    ip,
                    user.getRole()
            );

            log.info("필터 토큰 갱신 완료: email={}", email);
            return newTokens;
        } catch (Exception e) {
            log.error("토큰 갱신 실패: email={}", email, e);
            throw new IllegalArgumentException("토큰 갱신 중 오류가 발생했습니다.");
        }
    }

    /**
     * 로그아웃 처리 (토큰 무효화)
     */
    public void logout(String accessToken, String refreshToken) {
        log.info("필터 로그아웃 처리 시작");

        try {
            // Redis에서 토큰 제거
            if (accessToken != null && !accessToken.isBlank()) {
                authRedisService.removeAccessToken(accessToken);
                log.debug("Access Token 제거 완료");
            }
            if (refreshToken != null && !refreshToken.isBlank()) {
                authRedisService.removeRefreshToken(refreshToken);
                log.debug("Refresh Token 제거 완료");
            }

            log.info("필터 로그아웃 처리 완료");
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            throw new IllegalArgumentException("로그아웃 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 검증 (토큰 없이 이메일로만)
     */
    public boolean validateUser(String email) {
        try {
            Optional<User> user = userRepository.findByEmail(email);
            return user.isPresent() && user.get().isActive() && user.get().isVerified();
        } catch (Exception e) {
            log.error("사용자 검증 중 오류: email={}", email, e);
            return false;
        }
    }
}