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
    public SecurityFilterChain securityfilterChain(HttpSecurity http) throws Exception {
        http
                // 1) 모든 요청 허용
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // 2) formLogin 비활성화 (기본 로그인 페이지 제거)
                .formLogin(form -> form.disable())

                // 3) CSRF 비활성화 (REST API에서 필요 없음)
                .csrf(csrf -> csrf.disable())

                // 4) 세션 비활성화 (Stateful 인증 제거)
                .sessionManagement(session -> session.disable())

                // 5) 모든 Security 필터 해제 (필요하면 주석 제거)
                .securityMatcher("/**");

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
