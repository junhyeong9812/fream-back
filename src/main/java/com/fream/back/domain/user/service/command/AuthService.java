package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenDto login(LoginRequestDto loginRequestDto, String ip) {

        // 사용자 조회
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // (원한다면 role, 나이, 성별 등)
        Integer age = user.getAge();
        Gender gender = user.getGender();
        // String role = user.getRole() ... etc.

        // 토큰 발급
        return jwtTokenProvider.generateTokenPair(user.getEmail(), age, gender, ip);
    }
}
