# 웹소켓 모듈

이 디렉토리는 Fream 백엔드 애플리케이션의 실시간 통신을 위한 웹소켓 관련 컴포넌트를 포함합니다.

## WebSocketConfig

웹소켓 메시지 브로커와 엔드포인트를 설정하는 클래스입니다.

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config);
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry);
}
```

### 주요 기능

- **메시지 브로커 설정**: 그룹 및 개인 메시지를 위한 브로커를 활성화합니다.
- **엔드포인트 등록**: 클라이언트가 연결할 웹소켓 엔드포인트를 등록합니다.
- **CORS 설정**: 허용된 도메인(Origin)을 설정합니다.
- **인증 인터셉터 등록**: 웹소켓 연결 전 인증을 처리하는 인터셉터를 등록합니다.

### 주요 설정

- **메시지 브로커 주소**:
    - `/topic`: 다중 클라이언트 브로드캐스트
    - `/queue`: 특정 클라이언트 메시지
    - `/user`: 개별 사용자 메시지

- **애플리케이션 목적지 접두사**: `/app`
- **사용자 목적지 접두사**: `/user`
- **STOMP 엔드포인트**: `/ws`

### 사용 예시 (클라이언트 측)

```javascript
// STOMP 클라이언트 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 연결 및 구독
stompClient.connect({}, function(frame) {
  // 그룹 메시지 구독
  stompClient.subscribe('/topic/notifications', function(message) {
    console.log(JSON.parse(message.body));
  });
  
  // 개인 메시지 구독
  stompClient.subscribe('/user/queue/private-messages', function(message) {
    console.log(JSON.parse(message.body));
  });
});

// 메시지 전송
stompClient.send("/app/message", {}, JSON.stringify({content: "Hello!"}));
```

## WebSocketAuthInterceptor

웹소켓 연결 시 인증을 처리하는 인터셉터입니다.

```java
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception;
            
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception);
            
    private String extractAccessToken(ServerHttpRequest request);
}
```

### 주요 기능

- **토큰 추출**: 쿠키에서 JWT 액세스 토큰을 추출합니다.
- **토큰 검증**: 추출한 토큰의 유효성을 검증합니다.
- **사용자 확인**: 토큰에서 추출한 이메일로 사용자 존재 여부를 확인합니다.
- **세션 정보 저장**: 사용자 이메일을 웹소켓 세션 속성에 저장합니다.
- **Redis TTL 관리**: 사용자 웹소켓 연결 정보를 Redis에 저장하고 TTL을 관리합니다.

### 인증 처리 흐름

1. 클라이언트 웹소켓 연결 요청
2. `beforeHandshake` 메서드에서 토큰 추출 및 검증
3. 유효한 토큰이면 사용자 정보를 세션 속성에 저장
4. 연결 성공 후 `afterHandshake` 메서드에서 Redis에 연결 정보 저장
5. Redis TTL 관리를 통해 활성 사용자 추적

### 웹소켓 인증 흐름도

```
클라이언트           웹소켓 서버           인증 인터셉터         사용자 저장소
    |                   |                    |                    |
    |--- 연결 요청 --->|--- beforeHandshake ->|                    |
    |                   |                    |-- 토큰 추출 & 검증--|
    |                   |                    |------ 사용자 조회 ->|
    |                   |                    |<----- 사용자 정보 --|
    |                   |<-- 인증 결과 ---------|                    |
    |<-- 연결 성공/실패--|                    |                    |
    |                   |---- afterHandshake ->|                    |
    |                   |                    |-- Redis 연결 정보 저장 --|
    |                   |                    |<------ 저장 완료 ----|
```

## EmailBasedUserDestinationResolver

사용자 이메일 기반으로 웹소켓 목적지를 해석하는 컴포넌트입니다.

```java
@Component
public class EmailBasedUserDestinationResolver {
    private final SimpUserRegistry simpUserRegistry;
    
    public String resolveUserDestination(String destination);
}
```

### 주요 기능

- SecurityContext에서 사용자 이메일을 추출하여 웹소켓 목적지를 이메일 기반으로 변환합니다.

### 사용 예시

```java
// 메시지 전송 예시
String destination = emailBasedUserDestinationResolver.resolveUserDestination("/queue/notifications");
messagingTemplate.convertAndSend(destination, messageDto);
```