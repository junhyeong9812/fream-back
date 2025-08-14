package com.fream.back.global.config.kafka;

import com.fream.back.domain.order.dto.kafka.OrderProcessingEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * OrderProcessingEvent를 위한 Kafka 설정 클래스
 * 주문 전체 처리의 비동기화 및 안정성을 위한 설정
 */
@Configuration
@EnableKafka
public class OrderKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ================================
    // Producer 설정 (OrderProcessingEvent)
    // ================================

    /**
     * 주문 처리 이벤트 Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, OrderProcessingEvent> orderProcessingEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 주문 처리의 안정성을 위한 추가 설정
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 복제본에서 확인
        props.put(ProducerConfig.RETRIES_CONFIG, 3); // 재시도 횟수
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 멱등성 보장
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * 주문 처리 이벤트 KafkaTemplate 설정
     */
    @Bean
    public KafkaTemplate<String, OrderProcessingEvent> orderProcessingEventKafkaTemplate() {
        return new KafkaTemplate<>(orderProcessingEventProducerFactory());
    }

    // ================================
    // Consumer 설정 (OrderProcessingEvent)
    // ================================

    /**
     * 주문 처리 이벤트 Consumer Factory 설정
     */
    @Bean
    public ConsumerFactory<String, OrderProcessingEvent> orderProcessingEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 중복 처리 방지를 위한 설정
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5); // 한 번에 처리할 메시지 수 제한
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // JsonDeserializer 설정
        JsonDeserializer<OrderProcessingEvent> deserializer = new JsonDeserializer<>(OrderProcessingEvent.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * 주문 처리 이벤트 Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderProcessingEvent> orderProcessingKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderProcessingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderProcessingEventConsumerFactory());

        // 동시성 제어 - 주문 처리는 순차적으로 처리하는 것이 안전
        factory.setConcurrency(2); // 2개의 컨슈머로 병렬 처리 (주문별로 순서 보장)

        // 수동 ACK 모드 설정 (처리 완료 후 커밋)
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE
        );

        return factory;
    }

    // ================================
    // 알림용 Producer 설정
    // ================================

    /**
     * 알림 전용 Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, Map<String, Object>> notificationProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 알림은 성능보다 안정성 중심
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 1);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 8192);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * 알림 전용 KafkaTemplate 설정
     */
    @Bean
    public KafkaTemplate<String, Map<String, Object>> notificationKafkaTemplate() {
        return new KafkaTemplate<>(notificationProducerFactory());
    }
}