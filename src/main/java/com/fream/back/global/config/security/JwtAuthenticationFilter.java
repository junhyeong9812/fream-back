package com.fream.back.global.config.security;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.redis.AuthRedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 매 요청 시 "ACCESS_TOKEN" 쿠키에서 토큰 추출 -> 검증 -> SecurityContextHolder 세팅
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException
    {
        // 1) 쿠키에서 AccessToken 찾기
        String accessToken = getCookieValue(request, "ACCESS_TOKEN");

        // 2) 유효성 검증
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            // 3) Redis에 존재하는지(화이트리스트)
            if (authRedisService.isAccessTokenValid(accessToken)) {
                // 4) 토큰에서 이메일, 나이, 성별 등
                String email = jwtTokenProvider.getEmailFromToken(accessToken);
                Integer age = authRedisService.getAgeByAccessToken(accessToken);
                Gender gender = authRedisService.getGenderByAccessToken(accessToken);

                // 예) 권한을 넣고 싶다면 GrantedAuthority 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, null);

                // 나이/성별 등 추가 정보를 details에 저장
                UserInfo userInfo = new UserInfo(age, gender);
                authentication.setDetails(userInfo);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
        } else {
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 특정 이름의 값 추출
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (c.getName().equals(cookieName)) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 임의의 사용자 정보 객체 (SecurityContextHolder에 details로 담을 수 있음)
     */
    public static class UserInfo {
        private final Integer age;
        private final Gender gender;

        public UserInfo(Integer age, Gender gender) {
            this.age = age;
            this.gender = gender;
        }

        public Integer getAge() { return age; }
        public Gender getGender() { return gender; }
    }
}
