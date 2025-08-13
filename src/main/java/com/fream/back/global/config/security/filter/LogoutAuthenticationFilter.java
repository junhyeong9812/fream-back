package com.fream.back.global.config.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.global.config.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 로그아웃 요청을 처리하는 필터
 * /auth/logout 경로의 POST 요청을 가로채서 처리
 */
@Slf4j
@RequiredArgsConstructor
public class LogoutAuthenticationFilter extends OncePerRequestFilter {

    private final AuthRedisService authRedisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 로그아웃 요청인지 확인
        if (!isLogoutRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 로그아웃 처리
            handleLogout(request, response);
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생", e);
            handleLogoutError(response, e.getMessage());
        }
    }

    /**
     * 로그아웃 요청인지 확인
     */
    private boolean isLogoutRequest(HttpServletRequest request) {
        return HttpMethod.POST.name().equals(request.getMethod()) &&
                "/auth/logout".equals(request.getRequestURI());
    }

    /**
     * 로그아웃 처리 로직
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("로그아웃 요청 처리 시작");

        // 1. 쿠키에서 토큰 추출
        String accessToken = getCookieValue(request, "ACCESS_TOKEN");
        String refreshToken = getCookieValue(request, "REFRESH_TOKEN");

        // 2. Access Token 검증 (선택적)
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                boolean accessValid = jwtTokenProvider.validateToken(accessToken);
                if (accessValid) {
                    String email = jwtTokenProvider.getEmailFromToken(accessToken);
                    log.info("사용자 로그아웃: {}", email);
                }
            } catch (Exception e) {
                log.warn("로그아웃 시 토큰 검증 실패 (무시하고 진행): {}", e.getMessage());
            }
        }

        // 3. Redis에서 토큰 제거 (화이트리스트 무효화)
        if (accessToken != null && !accessToken.isBlank()) {
            authRedisService.removeAccessToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            authRedisService.removeRefreshToken(refreshToken);
        }

        // 4. 쿠키 만료 (삭제)
        expireCookie(response, "ACCESS_TOKEN");
        expireCookie(response, "REFRESH_TOKEN");

        // 5. 성공 응답
        handleLogoutSuccess(response);

        log.info("로그아웃 처리 완료");
    }

    /**
     * 쿠키 값 조회
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 쿠키 만료 (삭제)
     */
    private void expireCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("None")
                .secure(true)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 로그아웃 성공 응답
     */
    private void handleLogoutSuccess(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "로그아웃이 완료되었습니다.");
        responseData.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(responseData);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 로그아웃 실패 응답
     */
    private void handleLogoutError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "로그아웃 처리 중 오류가 발생했습니다: " + errorMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}