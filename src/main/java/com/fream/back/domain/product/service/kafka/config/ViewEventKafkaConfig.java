package com.fream.back.domain.product.service.kafka.config;

import com.fream.back.domain.product.dto.kafka.ViewEvent;
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
public class ViewEventKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ================================
    // Producer (ViewEvent)
    // ================================
    @Bean
    public ProducerFactory<String, ViewEvent> viewEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, ViewEvent> viewEventKafkaTemplate() {
        return new KafkaTemplate<>(viewEventProducerFactory());
    }

    // ================================
    // Consumer (ViewEvent)
    // ================================
    @Bean
    public ConsumerFactory<String, ViewEvent> viewEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // groupId는 @KafkaListener에 적어도 되고, 여기 props에 넣어도 됩니다.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // ViewEvent 전용 역직렬화
        JsonDeserializer<ViewEvent> deserializer = new JsonDeserializer<>(ViewEvent.class);
        // 필요하면 trustedPackages 설정
        // deserializer.addTrustedPackages("Fream_back.improve_Fream_Back.product.dto.kafka");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ViewEvent> viewEventKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, ViewEvent>();
        factory.setConsumerFactory(viewEventConsumerFactory());
        return factory;
    }
}
