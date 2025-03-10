package com.fream.back.domain.style.service.kafka.config;

import com.fream.back.domain.style.dto.kafka.StyleViewEvent;
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

@Configuration
@EnableKafka
public class StyleViewEventKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ================================
    // Producer (StyleViewEvent)
    // ================================
    @Bean
    public ProducerFactory<String, StyleViewEvent> styleViewEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, StyleViewEvent> styleViewEventKafkaTemplate() {
        return new KafkaTemplate<>(styleViewEventProducerFactory());
    }

    // ================================
    // Consumer (StyleViewEvent)
    // ================================
    @Bean
    public ConsumerFactory<String, StyleViewEvent> styleViewEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // StyleViewEvent 전용 역직렬화
        JsonDeserializer<StyleViewEvent> deserializer = new JsonDeserializer<>(StyleViewEvent.class);
        // 필요하면 trustedPackages 설정
        // deserializer.addTrustedPackages("com.fream.back.domain.style.dto.kafka");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StyleViewEvent> styleViewEventKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, StyleViewEvent>();
        factory.setConsumerFactory(styleViewEventConsumerFactory());
        return factory;
    }
}