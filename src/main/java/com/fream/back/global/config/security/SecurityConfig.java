package com.fream.back.global.config.security;

import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.security.oauth2.CustomOAuth2UserService;
import com.fream.back.domain.user.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //JWT 필터 생성
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, authRedisService);

        http
                // 1) 모든 요청 허용
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

                // OAuth2 로그인 설정 추가
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/api/login/oauth2/code/*"))
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                //커스텀 JWT필터 추가
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                // 6) Security 필터 완전히 제거 (필요하면 주석 해제)
//                .securityMatcher("/**");


        return http.build();
    }

    /**
     * [Whitelist] - 인증 없이 접근 가능한 경로들
     */
    private String[] whiteListPaths() {
        return new String[] {
                "/**", "/ws/**"
//                "/auth/**",       // 로그인, 회원가입, 리프레시, 로그아웃 등
//                "/api/public/**", // 예시: 공개 API
//                "/swagger-ui/**", // 예시: Swagger 문서
//                "/v3/api-docs/**" // 예시: Swagger OpenAPI
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    // PasswordEncoder 설정
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();  // BCrypt 패스워드 암호화
//    }
}
