package com.fream.back.global.config.websocket;

import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.exception.security.InvalidTokenException;
import com.fream.back.global.exception.security.TokenNotFoundException;
import com.fream.back.global.exception.websocket.WebSocketAuthenticationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 웹소켓 인증 인터셉터
 * 웹소켓 연결 전/후에 실행되어 인증 처리를 담당합니다.
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public WebSocketAuthInterceptor(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 웹소켓 핸드셰이크 전 실행되는 메소드
     * 사용자 인증을 처리합니다.
     *
     * @param request 서버 HTTP 요청
     * @param response 서버 HTTP 응답
     * @param wsHandler 웹소켓 핸들러
     * @param attributes 웹소켓 세션 속성
     * @return 핸드셰이크 진행 여부 (true: 진행, false: 중단)
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().toString() : "알 수 없음";
        String uri = request.getURI().toString();

        log.info("웹소켓 핸드셰이크 시작: 요청 경로={}, 요청 IP={}", uri, ip);

        try {
            // 쿠키에서 Access Token 추출
            String accessToken = extractAccessToken(request);
            if (accessToken == null) {
                log.warn("웹소켓 인증 실패: 액세스 토큰 없음 (IP={})", ip);
                throw new TokenNotFoundException();
            }

            log.debug("액세스 토큰 추출 성공");

            // 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(accessToken)) {
                log.warn("웹소켓 인증 실패: 유효하지 않은 토큰 (IP={})", ip);
                throw new InvalidTokenException();
            }

            log.debug("토큰 유효성 검증 성공");

            // 토큰에서 이메일 추출
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            log.debug("토큰에서 이메일 추출 성공: {}", email);

            // 이메일이 존재하는지 확인
            userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("웹소켓 인증 실패: 존재하지 않는 사용자 - 이메일={}, IP={}", email, ip);
                        return new WebSocketAuthenticationException("사용자를 찾을 수 없습니다.");
                    });

            // WebSocket 세션에 이메일 저장
            attributes.put("email", email);

            log.info("웹소켓 핸드셰이크 성공: 사용자 Email={}, IP={}", email, ip);
            return true;
        } catch (Exception e) {
            log.error("웹소켓 핸드셰이크 실패: 요청 경로={}, 요청 IP={}, 원인={}", uri, ip, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 웹소켓 핸드셰이크 후 실행되는 메소드
     *
     * @param request 서버 HTTP 요청
     * @param response 서버 HTTP 응답
     * @param wsHandler 웹소켓 핸들러
     * @param exception 발생한 예외 (있는 경우)
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        String uri = request.getURI().toString();
        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().toString() : "알 수 없음";

        if (exception != null) {
            log.error("웹소켓 연결 실패: 요청 경로={}, 요청 IP={}, 원인={}", uri, ip, exception.getMessage(), exception);
            return;
        }

        // 명시적으로 attributes를 Map으로 변환
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            Object attributeObject = servletRequest.getServletRequest().getAttribute("attributes");

            if (attributeObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>) attributeObject;

                if (attributes.containsKey("email")) {
                    String email = (String) attributes.get("email");

                    String redisKey = "WebSocket:User:" + email;

                    Long remainingTime = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS); // TTL 확인
                    if (remainingTime == null || remainingTime <= 600) { // 10분 이하이거나 없을 때 갱신
                        redisTemplate.opsForValue().set(redisKey, "CONNECTED");
                        redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
                        log.info("Redis TTL 갱신: 사용자 Email={}, 남은 TTL={}초", email, remainingTime);
                    }

                    log.info("웹소켓 연결 성공: 사용자 Email={}, IP={}", email, ip);
                } else {
                    log.warn("웹소켓 연결 성공했으나 'email'이 attributes에 없음 (IP={})", ip);
                }
            } else {
                log.error("웹소켓 연결 성공했으나 attributes가 Map 타입이 아님 (IP={})", ip);
            }
        } else {
            log.error("웹소켓 요청이 ServletServerHttpRequest 타입이 아님 (IP={})", ip);
        }
    }

    /**
     * 요청에서 액세스 토큰 추출
     *
     * @param request 서버 HTTP 요청
     * @return 추출된 액세스 토큰 또는 null
     */
    private String extractAccessToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            Cookie[] cookies = servletRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("ACCESS_TOKEN".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        return null;
    }
}
