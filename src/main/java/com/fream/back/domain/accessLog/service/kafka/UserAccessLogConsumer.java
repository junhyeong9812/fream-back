package com.fream.back.domain.accessLog.service.kafka;

import com.fream.back.domain.accessLog.dto.UserAccessLogEvent;
import com.fream.back.domain.accessLog.entity.UserAccessLog;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import com.fream.back.domain.accessLog.service.geo.GeoIPService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Consumer 측에서 Log Event를 수신하여 DB에 저장
 */
@Service
@RequiredArgsConstructor
public class UserAccessLogConsumer {

    private final UserAccessLogRepository userAccessLogRepository;
    private final GeoIPService geoIPService;

    @KafkaListener(
            topics = "user-access-log-topic",
            groupId = "user-access-log-group",
            containerFactory = "userAccessLogKafkaListenerContainerFactory"
    )
    public void consume(UserAccessLogEvent event) {
        // IP → 위치 조회
        GeoIPService.Location location = geoIPService.getLocation(event.getIpAddress());

        // DB 엔티티 구성
        UserAccessLog log = UserAccessLog.builder()
                .refererUrl(event.getRefererUrl())
                .userAgent(event.getUserAgent())
                .os(event.getOs())
                .browser(event.getBrowser())
                .deviceType(event.getDeviceType())
                .ipAddress(event.getIpAddress())
                .country(location.getCountry())
                .region(location.getRegion())
                .city(location.getCity())
                .pageUrl(event.getPageUrl())
                .email(event.getEmail() != null ? event.getEmail() : "Anonymous")
                .isAnonymous(event.isAnonymous())
                .networkType(event.getNetworkType())
                .browserLanguage(event.getBrowserLanguage())
                .screenWidth(event.getScreenWidth())
                .screenHeight(event.getScreenHeight())
                .devicePixelRatio(event.getDevicePixelRatio())
                .accessTime(event.getAccessTime() != null ? event.getAccessTime() : LocalDateTime.now())
                .build();

        // 저장
        userAccessLogRepository.save(log);
    }
}
