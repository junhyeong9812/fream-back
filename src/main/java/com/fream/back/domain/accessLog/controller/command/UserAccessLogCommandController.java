package com.fream.back.domain.accessLog.controller.command;

import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.InvalidParameterException;
import com.fream.back.domain.accessLog.service.command.UserAccessLogCommandService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

/**
 * 접근 로그 생성, 수정, 삭제 등을 담당하는
 * Command 전용 컨트롤러
 */
@RestController
@RequestMapping("/access-log/commands")
@RequiredArgsConstructor
@Slf4j
public class UserAccessLogCommandController {

    private final UserAccessLogCommandService userAccessLogCommandService;
    // 정규식 패턴: IPv4 주소 검증용
    private static final Pattern ipPattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    /**
     * 접근 로그 생성 엔드포인트
     * @param logDto 접근 로그 정보
     * @param request HttpServletRequest (IP, User-Agent, Referer 등 추출)
     * @return 성공 시 204 No Content
     * @throws InvalidParameterException 유효하지 않은 파라미터인 경우
     */
    @PostMapping("/create")
    public ResponseEntity<Void> createAccessLog(@RequestBody UserAccessLogDto logDto, HttpServletRequest request) {
        if (logDto == null) {
            throw new InvalidParameterException(AccessLogErrorCode.INVALID_ACCESS_LOG_DATA,
                    "접근 로그 데이터가 누락되었습니다.");
        }

        enrichAccessLogWithRequestData(logDto, request);
        userAccessLogCommandService.createAccessLog(logDto);
        return ResponseEntity.noContent().build();
    }

    /**
     * 요청 정보를 이용해 DTO 데이터 보강
     */
    private void enrichAccessLogWithRequestData(UserAccessLogDto logDto, HttpServletRequest request) {
        // IP 주소 처리
        String clientIp = extractClientIp(request);
        logDto.setIpAddress(clientIp);

        // User-Agent 처리
        String userAgent = request.getHeader("User-Agent");
        logDto.setUserAgent(userAgent);

        // Referer URL 처리
        String refererUrl = request.getHeader("Referer");
        logDto.setRefererUrl(refererUrl);

        // 이메일이 없으면 익명 사용자 처리
        if (logDto.getEmail() == null || logDto.getEmail().isEmpty()) {
            logDto.setEmail("Anonymous");
            logDto.setAnonymous(true);
        }
    }

    /**
     * 클라이언트 IP를 추출 및 검증
     */
    private String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_CLIENT_IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        // X-Forwarded-For에 여러 IP가 있을 경우 첫 번째 IP만 사용
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        // IP 보안 강화: 특수 문자 제거 (XSS 방지)
        if (clientIp != null) {
            clientIp = clientIp.replaceAll("[^0-9.]", "");
        }

        // 유효한 IPv4 형식인지 검증
        if (clientIp == null || clientIp.isEmpty() || !ipPattern.matcher(clientIp).matches()) {
            log.warn("유효하지 않은 IP 형식: {}", clientIp);
            clientIp = "0.0.0.0";
        }

        return clientIp;
    }
}