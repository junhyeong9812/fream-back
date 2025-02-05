package com.fream.back.domain.product.service.kafka;

import com.fream.back.domain.product.dto.kafka.ViewEvent;
import com.fream.back.domain.user.entity.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ViewEventProducer {

    private final KafkaTemplate<String, ViewEvent> kafkaTemplate;
    private static final String TOPIC_NAME = "view-log-topic";

    /**
     * 특정 ProductColor가 조회되었을 때,
     * ViewEvent를 생성하여 Kafka에 전송.
     *
     * @param productColorId 조회된 상품 색상 ID
     * @param email          사용자 이메일 (익명 시 "anonymous")
     * @param age            나이 (익명 시 0)
     * @param gender         성별 (익명 시 Gender.OTHER)
     */
    public void sendViewEvent(Long productColorId, String email, Integer age, Gender gender) {
        ViewEvent event = new ViewEvent(
                productColorId,
                email,
                age,
                gender,
                LocalDateTime.now()
        );
        kafkaTemplate.send(TOPIC_NAME, event);
    }
}
