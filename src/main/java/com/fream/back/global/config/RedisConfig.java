//package com.fream.back.global.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//
//@Configuration
//public class RedisConfig {
//
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        LettuceConnectionFactory factory = new LettuceConnectionFactory("redis", 6379);
//        // 필요시 factory.setPassword("..."); etc.
//        return factory;
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        // 필요시 serializer 설정
//        return template;
//    }
//}
package com.fream.back.global.config;

import io.lettuce.core.ReadFrom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.redis.primary.host:redis-primary}")
    private String primaryHost;

    @Value("${spring.redis.primary.port:6379}")
    private int primaryPort;

    @Value("${spring.redis.replica.host:redis-replica}")
    private String replicaHost;

    @Value("${spring.redis.replica.port:6380}")
    private int replicaPort;

    @Value("${spring.redis.timeout:2000}")
    private long timeoutMs;

    /**
     * Primary-Replica 구성을 위한 ConnectionFactory
     * 쓰기는 Primary, 읽기는 Replica에서 처리
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Redis Primary-Replica 연결 설정 - Primary: {}:{}, Replica: {}:{}",
                primaryHost, primaryPort, replicaHost, replicaPort);

        // Primary-Replica 구성 설정
        RedisStaticMasterReplicaConfiguration configuration =
                new RedisStaticMasterReplicaConfiguration(primaryHost, primaryPort);
        configuration.addNode(replicaHost, replicaPort);

        // Lettuce 클라이언트 설정
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED) // 읽기는 Replica 우선, 없으면 Master
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, clientConfig);
        factory.setValidateConnection(true);

        return factory;
    }

    /**
     * 쓰기 전용 Primary Redis ConnectionFactory
     */
    @Bean("primaryRedisConnectionFactory")
    public RedisConnectionFactory primaryRedisConnectionFactory() {
        log.info("Redis Primary 전용 연결 설정 - Host: {}:{}", primaryHost, primaryPort);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .build();

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(primaryHost, primaryPort);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.setValidateConnection(true);

        return factory;
    }

    /**
     * 읽기 전용 Replica Redis ConnectionFactory
     */
    @Bean("replicaRedisConnectionFactory")
    public RedisConnectionFactory replicaRedisConnectionFactory() {
        log.info("Redis Replica 전용 연결 설정 - Host: {}:{}", replicaHost, replicaPort);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .build();

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(replicaHost, replicaPort);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.setValidateConnection(true);

        return factory;
    }

    /**
     * 기본 RedisTemplate (Primary-Replica 자동 분산)
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("기본 RedisTemplate 설정");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value는 JSON으로 직렬화
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }

    /**
     * String 전용 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("String 전용 RedisTemplate 설정");

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 모든 Serializer를 String으로 설정
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setDefaultSerializer(stringSerializer);

        template.afterPropertiesSet();

        return template;
    }

    /**
     * 쓰기 전용 RedisTemplate (Primary만 사용)
     */
    @Bean("writeRedisTemplate")
    public RedisTemplate<String, String> writeRedisTemplate() {
        log.info("쓰기 전용 RedisTemplate 설정");

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(primaryRedisConnectionFactory());

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setDefaultSerializer(stringSerializer);

        template.afterPropertiesSet();

        return template;
    }

    /**
     * 읽기 전용 RedisTemplate (Replica만 사용)
     */
    @Bean("readRedisTemplate")
    public RedisTemplate<String, String> readRedisTemplate() {
        log.info("읽기 전용 RedisTemplate 설정");

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(replicaRedisConnectionFactory());

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setDefaultSerializer(stringSerializer);

        template.afterPropertiesSet();

        return template;
    }
}