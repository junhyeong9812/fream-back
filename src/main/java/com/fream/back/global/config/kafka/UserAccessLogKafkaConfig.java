package com.fream.back.global.config.kafka;

import com.fream.back.domain.accessLog.dto.UserAccessLogEvent;
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
 * UserAccessLogEvent를 위한 Kafka 설정 클래스
 */
@Configuration
@EnableKafka
public class UserAccessLogKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * ProducerFactory 설정
     */
    @Bean
    public ProducerFactory<String, UserAccessLogEvent> userAccessLogProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate 설정
     */
    @Bean
    public KafkaTemplate<String, UserAccessLogEvent> userAccessLogKafkaTemplate() {
        return new KafkaTemplate<>(userAccessLogProducerFactory());
    }

    /**
     * ConsumerFactory 설정
     */
    @Bean
    public ConsumerFactory<String, UserAccessLogEvent> userAccessLogConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // DefaultKafkaConsumerFactory에 JsonDeserializer 설정
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(UserAccessLogEvent.class)
        );
    }

    /**
     * ListenerContainerFactory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserAccessLogEvent> userAccessLogKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserAccessLogEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userAccessLogConsumerFactory());
        return factory;
    }
}

