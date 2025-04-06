package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.EmailFindRequestDto;
import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.dto.PasswordResetRequestDto;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.service.command.AuthService;
import com.fream.back.domain.user.service.command.PasswordResetService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final UserQueryService userQueryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetService passwordResetService;
    private final AuthRedisService authRedisService;

    /**
     * [POST] /admin/auth/login
     * - 관리자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequestDto loginRequestDto,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        try {
            // 1) IP 추출
            String ip = getClientIp(request);

            // 2) 일반 로그인 로직 실행 (기존 AuthService 활용)
            TokenDto tokenDto = authService.login(loginRequestDto, ip);

            // 3) 관리자 권한 확인
            String email = jwtTokenProvider.getEmailFromToken(tokenDto.getAccessToken());
            userQueryService.checkAdminRole(email); // 관리자 아니면 예외 발생

            // 4) 쿠키 설정
            long accessTokenExpireSeconds = 30 * 60; // 30분
            setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), accessTokenExpireSeconds);

            long refreshTokenExpireSeconds = 24 * 60 * 60; // 1일
            setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), refreshTokenExpireSeconds);

            // 5) 응답
            return ResponseEntity.ok("관리자 로그인 성공");
        } catch (SecurityException e) {
            // 관리자 권한 없음
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 로그인 정보 불일치
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("관리자 로그인 처리 중 오류 발생");
        }
    }

    /**
     * [POST] /admin/auth/find-email
     * - 관리자 이메일 찾기
     */
    @PostMapping("/find-email")
    public ResponseEntity<?> findAdminEmail(@RequestBody EmailFindRequestDto requestDto) {
        try {
            // 1) 전화번호로 이메일 조회
            String email = userQueryService.findEmailByPhoneNumber(requestDto);

            // 2) 관리자 권한 확인
            try {
                userQueryService.checkAdminRole(email);
            } catch (SecurityException e) {
                return ResponseEntity.status(403).body("관리자 계정이 아닙니다.");
            }

            return ResponseEntity.ok(email);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("일치하는 관리자 계정을 찾을 수 없습니다.");
        }
    }
    /**
     * [GET] /admin/auth/check
     * - 관리자 토큰 유효성 확인
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAdminToken(HttpServletRequest request) {
        try {
            // 1) 쿠키에서 Access Token 꺼내기
            String accessToken = getCookieValue(request, "ACCESS_TOKEN");
            if (accessToken == null || accessToken.isBlank()) {
                return ResponseEntity.status(401).body("토큰이 없습니다");
            }

            // 2) 유효성 검사
            boolean valid = jwtTokenProvider.validateToken(accessToken);
            if (!valid) {
                return ResponseEntity.status(401).body("토큰이 유효하지 않거나 만료되었습니다");
            }

            // 3) 관리자 권한 확인
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            try {
                userQueryService.checkAdminRole(email); // 관리자 아니면 예외 발생
            } catch (SecurityException e) {
                return ResponseEntity.status(403).body("관리자 권한이 없습니다");
            }

            return ResponseEntity.ok("토큰이 유효합니다");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("토큰 검증 중 오류: " + e.getMessage());
        }
    }

    /**
     * [POST] /admin/auth/refresh
     * - 관리자 전용 토큰 갱신 (REFRESH_TOKEN 쿠키로부터 새 ACCESS_TOKEN 발급)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAdminToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1) 쿠키에서 Refresh Token 꺼내기
            String refreshToken = getCookieValue(request, "REFRESH_TOKEN");
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(400).body("리프레시 토큰이 없습니다");
            }

            // 2) 유효성 검사
            boolean valid = jwtTokenProvider.validateToken(refreshToken);
            if (!valid) {
                return ResponseEntity.status(401).body("리프레시 토큰이 유효하지 않거나 만료되었습니다");
            }

            // 3) Redis 화이트리스트 확인
            if (!authRedisService.isRefreshTokenValid(refreshToken)) {
                return ResponseEntity.status(401).body("리프레시 토큰이 화이트리스트에 없습니다");
            }

            // 4) DB에서 유저 조회
            String email = jwtTokenProvider.getEmailFromToken(refreshToken);
            User user = userQueryService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다");
            }

            // 5) 관리자 권한 확인
            try {
                userQueryService.checkAdminRole(email); // 관리자 아니면 예외 발생
            } catch (SecurityException e) {
                return ResponseEntity.status(403).body("관리자 권한이 없습니다");
            }

            // 6) 새 토큰 페어 발급
            Integer age = user.getAge();
            Gender gender = user.getGender();
            String ip = getClientIp(request);
            Role role = user.getRole();

            TokenDto newTokens = jwtTokenProvider.generateTokenPair(email, age, gender, ip, role);

            // 7) 새 Access 토큰을 쿠키로 내려줌
            long accessTokenExpireSeconds = 30 * 60; // 30분
            setCookie(response, "ACCESS_TOKEN", newTokens.getAccessToken(), accessTokenExpireSeconds);

            // 8) 응답 데이터 생성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "관리자 토큰 갱신 성공");
            responseData.put("accessToken", newTokens.getAccessToken());
            responseData.put("accessTokenExpiry", accessTokenExpireSeconds);

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("토큰 갱신 처리 중 오류: " + e.getMessage());
        }
    }

    /**
     * [POST] /admin/auth/find-password
     * - 관리자 비밀번호 찾기 (임시 비밀번호 발급)
     */
    @PostMapping("/find-password")
    public ResponseEntity<?> findAdminPassword(@RequestBody PasswordResetRequestDto requestDto) {
        try {
            // 1) 이메일로 사용자 확인
            User user = userQueryService.findByEmail(requestDto.getEmail());

            // 2) 관리자 확인
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body("관리자 계정이 아닙니다.");
            }

            // 3) 전화번호 일치 확인
            if (!user.getPhoneNumber().equals(requestDto.getPhoneNumber())) {
                return ResponseEntity.status(400).body("전화번호가 일치하지 않습니다.");
            }

            // 4) PasswordResetService를 사용하여 임시 비밀번호 생성 및 이메일 발송
            boolean result = passwordResetService.AdminCheckPasswordResetAndSendEmail(
                    requestDto.getEmail(),
                    requestDto.getPhoneNumber()
            );

            if (result) {
                return ResponseEntity.ok("임시 비밀번호가 이메일로 발송되었습니다.");
            } else {
                return ResponseEntity.status(500).body("비밀번호 재설정 중 오류가 발생했습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(404).body("관리자 계정을 찾을 수 없습니다.");
        }
    }

    /**
     * [POST] /admin/auth/logout
     * - 관리자 로그아웃: 쿠키 만료 + Redis(화이트리스트)에서 제거
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        try {
            // 1) ACCESS_TOKEN, REFRESH_TOKEN 쿠키 값
            String accessToken = getCookieValue(request, "ACCESS_TOKEN");
            String refreshToken = getCookieValue(request, "REFRESH_TOKEN");

            // 2) Access Token 검증 및 관리자 권한 확인
            if (accessToken != null && !accessToken.isBlank()) {
                boolean accessValid = jwtTokenProvider.validateToken(accessToken);
                if (!accessValid) {
                    return ResponseEntity.status(401).body("토큰이 유효하지 않거나 만료되었습니다");
                }

                // 관리자 권한 확인 (선택적)
                try {
                    String email = jwtTokenProvider.getEmailFromToken(accessToken);
                    userQueryService.checkAdminRole(email); // 관리자 아니면 예외 발생
                } catch (SecurityException e) {
                    // 권한 검증 실패해도 로그아웃은 진행
                    // 로그 기록만 남김
                    System.out.println("Warning: 관리자가 아닌 사용자의 로그아웃 시도 - " + e.getMessage());
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

            return ResponseEntity.ok("관리자 로그아웃 성공");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("로그아웃 처리 중 오류: " + e.getMessage());
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
                .secure(true)             // HTTPS 사용시 true
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