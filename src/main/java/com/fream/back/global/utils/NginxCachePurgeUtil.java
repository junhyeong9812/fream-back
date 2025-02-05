package com.fream.back.global.utils;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class NginxCachePurgeUtil {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String nginxUrl = "http://nginx:80";
    // docker-compose 환경에서 nginx 컨테이너 호스트명

    public void purgeProductCache() {
        String purgeUrl = nginxUrl + "/purge_products/api/products/";
        RequestEntity<Void> requestEntity = RequestEntity
                .method(HttpMethod.valueOf("PURGE"), URI.create(purgeUrl))
                .build();
        try {
            restTemplate.exchange(requestEntity, String.class);
        } catch (Exception e) {
            // 로그 처리 (logger.warn, etc.)
        }
    }
    public void purgeEsCache() {
        String purgeUrl = nginxUrl + "/purge_es/api/es/products/";
        RequestEntity<Void> requestEntity = RequestEntity
                .method(HttpMethod.valueOf("PURGE"), URI.create(purgeUrl))
                .build();
        try {
            restTemplate.exchange(requestEntity, String.class);
        } catch (Exception e) {
            // 로그 처리 (logger.warn, etc.)
        }
    }

    public void purgeStyleCache() {
        String purgeUrl = nginxUrl + "/purge_styles/api/styles/queries";
        RequestEntity<Void> requestEntity = RequestEntity
                .method(HttpMethod.valueOf("PURGE"), URI.create(purgeUrl))
                .build();
        try {
            restTemplate.exchange(requestEntity, String.class);
        } catch (Exception e) {
            // 로그 처리
        }
    }
}
