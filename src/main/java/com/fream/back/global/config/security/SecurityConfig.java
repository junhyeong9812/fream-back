package com.fream.back.global.config.security;

import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.security.oauth2.CustomOAuth2UserService;
import com.fream.back.domain.user.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.fream.back.global.config.security.filter.LoginAuthenticationFilter;
import com.fream.back.global.config.security.filter.LogoutAuthenticationFilter;
import com.fream.back.global.config.security.filter.TokenRefreshFilter;
import com.fream.back.global.security.redis.IpBlockingRedisService;
import com.fream.back.global.security.filter.IpBlockingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final IpBlockingRedisService ipBlockingRedisService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 커스텀 필터들 생성
        IpBlockingFilter ipBlockingFilter = new IpBlockingFilter(ipBlockingRedisService);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, authRedisService);
        LoginAuthenticationFilter loginFilter = new LoginAuthenticationFilter(
                userRepository, passwordEncoder, jwtTokenProvider, authRedisService);
        LogoutAuthenticationFilter logoutFilter = new LogoutAuthenticationFilter(
                authRedisService, jwtTokenProvider);
        TokenRefreshFilter refreshFilter = new TokenRefreshFilter(
                jwtTokenProvider, authRedisService, userRepository);

        http
                // 1) 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(whiteListPaths()).permitAll()
                        .anyRequest().authenticated())

                // 2) CSRF 비활성화 (REST API의 경우)
                .csrf(csrf -> csrf.disable())

                // 3) 폼 로그인 비활성화
                .formLogin(form -> form.disable())

                // 4) HTTP 기본 인증 비활성화 (Basic Auth 비활성화)
                .httpBasic(httpBasic -> httpBasic.disable())

                // 5) 세션을 사용하지 않도록 설정 (STATELESS)
                .sessionManagement(session -> session.disable())

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                // 커스텀 필터들 추가 (순서 중요!)
                .addFilterBefore(ipBlockingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loginFilter, IpBlockingFilter.class)
                .addFilterAfter(logoutFilter, LoginAuthenticationFilter.class)
                .addFilterAfter(refreshFilter, LogoutAuthenticationFilter.class)
                .addFilterAfter(jwtFilter, TokenRefreshFilter.class);

        return http.build();
    }

    /**
     * [Whitelist] - 인증 없이 접근 가능한 경로들
     */
    private String[] whiteListPaths() {
        return new String[] {
                // 정적 리소스
                "/favicon.ico",
                "/error",

                // OAuth 관련
                "/oauth2/**",
                "/login/oauth2/**",
                "/oauth/**",

                // 인증 관련 (필터에서 처리됨)
                "/auth/login",          // 로그인 (LoginAuthenticationFilter에서 처리)
                "/auth/logout",         // 로그아웃 (LogoutAuthenticationFilter에서 처리)
                "/auth/refresh",        // 토큰 갱신 (TokenRefreshFilter에서 처리)

                // 공개 API
                "/users/register",              // 회원가입
                "/users/find-email",            // 이메일 찾기
                "/users/reset-password",        // 비밀번호 찾기 - 사용자 확인
                "/users/reset-password-sandEmail", // 비밀번호 찾기 - 이메일 발송
                "/users/reset",                 // 비밀번호 변경
                "/users/verify-identity",       // 본인인증 검증
                "/identity-verification/**",    // 본인인증 관련

                // 관리자 인증 (별도 처리 필요시)
                "/admin/auth/**",

                // 헬스체크 및 모니터링
                "/health",
                "/actuator/**",

                // WebSocket (필요시)
                "/ws/**",

                // 개발/테스트용 (운영환경에서는 제거)
                "/swagger-ui/**",
                "/v3/api-docs/**"
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}