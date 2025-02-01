package com.fream.back.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1) 모든 요청 허용
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

                // 2) CSRF 비활성화 (REST API의 경우)
                .csrf(csrf -> csrf.disable())

                // 3) 폼 로그인 비활성화
                .formLogin(form -> form.disable())

                // 4) HTTP 기본 인증 비활성화 (Basic Auth 비활성화)
                .httpBasic(httpBasic -> httpBasic.disable())

                // 5) 세션을 사용하지 않도록 설정 (STATELESS)
                .sessionManagement(session -> session.disable())

                // 6) Security 필터 완전히 제거 (필요하면 주석 해제)
                .securityMatcher("/**");

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
