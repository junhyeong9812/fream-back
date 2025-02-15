package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.LoginInfoDto;
import com.fream.back.domain.user.dto.LoginInfoUpdateDto;
import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.service.command.UserCommandService;
import com.fream.back.domain.user.service.command.UserUpdateService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.dto.TokenDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;
    private final UserUpdateService userUpdateService;
    private final UserQueryService userQueryService;
    private final AuthRedisService authRedisService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto dto) {
        // 회원가입 로직
        User user = userCommandService.registerUser(dto);
        return ResponseEntity.ok("회원가입 성공: " + user.getEmail());
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUserInfo(
            @RequestBody LoginInfoUpdateDto updateDto,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // IP 주소 추출
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }

            // SecurityContext에서 현재 이메일 추출
            String currentEmail = SecurityUtils.extractEmailFromSecurityContext();

            // 쿠키에서 토큰들 추출
            String accessToken = getCookieValue(request, "ACCESS_TOKEN");
            String refreshToken = getCookieValue(request, "REFRESH_TOKEN");

            // 이메일 변경 여부 확인
            boolean isEmailChanged = updateDto.getNewEmail() != null &&
                    !updateDto.getNewEmail().equals(currentEmail);

            // 사용자 정보 업데이트
            userUpdateService.updateLoginInfo(currentEmail, updateDto);

            // 변경된 정보 조회
            LoginInfoDto updatedInfo = userQueryService.getLoginInfo(
                    isEmailChanged ? updateDto.getNewEmail() : currentEmail
            );

            // 이메일이 변경된 경우 토큰 재발급
            if (isEmailChanged) {
                // 기존 토큰들 Redis에서 제거
                if (accessToken != null) {
                    authRedisService.removeAccessToken(accessToken);
                }
                if (refreshToken != null) {
                    authRedisService.removeRefreshToken(refreshToken);
                }

                // 새로운 토큰 발급
                TokenDto newTokens = userUpdateService.reissueTokenAfterEmailChange(
                        null,
                        null,
                        currentEmail,
                        updateDto.getNewEmail(),
                        ip
                );

                // 새로운 토큰을 쿠키에 설정
                setCookie(response, "ACCESS_TOKEN", newTokens.getAccessToken(), 30 * 60); // 30분
                setCookie(response, "REFRESH_TOKEN", newTokens.getRefreshToken(), 24 * 60 * 60); // 1일
            }

            return ResponseEntity.ok(updatedInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "사용자 정보 업데이트 중 오류가 발생했습니다."
            ));
        }
    }

    // 쿠키 관련 유틸리티 메서드들
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

