# 이벤트 기반 설계 (Event-Driven Architecture)

## 1. 개요

이벤트 기반 설계(Event-Driven Architecture, EDA)는 시스템 내에서 발생하는 **이벤트(event)** 를 중심으로 구성 요소 간의 상호작용을 설계하는 아키텍처 스타일이다. 각 구성 요소는 이벤트를 **발행(publish)** 하고, 이를 **구독(subscribe)** 하는 방식으로 느슨하게 결합된다.

핵심 아이디어는 **발행자(Publisher)** 와 **구독자(Subscriber)** 간의 직접적인 의존성을 제거하여, 시스템의 확장성과 유연성을 극대화하는 것이다.

---

## 2. 주요 구성 요소

| 구성 요소                          | 역할                                                      |
| ------------------------------ | ------------------------------------------------------- |
| **Event**                      | 시스템 내에서 발생한 의미 있는 사건 (ex. 주문 생성, 결제 완료, 이메일 발송 요청)      |
| **Publisher**                  | 이벤트를 생성하고 브로커로 발행하는 주체                                  |
| **Subscriber (Event Handler)** | 특정 이벤트를 구독하고 발생 시 비즈니스 로직을 처리하는 주체                      |
| **Event Broker / Queue**       | 이벤트를 전달하고 비동기적으로 처리하도록 중개하는 메시지 시스템 (Kafka, RabbitMQ 등) |

---

## 3. 이벤트 흐름 구조

1. **도메인 이벤트 발생**: 비즈니스 로직 수행 중 도메인 내에서 이벤트 객체를 생성한다.

   ```java
   Events.raise(new OrderCreatedEvent(orderId));
   ```

2. **Publisher 발행**: 이벤트 퍼블리셔가 이벤트를 브로커 또는 내부 큐에 발행한다.

3. **Consumer / Handler 처리**: 구독자가 이벤트를 수신하여 비즈니스 로직을 수행한다.

4. **Event Log 관리 (선택적)**: 이벤트 처리 상태를 추적하기 위해 로그 테이블에 저장한다.

---

## 4. 설계 계층 구조

```plaintext
┌──────────────────────────┐
│       Domain Layer       │ ← 이벤트 생성 (OrderCreatedEvent 등)
└─────────────┬────────────┘
              │
┌─────────────▼────────────┐
│  Application Layer       │ ← 이벤트 발행 및 핸들러 등록
└─────────────┬────────────┘
              │
┌─────────────▼────────────┐
│ Infrastructure Layer     │ ← EventBus, Broker, Repository
└──────────────────────────┘
```

* **Domain Layer**: 이벤트 정의 및 발생 트리거.
* **Application Layer**: 이벤트 발행(Publisher) 및 핸들러(Subscriber) 관리.
* **Infrastructure Layer**: 브로커, 로그 저장소, 큐 시스템 연동.

---

## 5. 이벤트 설계 패턴

### 5.1 도메인 이벤트 (Domain Event)

도메인 내의 상태 변화를 표현하는 이벤트.

```java
public record OrderCreatedEvent(String orderId, LocalDateTime createdAt) implements DomainEvent {}
```

### 5.2 통합 이벤트 (Integration Event)

다른 마이크로서비스나 외부 시스템에 전달되는 이벤트.

```java
public record PaymentCompletedEvent(String paymentId, BigDecimal amount) implements IntegrationEvent {}
```

### 5.3 이벤트 핸들러 (Event Handler)

```java
@EventHandler
public class OrderEventHandler {
    public void handle(OrderCreatedEvent event) {
        notificationService.sendOrderConfirmation(event.orderId());
    }
}
```

---

## 6. 이벤트 저장 및 재처리

| 상태          | 설명                    |
| ----------- | --------------------- |
| **PENDING** | 이벤트가 발생했으나 아직 처리되지 않음 |
| **SUCCESS** | 이벤트가 정상적으로 처리됨        |
| **FAILED**  | 처리 중 예외 발생            |

재처리 전략:

* 실패 이벤트를 일정 시간 이후 재시도 (`@Scheduled` 기반)
* 이벤트 중복 방지를 위한 `eventId` 기준 멱등성 처리

---

## 7. 장점

* 서비스 간 **느슨한 결합**으로 유지보수 용이
* **비동기 처리**를 통한 성능 향상
* **확장성** 및 **신뢰성** 강화 (재시도, 로그 기반 복구 가능)

## 8. 단점

* 흐름 추적이 어려움 (비동기 디버깅 복잡)
* 중복 이벤트 처리 가능성 → 멱등성 보장 필요
* 트랜잭션 경계 관리 복잡 (SAGA 패턴 활용 가능)

---

## 9. 구현 예시 (Spring 기반)

```java
// 이벤트 퍼블리셔
@Component
@RequiredArgsConstructor
public class EventPublisher {
    private final ApplicationEventPublisher publisher;

    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}

// 이벤트 핸들러
@Component
public class OrderEventHandler {
    @EventListener
    public void handle(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리: {}", event.orderId());
    }
}
```

---

## 10. 확장 전략

* **Kafka / RabbitMQ 연동**으로 마이크로서비스 간 이벤트 통신
* **Outbox Pattern**: DB 트랜잭션과 이벤트 발행을 일관되게 유지
* **SAGA Pattern**: 분산 트랜잭션을 이벤트 체인으로 관리

---

## 11. 결론

이벤트 기반 설계는 시스템의 확장성과 복원력을 높여주며, 마이크로서비스 아키텍처에서 특히 강력한 패턴이다. 하지만 이벤트 중복, 트랜잭션 관리, 이벤트 흐름 추적 등 부가적인 고려사항이 필요하다. 따라서 **명확한 이벤트 경계 정의와 멱등성 보장 전략**이 필수적이다.
