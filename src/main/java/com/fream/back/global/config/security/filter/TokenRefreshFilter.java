package com.fream.back.global.config.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 토큰 갱신 요청을 처리하는 필터
 * /auth/refresh 경로의 POST 요청을 가로채서 처리
 */
@Slf4j
@RequiredArgsConstructor
public class TokenRefreshFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 토큰 갱신 요청인지 확인
        if (!isRefreshRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 토큰 갱신 처리
            handleTokenRefresh(request, response);
        } catch (Exception e) {
            log.error("토큰 갱신 처리 중 오류 발생", e);
            handleRefreshError(response, e.getMessage());
        }
    }

    /**
     * 토큰 갱신 요청인지 확인
     */
    private boolean isRefreshRequest(HttpServletRequest request) {
        return HttpMethod.POST.name().equals(request.getMethod()) &&
                "/auth/refresh".equals(request.getRequestURI());
    }

    /**
     * 토큰 갱신 처리 로직
     */
    private void handleTokenRefresh(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("토큰 갱신 요청 처리 시작");

        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = getCookieValue(request, "REFRESH_TOKEN");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token이 없습니다.");
        }

        // 2. Refresh Token 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 유효하지 않거나 만료되었습니다.");
        }

        // 3. Redis 화이트리스트 확인
        if (!authRedisService.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 화이트리스트에 없습니다.");
        }

        // 4. 토큰에서 이메일 추출 및 사용자 조회
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        User user = optionalUser.get();

        // 5. 클라이언트 IP 추출
        String clientIp = getClientIp(request);

        // 6. 새로운 토큰 쌍 생성
        TokenDto newTokens = jwtTokenProvider.generateTokenPair(
                user.getEmail(),
                user.getAge(),
                user.getGender(),
                clientIp,
                user.getRole()
        );

        // 7. 새로운 Access Token을 쿠키로 설정
        setCookie(response, "ACCESS_TOKEN", newTokens.getAccessToken(), 30 * 60); // 30분

        // 8. 성공 응답
        handleRefreshSuccess(response, newTokens.getAccessToken());

        log.info("토큰 갱신 완료: {}", email);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
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
     * 쿠키 설정
     */
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

    /**
     * 토큰 갱신 성공 응답
     */
    private void handleRefreshSuccess(HttpServletResponse response, String newAccessToken) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "새로운 Access Token이 발급되었습니다.");
        responseData.put("accessToken", newAccessToken);
        responseData.put("accessTokenExpiry", 30 * 60);
        responseData.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(responseData);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 토큰 갱신 실패 응답
     */
    private void handleRefreshError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", errorMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}