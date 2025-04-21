# 설정 모듈

이 디렉토리는 Fream 백엔드 애플리케이션의 다양한 설정 클래스들을 포함합니다.

## 데이터베이스 및 ORM 설정

### QueryDslConfig

QueryDSL을 위한 설정 클래스입니다.

```java
@Configuration
@RequiredArgsConstructor
public class QueryDslConfig {
    private final EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
```

### 주요 기능

- JPAQueryFactory 빈을 생성하여 QueryDSL을 사용할 수 있게 합니다.
- EntityManager를 주입받아 JPAQueryFactory를 초기화합니다.

### 사용 예시

```java
@Repository
@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> findByBrandAndPriceRange(String brand, BigDecimal minPrice, BigDecimal maxPrice) {
        return queryFactory
                .selectFrom(product)
                .where(
                    product.brand.eq(brand),
                    product.price.between(minPrice, maxPrice)
                )
                .fetch();
    }
}
```

## 웹 관련 설정

### WebConfig

Spring MVC 웹 설정을 담당하는 클래스입니다.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setOneIndexedParameters(true);
        resolver.setFallbackPageable(PageRequest.of(0, 10));
        resolver.setMaxPageSize(2000);
        resolvers.add(resolver);
    }
}
```

### 주요 기능

- **페이징 설정**: 페이지 번호를 1부터 시작하도록 설정합니다.
- **기본 페이지 크기**: 기본 페이지 크기를 10으로 설정합니다.
- **최대 페이지 크기**: 한 번에 조회 가능한 최대 항목 수를 제한합니다.

## 캐시 및 데이터 저장소 설정

### RedisConfig

Redis 캐시 설정을 담당하는 클래스입니다.

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("redis", 6379);
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
```

### 주요 기능

- **Redis 연결 설정**: Redis 서버 연결 설정을 제공합니다.
- **RedisTemplate 설정**: Redis 데이터 조작을 위한 템플릿을 제공합니다.

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void cacheData(String key, Object data, long expireSeconds) {
        redisTemplate.opsForValue().set(key, data);
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
    }
    
    public Object getCachedData(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

## 로깅 및 모니터링 설정

### LogViewerConfig

로그 뷰어 관련 설정을 담당하는 클래스입니다.

```java
@Configuration
@EnableMethodSecurity
public class LogViewerConfig {
    @Value("${logging.file.path:/home/ubuntu/springlog}")
    private String logDirectoryPath;

    @Bean
    public String logDirectoryPath() {
        return logDirectoryPath;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://www.pinjun.xyz"));
        configuration.setAllowedMethods(Arrays.asList("GET"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/logs/**", configuration);
        return source;
    }
}
```

### 주요 기능

- **로그 디렉토리 설정**: 로그 파일이 저장되는 경로를 설정합니다.
- **CORS 설정**: 로그 API에 대한 CORS 정책을 설정합니다.

## 배치 및 스케줄링 설정

### BatchInfraConfig

Spring Batch 인프라 설정을 담당하는 클래스입니다.

```java
@Configuration
@EnableBatchProcessing
public class BatchInfraConfig {
    @Bean
    public JobRepository jobRepository(
            DataSource dataSource,
            PlatformTransactionManager transactionManager
    ) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType(DatabaseType.MYSQL.name());
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
```

### 주요 기능

- **배치 작업 저장소**: 배치 작업 메타데이터를 저장할 리포지토리를 설정합니다.
- **데이터베이스 설정**: 배치 작업에 사용할 데이터베이스 타입을 설정합니다.

### ShipmentBatchScheduler

배송 상태 업데이트 배치 작업을 스케줄링하는 클래스입니다.

```java
@Component
@RequiredArgsConstructor
public class ShipmentBatchScheduler {
    private final JobLauncher jobLauncher;

    @Qualifier("updateShipmentStatusesJob")
    @Autowired
    private Job updateShipmentStatusesJob;

    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduleShipmentStatusJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateShipmentStatusesJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 주요 기능

- **정기적인 작업 실행**: cron 표현식을 사용하여 6시간마다 배치 작업을 실행합니다.
- **작업 파라미터**: 배치 작업 실행 시 필요한 파라미터를 설정합니다.

### 사용 예시

```java
@Configuration
@RequiredArgsConstructor
public class ShipmentBatchConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ShipmentReader shipmentReader;
    private final ShipmentProcessor shipmentProcessor;
    private final ShipmentWriter shipmentWriter;

    @Bean
    public Job updateShipmentStatusesJob() {
        return jobBuilderFactory.get("updateShipmentStatusesJob")
                .start(updateShipmentStatusesStep())
                .build();
    }

    @Bean
    public Step updateShipmentStatusesStep() {
        return stepBuilderFactory.get("updateShipmentStatusesStep")
                .<Shipment, Shipment>chunk(100)
                .reader(shipmentReader)
                .processor(shipmentProcessor)
                .writer(shipmentWriter)
                .build();
    }
}
```

## 외부 서비스 연동 설정

### RestTemplateConfig

REST API 호출을 위한 RestTemplate 설정입니다.

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### 주요 기능

- HTTP 요청을 보내기 위한 RestTemplate 빈을 제공합니다.

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class ExternalApiService {
    private final RestTemplate restTemplate;
    
    public ExternalDataDto fetchExternalData(String resourceId) {
        String url = "https://api.external-service.com/resources/" + resourceId;
        return restTemplate.getForObject(url, ExternalDataDto.class);
    }
    
    public void sendNotification(NotificationDto notification) {
        String url = "https://api.notification-service.com/send";
        restTemplate.postForEntity(url, notification, Void.class);
    }
}
```

### GPTConfig

OpenAI GPT API 연동을 위한 설정입니다.

```java
@Configuration
public class GPTConfig {
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
```

### 주요 기능

- **API 키 관리**: OpenAI API 키를 설정 파일에서 가져옵니다.
- **모델 선택**: 사용할 GPT 모델을 설정합니다 (기본값: gpt-3.5-turbo).
- **API 엔드포인트**: OpenAI API 엔드포인트 URL을 설정합니다.

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class GPTService {
    private final RestTemplate restTemplate;
    private final GPTConfig gptConfig;
    
    public String generateProductDescription(String productName, String category) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gptConfig.getApiKey());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", gptConfig.getModel());
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "You are a product description writer."),
            Map.of("role", "user", "content", "Write a compelling description for a " + category + " product called '" + productName + "'")
        ));
        requestBody.put("max_tokens", 300);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            gptConfig.getApiUrl(),
            request,
            Map.class
        );
        
        // 응답 처리 로직...
        return extractContentFromResponse(response.getBody());
    }
}
```

## 애플리케이션 생명주기 관리

### ApplicationShutdownConfig

애플리케이션 종료 시 실행될 작업을 설정하는 클래스입니다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationShutdownConfig {
    private final StyleViewLogBufferManager bufferManager;

    @Bean
    public ApplicationListener<ContextClosedEvent> shutdownHook() {
        return event -> {
            log.info("Application shutdown hook triggered");
            bufferManager.flushBufferOnShutdown();
            log.info("Application shutdown tasks completed");
        };
    }
}
```

### 주요 기능

- **종료 이벤트 감지**: 애플리케이션 컨텍스트가 종료될 때 이벤트를 감지합니다.
- **버퍼 데이터 저장**: 메모리에 있는 로그 데이터를 디스크에 저장합니다.
- **안전한 종료**: 애플리케이션이 안전하게 종료되도록 리소스를 정리합니다.

### 사용 예시

```java
@Component
@Slf4j
public class StyleViewLogBufferManager {
    private final List<StyleViewLog> buffer = new ArrayList<>();
    private final StyleViewLogRepository repository;
    
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    public void flushBuffer() {
        log.debug("스타일 조회 로그 버퍼 플러시 시작, 누적 항목: {}", buffer.size());
        
        if (buffer.isEmpty()) {
            return;
        }
        
        List<StyleViewLog> toSave;
        synchronized (buffer) {
            toSave = new ArrayList<>(buffer);
            buffer.clear();
        }
        
        try {
            repository.saveAll(toSave);
            log.info("스타일 조회 로그 {} 건 저장 완료", toSave.size());
        } catch (Exception e) {
            log.error("스타일 조회 로그 저장 실패", e);
            // 저장 실패 시 버퍼에 다시 추가
            synchronized (buffer) {
                buffer.addAll(toSave);
            }
        }
    }
    
    public void flushBufferOnShutdown() {
        log.info("애플리케이션 종료 시 로그 버퍼 플러시");
        flushBuffer();
    }
}
```

## 카프카 관련 설정

### UserAccessLogKafkaConfig

사용자 접근 로그를 위한 Kafka 설정 클래스입니다.

```java
@Configuration
@EnableKafka
public class UserAccessLogKafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, UserAccessLogEvent> userAccessLogProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, UserAccessLogEvent> userAccessLogKafkaTemplate() {
        return new KafkaTemplate<>(userAccessLogProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, UserAccessLogEvent> userAccessLogConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(UserAccessLogEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserAccessLogEvent> userAccessLogKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserAccessLogEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userAccessLogConsumerFactory());
        return factory;
    }
}
```

### 주요 기능

- **카프카 프로듀서 설정**: 사용자 접근 로그 이벤트를 카프카로 전송하기 위한 설정을 제공합니다.
- **카프카 컨슈머 설정**: 사용자 접근 로그 이벤트를 카프카에서 수신하기 위한 설정을 제공합니다.
- **직렬화/역직렬화 설정**: JSON 형식으로 메시지를 변환하는 설정을 제공합니다.

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class UserAccessLogService {
    private final KafkaTemplate<String, UserAccessLogEvent> kafkaTemplate;
    private final UserAccessLogRepository userAccessLogRepository;
    
    public void logUserAccess(String userId, String uri, String method, String ip) {
        UserAccessLogEvent event = UserAccessLogEvent.builder()
                .userId(userId)
                .uri(uri)
                .method(method)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
                
        kafkaTemplate.send("user-access-logs", event);
    }
    
    @KafkaListener(
        topics = "user-access-logs",
        containerFactory = "userAccessLogKafkaListenerContainerFactory"
    )
    public void processUserAccessLog(UserAccessLogEvent event) {
        // 로그 처리 로직
        UserAccessLog log = UserAccessLog.builder()
                .userId(event.getUserId())
                .uri(event.getUri())
                .method(event.getMethod())
                .ip(event.getIp())
                .accessTime(event.getTimestamp())
                .build();
                
        userAccessLogRepository.save(log);
    }
}
```.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(UserAccessLogEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserAccessLogEvent> userAccessLogKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserAccessLogEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userAccessLogConsumerFactory());
        return factory;
    }
}
```

### 주요 기능

- **카프카 프로듀서 설정**: 사용자 접근 로그 이벤트를 카프카로 전송하기 위한 설정을 제공합니다.
- **카프카 컨슈머 설정**: 사용자 접근 로그 이벤트를 카프카에서 수신하기 위한 설정을 제공합니다.
- **직렬화/역직렬화 설정**: JSON 형식으로 메시지를 변환하는 설정을 제공합니다.

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class UserAccessLogService {
    private final KafkaTemplate<String, UserAccessLogEvent> kafkaTemplate;
    
    public void logUserAccess(String userId, String uri, String method, String ip) {
        UserAccessLogEvent event = UserAccessLogEvent.builder()
                .userId(userId)
                .uri(uri)
                .method(method)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
                
        kafkaTemplate.send("user-access-logs", event);
    }
    
    @KafkaListener(
        topics = "user-access-logs",
        containerFactory = "userAccessLogKafkaListenerContainerFactory"
    )
    public void processUserAccessLog(UserAccessLogEvent event) {
        // 로그 처리 로직
        userAccessLogRepository.save(convertToEntity(event));
    }
}
```