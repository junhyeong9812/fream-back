package com.fream.back.global.config.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.service.command.AuthService;
import com.fream.back.global.config.security.dto.TokenDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

/**
 * 로그인 요청을 처리하는 필터 (AuthService 활용)
 * /auth/login 경로의 POST 요청을 가로채서 처리
 */
@Slf4j
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 로그인 요청인지 확인
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 요청 본문에서 로그인 정보 추출
            LoginRequestDto loginRequest = extractLoginRequest(request);

            // AuthService를 통한 로그인 처리
            handleLogin(loginRequest, request, response);

        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            handleLoginError(response, e.getMessage());
        }
    }

    /**
     * 로그인 요청인지 확인
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        return HttpMethod.POST.name().equals(request.getMethod()) &&
                "/auth/login".equals(request.getRequestURI());
    }

    /**
     * 요청 본문에서 로그인 정보 추출
     */
    private LoginRequestDto extractLoginRequest(HttpServletRequest request) throws IOException {
        return objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);
    }

    /**
     * AuthService를 활용한 로그인 처리
     */
    private void handleLogin(LoginRequestDto loginRequest,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {

        log.info("로그인 필터 처리 시작: {}", loginRequest.getEmail());

        // IP 주소 추출
        String clientIp = getClientIp(request);

        // AuthService의 login 메소드 활용
        TokenDto tokenDto = authService.login(loginRequest, clientIp);

        // 쿠키에 토큰 설정
        setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), 30 * 60); // 30분
        setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), 24 * 60 * 60); // 24시간

        // 성공 응답
        handleLoginSuccess(response);

        log.info("로그인 필터 처리 완료: {}", loginRequest.getEmail());
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
     * 로그인 성공 응답
     */
    private void handleLoginSuccess(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "로그인이 성공했습니다.");
        responseData.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(responseData);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 로그인 실패 응답 (AuthService의 예외를 그대로 활용)
     */
    private void handleLoginError(HttpServletResponse response, String errorMessage) throws IOException {
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