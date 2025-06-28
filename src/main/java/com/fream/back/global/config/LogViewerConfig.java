package com.fream.back.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 로그 뷰어 관련 설정
 */
@Configuration
@EnableMethodSecurity
public class LogViewerConfig {

//    @Value("${logging.file.path:/home/ubuntu/springlog}")
    @Value("${logging.file.path:C:\\Users\\pickj\\webserver\\dockerVolums\\springlog}")
    private String logDirectoryPath;

    /**
     * 로그 디렉토리 패스 설정
     * application.yml에 설정되지 않은 경우 기본값 사용
     */
    @Bean
    public String logDirectoryPath() {
        return logDirectoryPath;
    }

    /**
     * 로그 뷰어를 위한 CORS 설정
     * 특정 도메인에서만 로그 API에 접근할 수 있도록 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://www.pinjun.xyz")); // 운영 도메인만 허용
        configuration.setAllowedMethods(Arrays.asList("GET"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/logs/**", configuration);
        return source;
    }
}