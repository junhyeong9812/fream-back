package com.fream.back.domain.user.controller.command;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class OAuth2AuthorizationController {
    @GetMapping("/oauth2/authorization/{registrationId}")
    public ResponseEntity<?> redirectToOAuthProvider(
            @PathVariable String registrationId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.info("OAuth2 Authorization Request for provider: {}", registrationId);

        // 리다이렉트 로직은 Spring Security가 실제로 처리할 것이므로
        // 여기서는 단순히 로깅하고 성공 응답을 보낼 수 있습니다.
        return ResponseEntity.ok("OAuth2 Authorization initiated for " + registrationId);
    }
}