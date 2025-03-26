package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.OAuthSignupCompleteDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.command.UserUpdateService;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final UserRepository userRepository;
    private final UserUpdateService userUpdateService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;

    /**
     * OAuth 회원가입 추가 정보 입력 API
     */
    @PostMapping("/complete-signup")
    @Transactional
    public ResponseEntity<?> completeOAuthSignup(
            @RequestBody OAuthSignupCompleteDto dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // 임시 토큰에서 이메일 추출
            String email = extractEmailFromToken(dto.getToken());

            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "유효하지 않은 토큰입니다."
                ));
            }

            // 사용자 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 필수 동의 확인
            if (!dto.getTermsAgreement() || !dto.getPrivacyAgreement()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "필수 동의사항에 동의해주세요."
                ));
            }

            // 사용자 정보 업데이트
            user.updateUser(
                    null, // 이메일은 변경하지 않음
                    null, // 비밀번호는 변경하지 않음
                    dto.getShoeSize(),
                    dto.getAdConsent(),
                    dto.getAdConsent(),
                    dto.getAge(),
                    dto.getGender()
            );

            // 전화번호 업데이트
            if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isEmpty()) {
                user.updateLoginInfo(
                        null, null,
                        dto.getPhoneNumber(),
                        null, null, null, null, null
                );
            }

            // 개인정보 동의 업데이트
            user.updateConsent(dto.getAdConsent(), dto.getOptionalPrivacyAgreement());

            // 본인인증 완료 처리
            user.setVerified(true);

            // 추천인 코드 처리
            if (dto.getReferralCode() != null && !dto.getReferralCode().isEmpty()) {
                userRepository.findByReferralCode(dto.getReferralCode())
                        .ifPresent(referrer -> user.addReferrer(referrer));
            }

            userRepository.save(user);

            // IP 주소 추출
            String ipAddress = getClientIp(request);

            // JWT 토큰 생성
            TokenDto tokenDto = jwtTokenProvider.generateTokenPair(
                    user.getEmail(),
                    user.getAge(),
                    user.getGender(),
                    ipAddress,
                    user.getRole()
            );

            // 쿠키에 토큰 설정
            setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), 30 * 60);
            setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), 24 * 60 * 60);

            // 성공 응답
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "회원가입이 완료되었습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OAuth 회원가입 완료 중 오류 발생", e);

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * OAuth 로그인 상태 확인 API
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAuthStatus(HttpServletRequest request) {
        // 토큰 기반으로 인증 상태 확인
        String accessToken = getCookieValue(request, "ACCESS_TOKEN");

        if (accessToken != null) {
            try {
                if (jwtTokenProvider.validateToken(accessToken) &&
                        authRedisService.isAccessTokenValid(accessToken)) {

                    String email = jwtTokenProvider.getEmailFromToken(accessToken);
                    return ResponseEntity.ok(Map.of(
                            "authenticated", true,
                            "email", email
                    ));
                }
            } catch (Exception e) {
                // 토큰 검증 실패는 무시하고 미인증 처리
                log.debug("토큰 검증 실패: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    /**
     * OAuth 연결 목록 조회 API
     */
    @GetMapping("/connections")
    public ResponseEntity<?> getOAuthConnections(HttpServletRequest request) {
        try {
            String accessToken = getCookieValue(request, "ACCESS_TOKEN");
            if (accessToken == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "인증되지 않았습니다."
                ));
            }

            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 연결된 OAuth 제공자 목록
            List<Map<String, Object>> connections = user.getOauthConnections().stream()
                    .map(conn -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("provider", conn.getProvider());
                        map.put("connectedAt", conn.getConnectedAt());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "connections", connections
            ));
        } catch (Exception e) {
            log.error("OAuth 연결 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 임시 토큰에서 이메일 추출
    private String extractEmailFromToken(String token) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(token);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");

            if (parts.length < 2) {
                return null;
            }

            String email = parts[0];
            long timestamp = Long.parseLong(parts[1]);

            // 토큰 유효 시간 체크 (예: 30분)
            if (System.currentTimeMillis() - timestamp > 30 * 60 * 1000) {
                return null; // 만료됨
            }

            return email;
        } catch (Exception e) {
            log.error("임시 토큰 추출 중 오류 발생", e);
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (c.getName().equals(cookieName)) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}