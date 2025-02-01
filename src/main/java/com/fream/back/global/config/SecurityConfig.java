package com.fream.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;
/**
 * 기본적인 Security 설정 (개발용/임시)
 * - 모든 요청 허용
 * - CORS 설정
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1) 요청 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                // 2) formLogin 비활성화 (옛날 http.formLogin().disable() 대신)
                .formLogin(form -> form.disable())
                // 또는 .formLogin(FormLoginConfigurer::disable)

                // 3) csrf 비활성화
                .csrf(csrf -> csrf.disable())

                // 4) CORS 활성화 (기본 설정)
                .cors(withDefaults());

        return http.build();
    }

}
