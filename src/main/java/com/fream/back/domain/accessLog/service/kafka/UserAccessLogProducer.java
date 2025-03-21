package com.fream.back.domain.accessLog.service.kafka;

import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.dto.UserAccessLogEvent;
import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.AccessLogKafkaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Command 측에서 생성된 로그를 Kafka에 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccessLogProducer {

    private static final String TOPIC_NAME = "user-access-log-topic";
    private final KafkaTemplate<String, UserAccessLogEvent> kafkaTemplate;

    /**
     * 접근 로그를 Kafka로 전송합니다.
     *
     * @param dto 접근 로그 DTO
     * @throws AccessLogKafkaException Kafka 전송 실패 시
     */
    public void sendAccessLog(UserAccessLogDto dto) {
        try {
            UserAccessLogEvent event = mapToEvent(dto);

            // 비동기 전송 및 콜백 처리
            CompletableFuture<SendResult<String, UserAccessLogEvent>> future =
                    kafkaTemplate.send(TOPIC_NAME, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka 메시지 전송 실패: {}", dto, ex);
                    // 실패 처리 로직 (재시도, 알림 등)
                } else {
                    log.debug("Kafka 메시지 전송 성공: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 준비 중 오류 발생", e);
            throw new AccessLogKafkaException(AccessLogErrorCode.KAFKA_SEND_ERROR,
                    "접근 로그를 전송하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * DTO를 Kafka 이벤트로 변환합니다.
     */
    private UserAccessLogEvent mapToEvent(UserAccessLogDto dto) {
        if (dto == null) {
            throw new AccessLogKafkaException(AccessLogErrorCode.INVALID_ACCESS_LOG_DATA,
                    "접근 로그 데이터가 null입니다.");
        }

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

        return event;
    }
}