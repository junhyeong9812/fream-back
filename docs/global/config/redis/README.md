# Redis 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 Redis 관련 설정을 포함합니다.

## RedisConfig

Redis 캐시 및 데이터 저장소 설정을 담당하는 클래스입니다.

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("redis", 6379);
        // 필요시 factory.setPassword("..."); etc.
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 필요시 serializer 설정
        return template;
    }
}
```

### 주요 기능

- **Redis 연결 설정**: Redis 서버 연결 설정을 제공합니다.
- **RedisTemplate 설정**: Redis 데이터 조작을 위한 템플릿을 제공합니다.
- **Docker 환경 호스트명**: Docker 컨테이너 네트워크에서 사용할 "redis" 호스트명을 사용합니다.

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
    
    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }
    
    public void deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

## Redis 사용 사례

### 1. 인증 토큰 관리

```java
@Service
@RequiredArgsConstructor
public class AuthRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 액세스 토큰 추가
    public void addAccessToken(String token, String email, Integer age, Gender gender, 
                              long expireTimeMs, String ip, Role role) {
        String key = "token:access:" + token;
        
        try {
            Map<String, Object> values = new HashMap<>();
            values.put("email", email);
            values.put("age", age);
            values.put("gender", gender.name());
            values.put("ip", ip);
            values.put("role", role.name());
            
            String jsonValue = objectMapper.writeValueAsString(values);
            redisTemplate.opsForValue().set(key, jsonValue);
            redisTemplate.expire(key, expireTimeMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Redis에 액세스 토큰 저장 실패", e);
        }
    }
    
    // 토큰 유효성 검증
    public boolean isAccessTokenValid(String token) {
        String key = "token:access:" + token;
        Boolean hasKey = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(hasKey);
    }
    
    // 토큰 정보 조회
    public String getEmailByAccessToken(String token) {
        String key = "token:access:" + token;
        String value = redisTemplate.opsForValue().get(key);
        
        try {
            if (value != null) {
                Map<String, Object> values = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
                return (String) values.get("email");
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰 정보 조회 실패", e);
        }
        
        return null;
    }
    
    // 토큰 무효화 (로그아웃 시)
    public void invalidateAccessToken(String token) {
        String key = "token:access:" + token;
        redisTemplate.delete(key);
    }
}
```

### 2. 상품 목록 캐싱

```java
@Service
@RequiredArgsConstructor
public class ProductCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final long CACHE_TTL_SECONDS = 300; // 5분
    
    // 상품 목록 캐싱
    public void cacheProductList(String category, List<ProductDto> products) {
        String key = "products:category:" + category;
        try {
            String jsonValue = objectMapper.writeValueAsString(products);
            redisTemplate.opsForValue().set(key, jsonValue);
            redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 캐싱 실패 시 로깅만 하고 예외는 던지지 않음 (성능 최적화 목적의 캐싱이므로)
            log.warn("상품 목록 캐싱 실패: {}", e.getMessage());
        }
    }
    
    // 캐시된 상품 목록 조회
    public List<ProductDto> getCachedProductList(String category) {
        String key = "products:category:" + category;
        String jsonValue = (String) redisTemplate.opsForValue().get(key);
        
        if (jsonValue != null) {
            try {
                return objectMapper.readValue(jsonValue, 
                    new TypeReference<List<ProductDto>>() {});
            } catch (Exception e) {
                log.warn("캐시된 상품 목록 파싱 실패: {}", e.getMessage());
            }
        }
        
        return null; // 캐시 미스
    }
    
    // 상품 목록 캐시 무효화 (상품 추가/수정/삭제 시)
    public void invalidateProductCache(String category) {
        String key = "products:category:" + category;
        redisTemplate.delete(key);
    }
}
```

### 3. 세션 관리

```java
@Service
@RequiredArgsConstructor
public class SessionService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long SESSION_TTL_SECONDS = 1800; // 30분
    
    // 세션 생성
    public String createSession(String userId, Map<String, Object> sessionData) {
        String sessionId = UUID.randomUUID().toString();
        String key = "session:" + sessionId;
        
        redisTemplate.opsForHash().putAll(key, sessionData);
        redisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        
        return sessionId;
    }
    
    // 세션 조회
    public Map<String, Object> getSession(String sessionId) {
        String key = "session:" + sessionId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        if (entries.isEmpty()) {
            return null;
        }
        
        // 세션 활성 시간 갱신
        redisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        
        // Map<Object, Object>를 Map<String, Object>로 변환
        Map<String, Object> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v));
        
        return result;
    }
    
    // 세션 업데이트
    public void updateSession(String sessionId, String key, Object value) {
        String redisKey = "session:" + sessionId;
        redisTemplate.opsForHash().put(redisKey, key, value);
        redisTemplate.expire(redisKey, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    // 세션 삭제
    public void deleteSession(String sessionId) {
        String key = "session:" + sessionId;
        redisTemplate.delete(key);
    }
}
```

### 4. 속도 제한 (Rate Limiting)

```java
@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    // API 요청 속도 제한 검사
    public boolean checkRateLimit(String clientIp, String endpoint, int maxRequests, int windowSeconds) {
        String key = "ratelimit:" + endpoint + ":" + clientIp;
        
        // 현재 요청 수 조회
        Long requestCount = redisTemplate.opsForValue().increment(key, 1);
        
        // 첫 요청인 경우 만료 시간 설정
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        
        // 최대 요청 수 초과 여부 반환
        return requestCount != null && requestCount <= maxRequests;
    }
}
```