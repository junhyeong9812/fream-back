package com.fream.back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();

        resolver.setOneIndexedParameters(true); // ✅ 페이지 번호를 1부터 시작하도록 설정
        resolver.setFallbackPageable(PageRequest.of(0, 10)); // ✅ 기본 페이지 크기 10으로 설정
        resolver.setMaxPageSize(2000); // ✅ 최대 페이지 크기

        resolvers.add(resolver);
    }
}
