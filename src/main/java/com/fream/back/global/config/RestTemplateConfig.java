package com.fream.back.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * 외부 API 통신을 위한 RestTemplate 설정
 * HTTP 요청/응답 로깅이 포함됨
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * 기본 RestTemplate 설정
     * - HttpComponents 기반 팩토리 사용
     * - 타임아웃 설정
     * - 로깅 인터셉터 추가
     *
     * @return 구성된 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        // HTTP Components 기반 팩토리 생성
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 연결 타임아웃 5초
        factory.setReadTimeout(10000);    // 읽기 타임아웃 10초

        // RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate(factory);

        // 로깅 인터셉터 추가
        restTemplate.getInterceptors().add(loggingInterceptor());

        return restTemplate;
    }

    /**
     * API 요청/응답 로깅을 위한 인터셉터
     *
     * @return 로깅 인터셉터
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            // 요청 로깅
            log.debug("REST 요청: {} {}", request.getMethod(), request.getURI());

            // 요청 시작 시간
            long startTime = System.currentTimeMillis();

            try {
                // 요청 실행
                ClientHttpResponse response = execution.execute(request, body);

                // 응답 처리 시간
                long duration = System.currentTimeMillis() - startTime;

                // 응답 로깅
                log.debug("REST 응답: {} {} ({}ms)", response.getStatusCode().value(),
                        response.getStatusText(), duration);

                return response;
            } catch (IOException e) {
                // 예외 발생 시
                log.error("REST 요청 중 오류 발생: {} {}", request.getMethod(), request.getURI(), e);
                throw e;
            }
        };
    }
}