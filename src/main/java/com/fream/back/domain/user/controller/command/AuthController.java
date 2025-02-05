package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.service.command.AuthService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final UserQueryService userQueryService;
    private final AuthService authService;

    /**
     * [POST] /auth/login
     * - JWT 발급 후 쿠키로 내려줌 (ACCESS_TOKEN, REFRESH_TOKEN)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            // 1) IP 추출
            String ip = getClientIp(request);

            // 2) 로그인 (토큰 발급)
            TokenDto tokenDto = authService.login(loginRequestDto, ip);

            // 3) Access/Refresh 토큰을 쿠키에 설정
            long accessTokenExpireSeconds = 30 * 60; // 예: 30분
            setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), accessTokenExpireSeconds);

            long refreshTokenExpireSeconds = 24 * 60 * 60; // 예: 1일
            setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), refreshTokenExpireSeconds);

            // 4) 응답 (바디에 토큰을 담지 않아도 됨)
            return ResponseEntity.ok("로그인 성공 (JWT 쿠키 발급)");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("로그인 처리 중 오류 발생");
        }
    }

    /**
     * [POST] /auth/refresh
     * - REFRESH_TOKEN 쿠키로부터 새 ACCESS_TOKEN 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1) 쿠키에서 Refresh Token 꺼내기
            String refreshToken = getCookieValue(request, "REFRESH_TOKEN");
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(400).body("No Refresh Token cookie");
            }

            // 2) 유효성 검사
            boolean valid = jwtTokenProvider.validateToken(refreshToken);
            if (!valid) {
                return ResponseEntity.status(401).body("Refresh Token is invalid or expired");
            }

            // 3) Redis 화이트리스트 확인
            if (!authRedisService.isRefreshTokenValid(refreshToken)) {
                return ResponseEntity.status(401).body("Refresh Token is not in the whitelist");
            }

            // 4) DB에서 유저 조회
            String email = jwtTokenProvider.getEmailFromToken(refreshToken);
            User user = userQueryService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }

            // 5) 새 Access 토큰 발급
            Integer age = user.getAge();
            Gender gender = user.getGender();
            String ip = getClientIp(request);

            TokenDto newTokens = jwtTokenProvider.generateTokenPair(email, age, gender, ip);

            // 6) 새 Access 토큰을 쿠키로 내려줌 (Refresh 토큰은 그대로)
            long accessTokenExpireSeconds = 30 * 60;
            setCookie(response, "ACCESS_TOKEN", newTokens.getAccessToken(), accessTokenExpireSeconds);

            // 7) 응답
            return ResponseEntity.ok("새 Access Token 발급");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Refresh 처리 중 오류: " + e.getMessage());
        }
    }

    /**
     * [POST] /auth/logout
     * - 쿠키 만료 + Redis(화이트리스트)에서 제거
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        try {
            // 1) ACCESS_TOKEN, REFRESH_TOKEN 쿠키 값
            String accessToken = getCookieValue(request, "ACCESS_TOKEN");
            String refreshToken = getCookieValue(request, "REFRESH_TOKEN");

            // 2) (선택) Access Token 검증
            if (accessToken != null && !accessToken.isBlank()) {
                boolean accessValid = jwtTokenProvider.validateToken(accessToken);
                if (!accessValid) {
                    return ResponseEntity.badRequest().body("Access Token invalid or expired");
                }
            }
            // 3) Redis에서 삭제 (화이트리스트 무효화)
            if (accessToken != null) {
                authRedisService.removeAccessToken(accessToken);
            }
            if (refreshToken != null && !refreshToken.isBlank()) {
                authRedisService.removeRefreshToken(refreshToken);
            }

            // 4) 쿠키 만료 (삭제)
            expireCookie(response, "ACCESS_TOKEN");
            expireCookie(response, "REFRESH_TOKEN");

            return ResponseEntity.ok("로그아웃 성공");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("로그아웃 처리 중 오류");
        }
    }

    /**
     * =====================================
     * Private Helper Methods
     * =====================================
     */

    // 요청 헤더/프록시에서 클라이언트 IP 얻기
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // 쿠키 생성 -> response에 추가 (HttpOnly, Secure, SameSite)
    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)            // JS로 접근 불가
                .secure(false)             // HTTPS 사용시 true
                .sameSite("None")          // CrossSite 요청 고려 시 "None"
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 쿠키 만료
    private void expireCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .secure(false)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 쿠키 값 조회
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
}
