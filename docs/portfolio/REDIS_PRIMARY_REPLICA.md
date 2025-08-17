# Redis Primary-Replica 클러스터 구성 가이드

## 📋 목차
1. [개요](#개요)
2. [Redis Primary-Replica 핵심 개념](#redis-primary-replica-핵심-개념)
3. [아키텍처 설계](#아키텍처-설계)
4. [Docker 설정](#docker-설정)
5. [Spring Boot 설정](#spring-boot-설정)
6. [AuthRedisService 구현](#authredisservice-구현)
7. [모니터링 및 운영](#모니터링-및-운영)
8. [트러블슈팅](#트러블슈팅)
9. [성능 최적화](#성능-최적화)
10. [확장성 고려사항](#확장성-고려사항)

---

## 개요

기존 단일 Redis 인스턴스에서 **Redis Primary-Replica 클러스터**로 확장하여 다음 목표를 달성합니다:

### 목표
- **읽기 성능 향상**: Replica에서 읽기 분산 처리
- **고가용성**: 단일 장애점 제거
- **확장성**: 트래픽 증가에 대응
- **기존 API 호환성**: 코드 변경 최소화

### Before & After
```
[Before] 단일 Redis
Client → Redis (Read/Write)

[After] Primary-Replica 클러스터
Client → Primary (Write)
Client → Replica (Read) → Primary (Fallback)
```

---

## Redis Primary-Replica 핵심 개념

### 1. Replication 메커니즘

#### 비동기 복제 (Asynchronous Replication)
```
1. Client → Primary: WRITE 명령
2. Primary → Client: 즉시 응답 (동기화 대기 안함)
3. Primary → Replica: 명령 전송 (백그라운드)
4. Replica: 명령 실행
```

**특징:**
- ✅ **고성능**: Primary가 응답 지연 없음
- ⚠️ **Eventual Consistency**: 복제 지연 가능
- ⚠️ **데이터 손실 위험**: Primary 장애 시 최신 데이터 손실 가능

#### 복제 과정
```
초기 동기화 (Full Sync):
1. Replica → Primary 연결 요청
2. Primary → RDB 스냅샷 생성
3. Primary → Replica로 RDB 전송
4. Replica → RDB 로드
5. Primary → 동기화 중 발생한 명령어들 전송

증분 동기화 (Incremental Sync):
1. Primary → 모든 쓰기 명령을 Replica로 실시간 전송
2. Replica → 동일한 명령 실행
```

### 2. Read/Write 분리 패턴

| 작업 유형 | 대상 인스턴스 | 이유 |
|----------|--------------|------|
| **Write** | Primary | 데이터 일관성 보장 |
| **Read** | Replica | 읽기 성능 향상 |
| **Critical Read** | Primary | 최신 데이터 보장 |
| **Fallback Read** | Primary | Replica 장애 대응 |

### 3. Consistency 모델

#### Strong Consistency (강한 일관성)
```java
// 중요한 데이터는 항상 Primary에서 읽기
public boolean isAccessTokenValid(String token) {
    return primaryRedis.hasKey("access:" + token);
}
```

#### Eventual Consistency (최종 일관성)
```java
// 캐시 데이터는 Replica 우선, Fallback 허용
public String getCachedData(String key) {
    return replicaRedis.get(key); // Fallback to Primary
}
```

---

## 아키텍처 설계

### 1. 전체 아키텍처

```
┌─────────────────┐    ┌──────────────────┐
│   Application   │    │   Application    │
│    Server 1     │    │    Server 2      │
└─────────────────┘    └──────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
┌─────────┐  ┌─────────────┐  ┌─────────┐
│ Primary │  │   Replica   │  │ Primary │
│ (Write) │◄─│   (Read)    │  │(Fallback)│
└─────────┘  └─────────────┘  └─────────┘
     │              ▲
     └──────────────┘
        Replication
```

### 2. 데이터 플로우

#### Write 플로우
```
1. AuthRedisService.addAccessToken()
2. → writeRedisTemplate (Primary)
3. → Primary Redis 저장
4. → Primary가 Replica로 비동기 복제
```

#### Read 플로우
```
1. AuthRedisService.getEmailByAccessToken()
2. → readRedisTemplate (Replica) 시도
3. → 성공 시 반환
4. → 실패/null 시 writeRedisTemplate (Primary) 재시도
5. → 최종 결과 반환
```

### 3. 연결 관리

```java
// 3개의 RedisTemplate Bean
┌─────────────────────┐
│ redisTemplate       │ ← Primary-Replica 자동 분산
│ (기본, 호환성)       │
├─────────────────────┤
│ writeRedisTemplate  │ ← Primary 전용 (쓰기)
│ (Primary Only)      │
├─────────────────────┤
│ readRedisTemplate   │ ← Replica 전용 (읽기)
│ (Replica Only)      │
└─────────────────────┘
```

---

## Docker 설정

### 1. docker-compose.yml

```yaml
services:
  redis-primary:
    build:
      context: ../..
      dockerfile: docker/redis/Dockerfile-redis-primary
    container_name: redis_primary_prod
    environment:
      TZ: Asia/Seoul
    ports:
      - "6379:6379"
    volumes:
      - C:\Users\pickj\webserver\dockerVolums\redis_primary_data:/data

  redis-replica:
    build:
      context: ../..
      dockerfile: docker/redis/Dockerfile-redis-replica
    container_name: redis_replica_prod
    environment:
      TZ: Asia/Seoul
    ports:
      - "6380:6379"
    depends_on:
      - redis-primary
    volumes:
      - C:\Users\pickj\webserver\dockerVolums\redis_replica_data:/data
```

### 2. Redis 설정 파일

#### Primary 설정 (redis-primary.conf)
```conf
# 네트워크 설정
bind 0.0.0.0
port 6379

# 데이터 지속성
save 900 1
save 300 10
save 60 10000
rdbcompression yes
dbfilename dump.rdb
dir /data

# AOF 설정
appendonly yes
appendfsync everysec

# 복제 설정 (Primary는 별도 설정 불필요)
# Replica가 연결하면 자동으로 동기화 시작
```

#### Replica 설정 (redis-replica.conf)
```conf
# 네트워크 설정
bind 0.0.0.0
port 6379

# 복제 설정 - 핵심!
replicaof redis-primary 6379  # Primary 서버 지정
replica-read-only yes         # 읽기 전용 모드

# 데이터 지속성
save 900 1
save 300 10
save 60 10000
rdbcompression yes
dbfilename dump.rdb
dir /data

# AOF 설정
appendonly yes
appendfsync everysec

# 복제 관련 추가 설정
replica-serve-stale-data yes  # 연결 끊어져도 기존 데이터 제공
replica-priority 100          # Failover 우선순위
```

### 3. Dockerfile

#### Dockerfile-redis-primary
```dockerfile
FROM redis:7.0

COPY redis-primary.conf /usr/local/etc/redis/redis.conf

EXPOSE 6379

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
```

#### Dockerfile-redis-replica
```dockerfile
FROM redis:7.0

COPY redis-replica.conf /usr/local/etc/redis/redis.conf

EXPOSE 6379

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]
```

---

## Spring Boot 설정

### 1. application.yml

```yaml
spring:
  redis:
    primary:
      host: redis-primary
      port: 6379
    replica:
      host: redis-replica
      port: 6380
    timeout: 2000
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        time-between-eviction-runs: 30000
```

### 2. RedisConfig.java

```java
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
     * Primary-Replica 자동 분산 ConnectionFactory
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStaticMasterReplicaConfiguration configuration = 
                new RedisStaticMasterReplicaConfiguration(primaryHost, primaryPort);
        configuration.addNode(replicaHost, replicaPort);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED) // Replica 우선, 없으면 Primary
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .build();

        return new LettuceConnectionFactory(configuration, clientConfig);
    }

    /**
     * 쓰기 전용 Primary ConnectionFactory
     */
    @Bean("primaryRedisConnectionFactory")
    public RedisConnectionFactory primaryRedisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .build();

        RedisStandaloneConfiguration standaloneConfig = 
                new RedisStandaloneConfiguration(primaryHost, primaryPort);
        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    /**
     * 읽기 전용 Replica ConnectionFactory
     */
    @Bean("replicaRedisConnectionFactory")
    public RedisConnectionFactory replicaRedisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeoutMs))
                .build();

        RedisStandaloneConfiguration standaloneConfig = 
                new RedisStandaloneConfiguration(replicaHost, replicaPort);
        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    /**
     * 기본 RedisTemplate (Primary-Replica 자동 분산)
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 쓰기 전용 RedisTemplate (Primary 전용)
     */
    @Bean("writeRedisTemplate")
    public RedisTemplate<String, String> writeRedisTemplate() {
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
     * 읽기 전용 RedisTemplate (Replica 전용)
     */
    @Bean("readRedisTemplate")
    public RedisTemplate<String, String> readRedisTemplate() {
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
```

---

## AuthRedisService 구현

### 1. 핵심 설계 원칙

- **API 호환성**: 기존 메서드 시그니처 100% 유지
- **Read from Replica**: 읽기 작업은 Replica 우선
- **Write to Primary**: 쓰기 작업은 Primary 전용
- **Safe Fallback**: Replica 실패 시 Primary로 자동 전환

### 2. 주요 메서드 구현

#### Write 작업 (Primary 전용)
```java
public void addAccessToken(String accessToken, String email, Integer age, 
                          Gender gender, long expirationMillis, String ip, Role role) {
    try {
        String key = "access:" + accessToken;
        Map<String, String> fields = new HashMap<>();
        fields.put("email", email);
        fields.put("age", String.valueOf(age));
        fields.put("gender", gender.toString());
        fields.put("role", role.toString());
        if (ip != null) fields.put("ip", ip);

        // Primary Redis에 쓰기
        writeRedisTemplate.opsForHash().putAll(key, fields);
        writeRedisTemplate.expire(key, Duration.ofMillis(expirationMillis));

        log.info("Access Token 저장 완료 (Primary): email={}", email);
    } catch (Exception e) {
        log.error("Access Token 저장 실패: email={}", email, e);
        throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 저장 중 오류가 발생했습니다.", e);
    }
}
```

#### Read 작업 (Replica → Primary Fallback)
```java
public String getEmailByAccessToken(String accessToken) {
    try {
        Object value = safeReadFromReplica("access:" + accessToken, "email");
        return (value != null) ? value.toString() : null;
    } catch (Exception e) {
        log.error("Access Token으로 이메일 조회 실패", e);
        return null;
    }
}

private Object safeReadFromReplica(String key, String field) {
    try {
        // 1차: Replica에서 읽기 시도
        Object value = readRedisTemplate.opsForHash().get(key, field);
        
        if (value == null) {
            // 2차: Replication lag 대응 - Primary에서 재시도
            log.debug("Replica에서 읽기 실패, Primary에서 재시도: key={}", key);
            value = writeRedisTemplate.opsForHash().get(key, field);
        }
        
        return value;
        
    } catch (Exception replicaException) {
        // 3차: Replica 장애 시 Primary로 fallback
        log.warn("Replica Redis 접근 실패, Primary로 fallback: key={}", key, replicaException);
        try {
            return writeRedisTemplate.opsForHash().get(key, field);
        } catch (Exception primaryException) {
            log.error("Primary Redis 접근도 실패: key={}", key, primaryException);
            return null;
        }
    }
}
```

### 3. Fallback 전략

| 상황 | 1차 시도 | 2차 시도 | 3차 시도 |
|------|---------|---------|---------|
| **정상** | Replica ✅ | - | - |
| **Replication Lag** | Replica (null) | Primary ✅ | - |
| **Replica 장애** | Replica ❌ | Primary ✅ | - |
| **전체 장애** | Replica ❌ | Primary ❌ | Exception |

---

## 모니터링 및 운영

### 1. 동기화 상태 모니터링

#### 수동 확인
```bash
# Primary 상태 확인
docker exec redis_primary_prod redis-cli INFO replication

# 출력 예시:
# role:master
# connected_slaves:1
# slave0:ip=172.18.0.3,port=6379,state=online,offset=123456,lag=0

# Replica 상태 확인
docker exec redis_replica_prod redis-cli INFO replication

# 출력 예시:
# role:slave
# master_host:redis-primary
# master_port:6379
# master_link_status:up
# master_sync_in_progress:0
```

#### 자동 모니터링 서비스
```java
@Service
public class RedisMonitoringService {

    @Scheduled(fixedRate = 300000) // 5분마다
    public void checkReplicationStatus() {
        try {
            // Primary 정보 조회
            Properties primaryInfo = writeRedisTemplate.execute(connection -> 
                connection.info("replication"));
            
            // Replica lag 확인
            String slaveInfo = primaryInfo.getProperty("slave0");
            if (slaveInfo != null && slaveInfo.contains("lag=")) {
                int lag = extractLag(slaveInfo);
                if (lag > 1000) { // 1초 이상 지연
                    alertService.sendAlert("Redis Replication Lag: " + lag + "ms");
                }
            }
        } catch (Exception e) {
            log.error("Replication monitoring failed", e);
        }
    }
}
```

### 2. 성능 메트릭

#### 모니터링 항목
- **Replication Lag**: 복제 지연 시간
- **Connection Count**: 연결 수
- **Memory Usage**: 메모리 사용량
- **Commands/sec**: 초당 명령 수
- **Hit Rate**: 캐시 히트율

#### Grafana 대시보드 설정
```yaml
# prometheus.yml에 Redis exporter 추가
- job_name: 'redis-primary'
  static_configs:
    - targets: ['redis-primary:6379']
    
- job_name: 'redis-replica'
  static_configs:
    - targets: ['redis-replica:6379']
```

### 3. 알림 설정

#### 핵심 알림 조건
```yaml
alerts:
  - alert: RedisReplicationLag
    expr: redis_replication_lag_seconds > 5
    labels:
      severity: warning
    annotations:
      summary: "Redis replication lag is high"

  - alert: RedisReplicaDown
    expr: up{job="redis-replica"} == 0
    labels:
      severity: critical
    annotations:
      summary: "Redis replica is down"

  - alert: RedisPrimaryDown
    expr: up{job="redis-primary"} == 0
    labels:
      severity: critical
    annotations:
      summary: "Redis primary is down - immediate action required"
```

---

## 트러블슈팅

### 1. 일반적인 문제들

#### 동기화 실패
```bash
# 증상: Replica에서 데이터가 보이지 않음

# 진단
docker exec redis_replica_prod redis-cli INFO replication
# master_link_status가 down인 경우

# 해결방법
1. 네트워크 연결 확인
2. Primary Redis 상태 확인
3. Replica 재시작
docker restart redis_replica_prod
```

#### Replication Lag 증가
```bash
# 증상: Primary와 Replica 데이터 차이 발생

# 진단
docker exec redis_primary_prod redis-cli INFO replication
# slave0 항목에서 lag 값 확인

# 해결방법
1. 네트워크 대역폭 확인
2. Primary 부하 확인
3. Redis 설정 튜닝:
   - repl-backlog-size 증가
   - tcp-keepalive 설정
```

#### Primary 장애 시 대응
```bash
# 1. Replica를 Primary로 승격
docker exec redis_replica_prod redis-cli SLAVEOF NO ONE

# 2. 애플리케이션 설정 변경
# redis.primary.host를 replica 주소로 임시 변경

# 3. 원본 Primary 복구 후 역할 교체
docker exec redis_original_primary redis-cli SLAVEOF redis-replica 6379
```

### 2. 성능 최적화

#### Redis 설정 튜닝
```conf
# redis.conf 최적화

# 메모리 관리
maxmemory 2gb
maxmemory-policy allkeys-lru

# 네트워크 최적화
tcp-keepalive 300
timeout 0

# 복제 최적화
repl-backlog-size 64mb
repl-backlog-ttl 3600

# 지속성 최적화
save 900 1
save 300 10
save 60 10000
```

#### 애플리케이션 최적화
```java
// 커넥션 풀 최적화
@Bean
public LettuceConnectionFactory lettuceConnectionFactory() {
    GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = 
            new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(50);        // 최대 연결 수
    poolConfig.setMaxIdle(20);         // 유휴 연결 수
    poolConfig.setMinIdle(5);          // 최소 연결 수
    poolConfig.setTestOnBorrow(true);  // 연결 검증
    poolConfig.setTestWhileIdle(true); // 유휴 연결 검증
    
    return new LettuceConnectionFactory(redisConfig, poolConfig);
}
```

---

## 확장성 고려사항

### 1. 확장 전략

#### 수직 확장 (Scale Up)
```yaml
# 리소스 증설
services:
  redis-primary:
    deploy:
      resources:
        limits:
          memory: 8G
          cpus: '4.0'
        reservations:
          memory: 4G
          cpus: '2.0'
```

#### 수평 확장 (Scale Out)
```yaml
# 추가 Replica 구성
services:
  redis-replica-2:
    build:
      context: ../..
      dockerfile: docker/redis/Dockerfile-redis-replica
    container_name: redis_replica_2_prod
    ports:
      - "6381:6379"
    depends_on:
      - redis-primary
```

### 2. 고급 구성으로 마이그레이션

#### Redis Sentinel (고가용성)
```yaml
# 자동 Failover를 위한 Sentinel 구성
services:
  redis-sentinel-1:
    image: redis:7.0
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel.conf:/etc/redis/sentinel.conf
```

#### Redis Cluster (샤딩)
```yaml
# 데이터 분산을 위한 Cluster 구성 (6노드 최소)
services:
  redis-cluster-1:
    image: redis:7.0
    command: redis-server --cluster-enabled yes --cluster-config-file nodes.conf
```

### 3. 마이그레이션 계획

| 현재 단계 | 다음 단계 | 트리거 조건 |
|----------|----------|------------|
| **Primary-Replica** | Sentinel | Primary 장애로 인한 수동 대응 증가 |
| **Sentinel** | Cluster | 메모리 한계 도달 (>16GB) |
| **Cluster** | Redis Enterprise | 관리 복잡도 증가 |

---

## 주요 학습 포인트

### 1. 개념적 이해

#### Redis Replication 핵심
- **비동기 복제**: 성능 vs 일관성 트레이드오프
- **Pull 방식**: Replica가 Primary에 연결 요청
- **백그라운드 동기화**: 서비스 중단 없이 복제

#### Consistency 모델
- **Strong Consistency**: 중요 데이터는 Primary에서
- **Eventual Consistency**: 캐시 데이터는 Replica에서
- **Fallback Strategy**: 장애 시 자동 대응

### 2. 아키텍처적 고려사항

#### 설계 원칙
- **Read/Write 분리**: 성능과 확장성
- **Graceful Degradation**: 장애 시에도 서비스 지속
- **Backward Compatibility**: 기존 API 유지

#### 운영 관점
- **모니터링**: Lag, 연결 상태, 성능 지표
- **알림**: 임계값 기반 자동 알림
- **자동화**: 헬스체크, 로그 수집, 백업

### 3. 실전 경험

#### 트레이드오프 이해
- **성능 vs 일관성**: 비즈니스 요구사항에 따른 선택
- **복잡성 vs 안정성**: 단순함과 고가용성의 균형
- **비용 vs 확장성**: 리소스와 성능의 최적점

#### 점진적 확장
- **단계별 접근**: 단일 → Primary-Replica → Sentinel → Cluster
- **위험 최소화**: 검증된 패턴 적용
- **운영 경험 축적**: 각 단계에서 충분한 학습

---

## 참고 자료

### 공식 문서
- [Redis Replication](https://redis.io/docs/management/replication/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Lettuce Documentation](https://lettuce.io/core/release/reference/)

### 모니터링 도구
- [Redis Exporter for Prometheus](https://github.com/oliver006/redis_exporter)
- [RedisInsight](https://redis.com/redis-enterprise/redis-insight/)

### 베스트 프랙티스
- [Redis Best Practices](https://redis.io/docs/management/optimization/)
- [High Availability with Redis](https://redis.io/docs/management/sentinel/)

---

## 결론

이번 Redis Primary-Replica 클러스터 구성을 통해 다음을 달성했습니다:

1. **성능 향상**: 읽기 분산으로 응답 시간 개선
2. **안정성 확보**: 단일 장애점 제거 및 Fallback 메커니즘
3. **확장성 준비**: 트래픽 증가에 대응할 수 있는 구조
4. **운영 효율성**: 모니터링 및 알림 체계 구축

다음 단계로는 **Redis Sentinel** 도입을 통한 자동 Failover 구현을 고려할 수 있습니다.