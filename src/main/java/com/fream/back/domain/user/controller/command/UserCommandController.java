package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.LoginInfoDto;
import com.fream.back.domain.user.dto.LoginInfoUpdateDto;
import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.service.command.IdentityVerificationService;
import com.fream.back.domain.user.service.command.UserCommandService;
import com.fream.back.domain.user.service.command.UserUpdateService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.dto.TokenDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;
    private final UserUpdateService userUpdateService;
    private final UserQueryService userQueryService;
    private final AuthRedisService authRedisService;
    private final IdentityVerificationService identityVerificationService; // 직접 주입

    /**
     * 회원가입 API
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto dto) {
        log.info("회원가입 요청: {}, 본인인증 ID: {}", dto.getEmail(), dto.getIdentityVerificationId());

        try {
            // 회원가입 로직 (본인인증 포함)
            User user = userCommandService.registerUser(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // 유효성 검증 실패
            log.warn("회원가입 유효성 검증 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            // 기타 오류
            log.error("회원가입 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원가입 처리 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 본인인증 검증 API (프론트엔드 테스트용)
     */
    @PostMapping("/verify-identity")
    public ResponseEntity<?> verifyIdentity(@RequestBody Map<String, String> request) {
        String identityVerificationId = request.get("identityVerificationId");

        if (identityVerificationId == null || identityVerificationId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "본인인증 ID가 필요합니다."
            ));
        }

        try {
            // 본인인증 서비스를 통해 검증 (직접 주입받은 서비스 사용)
            var verifiedCustomer = identityVerificationService.verifyIdentity(identityVerificationId);

            // 성공 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "본인인증이 완료되었습니다.");
            response.put("data", verifiedCustomer);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("본인인증 검증 중 오류 발생", e);

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<LoginInfoDto> updateUserInfo(
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
                String accessToken = getCookieValue(request, "ACCESS_TOKEN");
                String refreshToken = getCookieValue(request, "REFRESH_TOKEN");

                // 기존 토큰들 Redis에서 제거
                if (accessToken != null) {
                    authRedisService.removeAccessToken(accessToken);
                }
                if (refreshToken != null) {
                    authRedisService.removeRefreshToken(refreshToken);
                }

                // 새로운 토큰 발급 및 쿠키 설정
                TokenDto newTokens = userUpdateService.reissueTokenAfterEmailChange(
                        null,
                        null,
                        currentEmail,
                        updateDto.getNewEmail(),
                        ip
                );

                setCookie(response, "ACCESS_TOKEN", newTokens.getAccessToken(), 30 * 60);
                setCookie(response, "REFRESH_TOKEN", newTokens.getRefreshToken(), 24 * 60 * 60);
            }

            return ResponseEntity.ok(updatedInfo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
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