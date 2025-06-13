package com.fream.back.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.global.security.redis.IpBlockingRedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * IP 차단 및 레이트 리미팅 필터
 * JWT 필터보다 먼저 실행되어 차단된 IP의 요청을 조기에 차단
 */
@Slf4j
@RequiredArgsConstructor
public class IpBlockingFilter extends OncePerRequestFilter {

    private final IpBlockingRedisService ipBlockingRedisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);

        // IP가 null이거나 localhost인 경우 스킵 (개발 환경 고려)
        if (clientIp == null || isLocalhost(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        // IP 허용 여부 체크 (레이트 리미팅 + 블랙리스트)
        if (!ipBlockingRedisService.isIpAllowed(clientIp)) {
            handleBlockedIp(response, clientIp);
            return;
        }

        // 허용된 IP인 경우 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트 실제 IP 주소 추출
     * 프록시, 로드밸런서 등을 고려한 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = null;

        // X-Forwarded-For 헤더 체크 (프록시/로드밸런서 사용 시)
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 여러 IP가 있는 경우 첫 번째 IP 사용
            ip = ip.split(",")[0].trim();
        }

        // X-Real-IP 헤더 체크 (Nginx 등에서 사용)
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        // Proxy-Client-IP 헤더 체크
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        // WL-Proxy-Client-IP 헤더 체크 (WebLogic)
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        // HTTP_CLIENT_IP 헤더 체크
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        // HTTP_X_FORWARDED_FOR 헤더 체크
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        // 모든 헤더에서 찾지 못한 경우 RemoteAddr 사용
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * localhost 여부 체크
     */
    private boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) ||
                "0:0:0:0:0:0:0:1".equals(ip) ||
                "::1".equals(ip) ||
                "localhost".equals(ip);
    }

    /**
     * 차단된 IP 요청 처리
     */
    private void handleBlockedIp(HttpServletResponse response, String clientIp) throws IOException {
        log.warn("Blocked request from IP: {}", clientIp);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 차단 남은 시간 정보 포함
        long remainingTime = ipBlockingRedisService.getBlockRemainingTime(clientIp);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "TOO_MANY_REQUESTS");
        errorResponse.put("message", "Too many requests from your IP address. Please try again later.");
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());

        if (remainingTime > 0) {
            errorResponse.put("retryAfter", remainingTime); // 초 단위
            errorResponse.put("retryAfterMinutes", Math.ceil(remainingTime / 60.0)); // 분 단위
        }

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 특정 경로는 IP 차단 필터 적용 제외 (필요시 오버라이드)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 헬스체크, 모니터링 등의 경로는 제외
        return path.equals("/health") ||
                path.equals("/actuator/health") ||
                path.startsWith("/actuator/");
    }
}