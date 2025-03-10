package com.fream.back.domain.style.service.kafka;

import com.fream.back.domain.style.dto.kafka.StyleViewEvent;
import com.fream.back.domain.user.entity.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StyleViewEventProducer {

    private final KafkaTemplate<String, StyleViewEvent> styleViewEventKafkaTemplate;
    private static final String TOPIC_NAME = "style-view-log-topic";

    /**
     * 특정 Style이 조회되었을 때,
     * StyleViewEvent를 생성하여 Kafka에 전송.
     *
     * @param styleId 조회된 스타일 ID
     * @param email   사용자 이메일 (익명 시 "anonymous")
     * @param age     나이 (익명 시 0)
     * @param gender  성별 (익명 시 Gender.OTHER)
     */
    public void sendViewEvent(Long styleId, String email, Integer age, Gender gender) {
        try {
            StyleViewEvent event = new StyleViewEvent(
                    styleId,
                    email,
                    age,
                    gender,
                    LocalDateTime.now()
            );

            log.info("스타일 뷰 이벤트 전송: styleId={}, email={}", styleId, email);
            styleViewEventKafkaTemplate.send(TOPIC_NAME, event);
        } catch (Exception e) {
            log.error("스타일 뷰 이벤트 전송 실패: {}", e.getMessage(), e);
            // 이벤트 전송 실패해도 사용자 경험에 영향을 주지 않도록 예외 처리
        }
    }
}