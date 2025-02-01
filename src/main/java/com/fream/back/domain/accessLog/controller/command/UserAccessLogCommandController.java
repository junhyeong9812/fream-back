package com.fream.back.domain.accessLog.controller.command;

import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.service.command.UserAccessLogCommandService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 접근 로그 생성, 수정, 삭제 등을 담당하는
 * Command 전용 컨트롤러
 */
@RestController
@RequestMapping("/access-log/commands")
@RequiredArgsConstructor
public class UserAccessLogCommandController {

    private final UserAccessLogCommandService userAccessLogCommandService;

    /**
     * 접근 로그 생성 엔드포인트
     * @param logDto 접근 로그 정보
     * @param request HttpServletRequest (IP, User-Agent, Referer 등 추출)
     */
    @PostMapping("/create")
    public void createAccessLog(@RequestBody UserAccessLogDto logDto, HttpServletRequest request) {
        // IP 주소 처리
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
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

        // Command Service 호출
        userAccessLogCommandService.createAccessLog(logDto);
    }
}
