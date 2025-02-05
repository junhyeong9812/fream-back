package com.fream.back.global.utils;

import com.fream.back.global.config.security.JwtAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다."); // 인증 실패 처리
    }
    public static String extractEmailOrAnonymous() {
        try {
            return extractEmailFromSecurityContext();
        } catch (IllegalStateException e) {
            // 인증 안 된 경우
//            Spring Security의 익명(Anonymous) 인증은
//            내부적으로 AnonymousAuthenticationToken을 사용하며,
//            이때 기본 principal 값이 "anonymousUser"로 설정
            //이 리턴이 필요 없음
            return "anonymous";
        }
    }
    // 나이/성별 등 추가 정보도 가져오려면?
    public static JwtAuthenticationFilter.UserInfo extractUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("authentication.getDetails() = " + authentication.getDetails() );
        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationFilter.UserInfo) {
            return (JwtAuthenticationFilter.UserInfo) authentication.getDetails();
        }
        return null;
    }
}