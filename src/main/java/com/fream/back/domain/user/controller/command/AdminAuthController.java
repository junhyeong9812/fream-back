package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.EmailFindRequestDto;
import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.dto.PasswordResetRequestDto;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final UserQueryService userQueryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetService passwordResetService;

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