package com.fream.back.domain.user.controller.command;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 컨트롤러 (DEPRECATED)
 *
 * 주의: 이 컨트롤러의 주요 기능들은 이제 Security Filter에서 처리됩니다.
 * - 로그인: LoginAuthenticationFilter
 * - 로그아웃: LogoutAuthenticationFilter
 * - 토큰 갱신: TokenRefreshFilter
 *
 * 호환성을 위해 일부 엔드포인트만 유지하되, 가능한 한 필터를 사용하세요.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Deprecated
public class AuthController {

    /**
     * [DEPRECATED] 로그인 엔드포인트
     * 이제 LoginAuthenticationFilter에서 처리됩니다.
     */
    @PostMapping("/login")
    @Deprecated
    public ResponseEntity<?> login() {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "ENDPOINT_DEPRECATED",
                "message", "이 엔드포인트는 더 이상 사용되지 않습니다. LoginAuthenticationFilter가 자동으로 처리합니다.",
                "suggestion", "POST /auth/login 요청을 그대로 보내세요. 필터가 자동으로 처리합니다."
        ));
    }

    /**
     * [DEPRECATED] 로그아웃 엔드포인트
     * 이제 LogoutAuthenticationFilter에서 처리됩니다.
     */
    @PostMapping("/logout")
    @Deprecated
    public ResponseEntity<?> logout() {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "ENDPOINT_DEPRECATED",
                "message", "이 엔드포인트는 더 이상 사용되지 않습니다. LogoutAuthenticationFilter가 자동으로 처리합니다.",
                "suggestion", "POST /auth/logout 요청을 그대로 보내세요. 필터가 자동으로 처리합니다."
        ));
    }

    /**
     * [DEPRECATED] 토큰 갱신 엔드포인트
     * 이제 TokenRefreshFilter에서 처리됩니다.
     */
    @PostMapping("/refresh")
    @Deprecated
    public ResponseEntity<?> refresh() {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "ENDPOINT_DEPRECATED",
                "message", "이 엔드포인트는 더 이상 사용되지 않습니다. TokenRefreshFilter가 자동으로 처리합니다.",
                "suggestion", "POST /auth/refresh 요청을 그대로 보내세요. 필터가 자동으로 처리합니다."
        ));
    }

    /**
     * 사용자 이메일 조회 (유지)
     * 이 엔드포인트는 계속 사용 가능합니다.
     */
    @GetMapping("/email")
    public ResponseEntity<?> getUserEmail() {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "NOT_IMPLEMENTED",
                "message", "이 기능은 아직 필터로 이동되지 않았습니다.",
                "suggestion", "SecurityContext에서 직접 이메일을 추출하거나 별도 API를 사용하세요."
        ));
    }

    /**
     * 인증 상태 확인 (새로 추가)
     * JWT 토큰이 유효한지 확인합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAuthStatus() {
        // SecurityContext를 통해 인증 상태 확인 로직 구현
        // 이 부분은 서비스 레이어에서 구현 필요
        return ResponseEntity.ok(Map.of(
                "message", "인증 상태 확인 기능은 별도 구현이 필요합니다.",
                "suggestion", "SecurityContext 또는 JWT 검증 로직을 사용하세요."
        ));
    }
}