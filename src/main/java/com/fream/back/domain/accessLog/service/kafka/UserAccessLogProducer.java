package com.fream.back.domain.accessLog.service.kafka;

import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.dto.UserAccessLogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Command 측에서 생성된 로그를 Kafka에 전송
 */
@Service
@RequiredArgsConstructor
public class UserAccessLogProducer {

    private static final String TOPIC_NAME = "user-access-log-topic";
    private final KafkaTemplate<String, UserAccessLogEvent> kafkaTemplate;

    public void sendAccessLog(UserAccessLogDto dto) {
        UserAccessLogEvent event = new UserAccessLogEvent();
        event.setRefererUrl(dto.getRefererUrl());
        event.setUserAgent(dto.getUserAgent());
        event.setOs(dto.getOs());
        event.setBrowser(dto.getBrowser());
        event.setDeviceType(dto.getDeviceType());
        event.setIpAddress(dto.getIpAddress());
        // 위치 정보는 Consumer에서 채움
        event.setCountry(null);
        event.setRegion(null);
        event.setCity(null);
        event.setPageUrl(dto.getPageUrl());
        event.setEmail(dto.getEmail());
        event.setAnonymous(dto.isAnonymous());
        event.setNetworkType(dto.getNetworkType());
        event.setBrowserLanguage(dto.getBrowserLanguage());
        event.setScreenWidth(dto.getScreenWidth());
        event.setScreenHeight(dto.getScreenHeight());
        event.setDevicePixelRatio(dto.getDevicePixelRatio());
        event.setAccessTime(LocalDateTime.now());

        kafkaTemplate.send(TOPIC_NAME, event);
    }
}
