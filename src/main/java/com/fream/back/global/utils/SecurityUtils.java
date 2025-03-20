package com.fream.back.global.utils;

import com.fream.back.global.config.security.JwtAuthenticationFilter;
import com.fream.back.global.exception.security.SecurityUserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 보안 관련 유틸리티 클래스
 * 인증된 사용자 정보를 추출하는 기능을 제공합니다.
 */
@Slf4j
public class SecurityUtils {

    /**
     * SecurityContext에서 이메일 추출
     *
     * @return 인증된 사용자의 이메일
     * @throws SecurityUserNotFoundException 인증된 사용자가 없는 경우
     */
    public static String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.debug("SecurityContext에서 인증 객체 추출: {}",
                authentication != null ? authentication.getClass().getSimpleName() : "null");

        if (authentication != null && authentication.getPrincipal() instanceof String) {
            String email = (String) authentication.getPrincipal();
            log.debug("인증된 사용자 이메일 추출: {}", email);
            return email;
        }

        log.warn("인증된 사용자 정보 없음");
        throw new SecurityUserNotFoundException();
    }

    /**
     * SecurityContext에서 이메일 추출 (익명 사용자 허용)
     * 인증된 사용자가 없는 경우 "anonymous" 반환
     *
     * @return 인증된 사용자의 이메일 또는 "anonymous"
     */
    public static String extractEmailOrAnonymous() {
        try {
            return extractEmailFromSecurityContext();
        } catch (SecurityUserNotFoundException e) {
            log.debug("인증되지 않은 사용자, 'anonymous' 반환");
            // Spring Security의 익명(Anonymous) 인증은
            // 내부적으로 AnonymousAuthenticationToken을 사용하며,
            // 이때 기본 principal 값이 "anonymousUser"로 설정
            // 이 리턴이 필요 없음
            return "anonymous";
        }
    }

    /**
     * SecurityContext에서 사용자 추가 정보 추출 (나이, 성별 등)
     *
     * @return 사용자 추가 정보 객체 또는 null
     */
    public static JwtAuthenticationFilter.UserInfo extractUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.debug("SecurityContext에서 사용자 세부 정보 추출 시도");
        System.out.println("authentication.getDetails() = " + authentication.getDetails() );

        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationFilter.UserInfo) {
            JwtAuthenticationFilter.UserInfo userInfo =
                    (JwtAuthenticationFilter.UserInfo) authentication.getDetails();
            log.debug("사용자 세부 정보 추출 성공: 나이={}, 성별={}",
                    userInfo.getAge(), userInfo.getGender());
            return userInfo;
        }

        log.debug("사용자 세부 정보 없음");
        return null;
    }
}