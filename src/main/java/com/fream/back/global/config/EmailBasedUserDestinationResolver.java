package com.fream.back.global.config;

import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class EmailBasedUserDestinationResolver {

    private final SimpUserRegistry simpUserRegistry;

    public EmailBasedUserDestinationResolver(SimpUserRegistry simpUserRegistry) {
        this.simpUserRegistry = simpUserRegistry;
    }

    public String resolveUserDestination(String destination) {
        // SecurityContext에서 이메일 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            String email = (String) authentication.getPrincipal();
            // STOMP 목적지를 이메일 기반으로 변경
            return "/user/" + email + destination;
        }
        throw new IllegalStateException("사용자 인증 정보가 없습니다.");
    }
}
