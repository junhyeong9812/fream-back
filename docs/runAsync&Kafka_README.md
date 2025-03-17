# CompletableFuture.runAsync vs Kafka 비동기 처리 비교

## 개요

이 문서는 Java의 `CompletableFuture.runAsync()`와 Apache Kafka를 사용한 비동기 처리 방식의 차이점, 장단점, 그리고 적합한 사용 시나리오를 설명합니다.

## 1. CompletableFuture.runAsync

### 작동 방식

`CompletableFuture.runAsync()`는 Java 8에서 도입된 비동기 프로그래밍 API의 일부입니다. 이 메서드는 Runnable 태스크를 비동기적으로 실행하고 그 실행 완료를 나타내는 CompletableFuture를 반환합니다.

```java
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    // 비동기적으로 실행될 코드
    // 예: 데이터베이스 저장, 외부 API 호출, 파일 처리 등
});
```

기본적으로 `ForkJoinPool.commonPool()`을 사용하지만, 명시적으로 Executor를 제공할 수도 있습니다:

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    // 비동기적으로 실행될 코드
}, executor);
```

### 장점

1. **구현 용이성**: 추가 인프라 없이 Java 표준 라이브러리로 즉시 구현 가능
2. **낮은 지연시간**: 동일 JVM 내에서 실행되므로 지연 시간이 최소화됨
3. **빠른 설정**: 추가 구성이나 서버 설정 없이 사용 가능
4. **트랜잭션 컨텍스트 공유**: 필요한 경우 동일한 트랜잭션 컨텍스트를 유지할 수 있음
5. **간단한 에러 처리**: try-catch 또는 exceptionally()를 사용한 간단한 예외 처리
6. **직관적인 API**: 체이닝, 조합 등을 위한 풍부한 API 제공

### 단점

1. **서버 장애 복구 불가**: 서버가 다운되면 진행 중인 작업이 모두 손실됨
2. **확장성 제한**: 단일 서버의 리소스에 의존하므로 대규모 확장에 한계
3. **분산 처리 지원 제한**: 여러 서버 간 작업 분산에 추가 구성 필요
4. **메모리 제약**: JVM 힙 메모리에 의존하므로 대용량 작업에 제한
5. **모니터링 제한**: 기본적인 모니터링 도구만 제공

### 적합한 시나리오

1. **간단한 비동기 작업**: 빠르게 완료되는 단순한 작업
2. **단일 서버 애플리케이션**: 분산 아키텍처가 필요 없는 경우
3. **낮은 지연 시간이 중요한 경우**: 사용자 응답 시간이 중요한 상황
4. **시스템 규모가 작거나 중간 정도**: 동시 요청이 관리 가능한 수준인 경우
5. **작업 손실이 허용되는 경우**: 실패해도 다시 시도할 수 있는 작업

## 2. Apache Kafka를 활용한 비동기 처리

### 작동 방식

Kafka는 분산 이벤트 스트리밍 플랫폼으로, 발행-구독(pub-sub) 메시징 시스템을 제공합니다. 비동기 처리를 위해서는 다음과 같은 구성요소가 필요합니다:

- **Producer**: 메시지를 생성하여 토픽에 발행
- **Broker**: 메시지를 저장하고 관리하는 Kafka 서버
- **Topic**: 메시지가 게시되는 카테고리
- **Consumer**: 토픽으로부터 메시지를 구독하고 처리

```java
// 메시지 생산 (Producer)
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

Producer<String, String> producer = new KafkaProducer<>(props);
producer.send(new ProducerRecord<>("topic-name", "key", "value"));

// 메시지 소비 (Consumer)
Properties consumerProps = new Properties();
consumerProps.put("bootstrap.servers", "localhost:9092");
consumerProps.put("group.id", "consumer-group");
consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
consumer.subscribe(Collections.singletonList("topic-name"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        // 비동기 작업 처리
    }
}
```

### 장점

1. **높은 내구성**: 메시지 영속성으로 시스템 장애에도 데이터 보존
2. **대규모 확장성**: 분산 아키텍처로 대용량 처리 가능
3. **장애 허용성**: 브로커 장애 시에도 복제본을 통한 운영 지속
4. **부하 분산**: 파티션과 소비자 그룹을 통한 효율적인 부하 분산
5. **재처리 가능**: 오프셋 관리를 통한 메시지 재처리 지원
6. **이벤트 소싱**: 이벤트 기반 아키텍처에 적합
7. **다양한 통합 지원**: 다양한 시스템과의 연동 용이
8. **풍부한 모니터링 도구**: 다양한 모니터링 및 관리 도구 제공

### 단점

1. **복잡한 인프라**: Kafka 클러스터 설정 및 관리 필요
2. **높은 지연 시간**: 메시지 발행, 전송, 소비 과정에서 추가 지연 발생
3. **리소스 요구사항**: 브로커, ZooKeeper 등 추가 리소스 필요
4. **구현 및 운영 복잡성**: 프로듀서/컨슈머 설정, 파티셔닝, 오프셋 관리 등 복잡성
5. **학습 곡선**: 개념 이해와 효율적인 운영을 위한 학습 필요
6. **개발 및 테스트 복잡성**: 로컬 개발 환경 구성이 복잡할 수 있음

### 적합한 시나리오

1. **대규모 시스템**: 수백만 사용자/이벤트 처리가 필요한 경우
2. **높은 신뢰성 요구**: 메시지 손실이 허용되지 않는 중요 작업
3. **이벤트 기반 아키텍처**: 이벤트 소싱/CQRS 패턴 구현
4. **마이크로서비스 통합**: 여러 서비스 간 비동기 통신
5. **데이터 파이프라인**: 실시간 데이터 처리 및 분석
6. **분산 시스템**: 여러 서버/데이터센터에 분산된 시스템

## 3. 주요 차이점 비교

| 측면 | CompletableFuture.runAsync | Apache Kafka |
|------|---------------------------|--------------|
| **구현 복잡성** | 낮음 | 높음 |
| **지연 시간** | 낮음 | 높음 |
| **확장성** | 제한적 | 우수함 |
| **내구성** | 낮음 (서버 다운 시 손실) | 높음 (메시지 영속성) |
| **부하 분산** | 제한적 | 우수함 |
| **리소스 요구사항** | 낮음 | 높음 |
| **운영 복잡성** | 낮음 | 높음 |
| **재처리 기능** | 제한적 | 우수함 |
| **모니터링** | 기본적 | 광범위함 |
| **적합한 규모** | 소규모 ~ 중간 규모 | 중간 규모 ~ 대규모 |

## 4. 사용 사례별 권장 선택

### CompletableFuture.runAsync 권장 사례

1. **웹 애플리케이션 응답 최적화**:
    - 사용자 요청 처리 후 알림 전송, 로깅 등 부수적 작업
    - 예: 주문 완료 후 이메일 전송, 로그 기록

2. **단기 백그라운드 작업**:
    - 짧은 시간 내에 완료되는 작업
    - 예: 이미지 크기 조정, 간단한 데이터 변환

3. **내부 시스템 작업**:
    - 단일 애플리케이션 내 비동기 처리
    - 예: 캐시 갱신, 일괄 DB 업데이트

4. **로그인 토큰 관리**:
    - JWT 토큰 생성 및 Redis 저장과 같은 간단한 작업
    - 메모리 캐시 갱신

### Kafka 권장 사례

1. **마이크로서비스 간 통신**:
    - 서비스 간 이벤트 기반 통신
    - 예: 주문 서비스에서 발생한 이벤트를 배송, 결제 서비스로 전파

2. **대용량 데이터 처리**:
    - 대량의 이벤트/메시지 처리
    - 예: IoT 센서 데이터, 사용자 활동 로그, 클릭스트림 분석

3. **중요 업무 이벤트 처리**:
    - 높은 신뢰성이 요구되는 업무 처리
    - 예: 금융 거래, 결제 처리

4. **실시간 데이터 파이프라인**:
    - 실시간 데이터 수집 및 처리
    - 예: 실시간 분석, 모니터링, 대시보드

## 5. 하이브리드 접근법

많은 시스템에서는 두 방식을 함께 사용하는 하이브리드 접근법이 효과적일 수 있습니다:

1. **즉각적인 응답이 필요한 작업**:
    - CompletableFuture.runAsync() 사용
    - 예: 사용자 인터페이스 응답, 토큰 검증

2. **중요한 비즈니스 이벤트**:
    - Kafka 사용
    - 예: 주문 생성, 결제 처리, 계정 변경

3. **단계적 도입**:
    - 초기에는 CompletableFuture로 시작하여 필요에 따라 Kafka로 마이그레이션
    - 특정 도메인이나 중요한 이벤트만 Kafka로 처리

## 6. 결론

비동기 처리 방식의 선택은 시스템 요구사항, 규모, 복잡성에 따라 달라집니다:

- **CompletableFuture.runAsync()**는 간단한 구현, 낮은 지연 시간, 빠른 개발이 중요한 경우에 적합합니다.
- **Apache Kafka**는 높은 확장성, 내구성, 신뢰성이 필요한 대규모 분산 시스템에 적합합니다.

각 접근 방식의 장단점을 이해하고 시스템 요구사항에 맞게 선택하거나 결합하는 것이 중요합니다. 작은 규모로 시작하여 필요에 따라 확장하는 접근법이 많은 경우에 효과적입니다.