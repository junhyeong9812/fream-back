# Kafka 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 Kafka 관련 설정을 포함합니다.

## UserAccessLogKafkaConfig

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
```

## OrderEventKafkaConfig

주문 이벤트를 위한 Kafka 설정 클래스입니다.

```java
@Configuration
@EnableKafka
public class OrderEventKafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, OrderEvent> orderEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> orderEventKafkaTemplate() {
        return new KafkaTemplate<>(orderEventProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, OrderEvent> orderEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processor");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(OrderEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> orderEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory());
        return factory;
    }
}
```

### 주문 이벤트 처리 예시

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    
    // 주문 생성 및 이벤트 발행
    @Transactional
    public Order placeOrder(OrderRequest request) {
        // 주문 생성 로직
        Order order = Order.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.PENDING)
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // 주문 생성 이벤트 발행
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .productId(savedOrder.getProductId())
                .quantity(savedOrder.getQuantity())
                .totalAmount(savedOrder.getTotalAmount())
                .status(OrderStatus.PENDING)
                .eventType(OrderEventType.ORDER_CREATED)
                .timestamp(LocalDateTime.now())
                .build();
                
        kafkaTemplate.send("order-events", event);
        
        return savedOrder;
    }
    
    // 주문 이벤트 수신 및 처리
    @KafkaListener(
        topics = "order-events",
        containerFactory = "orderEventKafkaListenerContainerFactory"
    )
    public void processOrderEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case ORDER_CREATED:
                // 재고 확인 및 결제 처리
                processNewOrder(event);
                break;
            case PAYMENT_COMPLETED:
                // 결제 완료 처리
                completeOrder(event.getOrderId());
                break;
            case ORDER_SHIPPED:
                // 배송 처리
                updateOrderStatus(event.getOrderId(), OrderStatus.SHIPPED);
                break;
            default:
                log.warn("지원하지 않는 주문 이벤트 타입: {}", event.getEventType());
        }
    }
    
    private void processNewOrder(OrderEvent event) {
        // 재고 확인
        boolean isInStock = inventoryService.checkStock(event.getProductId(), event.getQuantity());
        
        if (isInStock) {
            // 결제 처리
            try {
                PaymentResult result = paymentService.processPayment(
                    event.getUserId(), event.getOrderId(), event.getTotalAmount());
                
                if (result.isSuccess()) {
                    // 결제 완료 이벤트 발행
                    OrderEvent paymentCompletedEvent = OrderEvent.builder()
                            .orderId(event.getOrderId())
                            .userId(event.getUserId())
                            .eventType(OrderEventType.PAYMENT_COMPLETED)
                            .timestamp(LocalDateTime.now())
                            .build();
                            
                    kafkaTemplate.send("order-events", paymentCompletedEvent);
                } else {
                    // 결제 실패 처리
                    handlePaymentFailure(event.getOrderId(), result.getErrorMessage());
                }
            } catch (Exception e) {
                // 결제 처리 중 오류 발생
                handlePaymentFailure(event.getOrderId(), e.getMessage());
            }
        } else {
            // 재고 부족 처리
            handleOutOfStock(event.getOrderId(), event.getProductId());
        }
    }
    
    private void completeOrder(Long orderId) {
        updateOrderStatus(orderId, OrderStatus.PAID);
    }
    
    private void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));
                
        order.setStatus(status);
        orderRepository.save(order);
    }
    
    private void handlePaymentFailure(Long orderId, String errorMessage) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));
                
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setStatusMessage(errorMessage);
        orderRepository.save(order);
    }
    
    private void handleOutOfStock(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다: " + orderId));
                
        order.setStatus(OrderStatus.CANCELLED);
        order.setStatusMessage("재고 부족: 상품 ID " + productId);
        orderRepository.save(order);
    }
}
```

## 알림 이벤트 설정

알림 이벤트를 위한 Kafka 설정 클래스입니다.

```java
@Configuration
@EnableKafka
public class NotificationKafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, NotificationEvent> notificationProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate() {
        return new KafkaTemplate<>(notificationProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, NotificationEvent> notificationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-processor");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(NotificationEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> notificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory());
        return factory;
    }
}
```

### 알림 시스템 구현 예시

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    // 알림 이벤트 발행
    public void sendNotification(String userId, NotificationType type, String message, Map<String, String> data) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
                
        kafkaTemplate.send("notification-events", event);
    }
    
    // 알림 이벤트 수신 및 웹소켓으로 실시간 전송
    @KafkaListener(
        topics = "notification-events",
        containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void processNotification(NotificationEvent event) {
        // 알림 저장
        saveNotification(event);
        
        // 웹소켓으로 실시간 알림 전송
        NotificationDto dto = NotificationDto.builder()
                .id(event.getId())
                .type(event.getType().name())
                .message(event.getMessage())
                .data(event.getData())
                .timestamp(event.getTimestamp())
                .read(false)
                .build();
                
        messagingTemplate.convertAndSendToUser(
            event.getUserId(),  // 수신자 이메일/ID
            "/queue/notifications",  // 목적지 경로
            dto  // 전송할 데이터
        );
    }
    
    private void saveNotification(NotificationEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .message(event.getMessage())
                .data(String.valueOf(event.getData())) // JSON으로 변환
                .timestamp(event.getTimestamp())
                .read(false)
                .build();
                
        notificationRepository.save(notification);
    }
}
```

## Kafka 토픽 관리

Kafka 토픽 자동 생성을 위한 설정입니다.

```java
@Configuration
public class KafkaTopicConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }
    
    @Bean
    public NewTopic userAccessLogTopic() {
        // 토픽명, 파티션 수, 복제 팩터
        return new NewTopic("user-access-logs", 3, (short) 1);
    }
    
    @Bean
    public NewTopic orderEventsTopic() {
        return new NewTopic("order-events", 5, (short) 1);
    }
    
    @Bean
    public NewTopic notificationEventsTopic() {
        return new NewTopic("notification-events", 3, (short) 1);
    }
    
    @Bean
    public NewTopic inventoryChangesTopic() {
        return new NewTopic("inventory-changes", 3, (short) 1);
    }
}
```