package com.fream.back.domain.accessLog.service.kafka;

import com.fream.back.domain.accessLog.dto.UserAccessLogEvent;
import com.fream.back.domain.accessLog.entity.UserAccessLog;
import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.AccessLogKafkaException;
import com.fream.back.domain.accessLog.exception.AccessLogSaveException;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import com.fream.back.domain.accessLog.service.geo.GeoIPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Consumer 측에서 Log Event를 수신하여 DB에 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccessLogConsumer_backup {

    private final UserAccessLogRepository userAccessLogRepository;
    private final GeoIPService geoIPService;

    /**
     * Kafka에서 접근 로그 이벤트를 수신하여 DB에 저장합니다.
     *
     * @param event 접근 로그 이벤트
     * @throws AccessLogKafkaException Kafka 수신 처리 오류 시
     * @throws AccessLogSaveException 데이터 저장 오류 시
     */
    @KafkaListener(
            topics = "user-access-log-topic",
            groupId = "user-access-log-group",
            containerFactory = "userAccessLogKafkaListenerContainerFactory"
    )
    public void consume(UserAccessLogEvent event) {
        try {
            if (event == null) {
                throw new AccessLogKafkaException(AccessLogErrorCode.KAFKA_RECEIVE_ERROR,
                        "수신된 이벤트가 null입니다.");
            }

            log.debug("접근 로그 이벤트 수신: {}", event);

            // IP → 위치 조회
            GeoIPService.Location location = geoIPService.getLocation(event.getIpAddress());

            // DB 엔티티 구성
            UserAccessLog accessLog = buildUserAccessLog(event, location);

            try {
                // 저장
                userAccessLogRepository.save(accessLog);
                log.debug("접근 로그 저장 완료: {}", accessLog.getIpAddress());
            } catch (DataAccessException e) {
                throw new AccessLogSaveException(AccessLogErrorCode.ACCESS_LOG_SAVE_ERROR,
                        "접근 로그 저장 중 데이터베이스 오류가 발생했습니다.", e);
            }
        } catch (Exception e) {
            // 예외 로깅 (Kafka 리스너에서 예외가 전파되면 메시지 처리가 중단될 수 있음)
            log.error("접근 로그 처리 중 오류 발생", e);

            // 심각한 오류의 경우 알림 또는 추가 처리 가능
            // notificationService.sendAlert("UserAccessLogConsumer 오류: " + e.getMessage());

            // 필요에 따라 예외 다시 던지기 또는 오류 메시지를 DLQ(Dead Letter Queue)로 보내기
            // kafkaErrorHandler.sendToDlq(event, e);
        }
    }

    /**
     * 이벤트와 위치 정보를 이용하여 UserAccessLog 엔티티를 생성합니다.
     */
    private UserAccessLog buildUserAccessLog(UserAccessLogEvent event, GeoIPService.Location location) {
        return UserAccessLog.builder()
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
    }
}