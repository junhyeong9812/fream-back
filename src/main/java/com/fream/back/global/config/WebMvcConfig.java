package com.fream.back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 전역 CORS 설정
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 모든 경로 허용
        registry.addMapping("/**")
                // 모든 Origin 허용(개발용)
                .allowedOrigins("*")
                // 모든 HTTP 메서드 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                // 쿠키/헤더 전송 허용
                .allowCredentials(true)
                // pre-flight 요청 캐시 시간(초)
                .maxAge(3600);
    }
}
