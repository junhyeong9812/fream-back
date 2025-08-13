package com.fream.back.global.config.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.user.dto.LoginRequestDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 로그인 요청을 처리하는 필터
 * /auth/login 경로의 POST 요청을 가로채서 처리
 */
@Slf4j
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
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

            // 로그인 처리
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
     * 로그인 처리 로직
     */
    private void handleLogin(LoginRequestDto loginRequest,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {

        log.info("로그인 시도: {}", loginRequest.getEmail());

        // 1. 사용자 조회
        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        User user = optionalUser.get();

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 사용자 상태 검증 (필요시 추가)
        if (!user.isVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않은 사용자입니다.");
        }

        // 4. IP 주소 추출
        String clientIp = getClientIp(request);

        // 5. JWT 토큰 생성
        TokenDto tokenDto = jwtTokenProvider.generateTokenPair(
                user.getEmail(),
                user.getAge(),
                user.getGender(),
                clientIp,
                user.getRole()
        );

        // 6. 쿠키에 토큰 설정
        setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), 30 * 60); // 30분
        setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), 24 * 60 * 60); // 24시간

        // 7. 성공 응답
        handleLoginSuccess(response);

        log.info("로그인 성공: {}", user.getEmail());
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
     * 로그인 실패 응답
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