# 웹소켓 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 웹소켓 관련 설정을 포함합니다. 웹소켓은 실시간 양방향 통신을 위해 사용됩니다.

## WebSocketConfig

웹소켓 설정을 담당하는 클래스입니다.

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public WebSocketConfig(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");  //  그룹 및 브로드캐스트
        config.setApplicationDestinationPrefixes("/app"); // 클라이언트에서 서버로 메시지 보낼 때
        config.setUserDestinationPrefix("/user"); //  개별 사용자 알림 추가 ✅
    }

    @Override
    public void registerStompEndpoints(StompEndpoints registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://www.pinjun.xyz")
                .addInterceptors(new WebSocketAuthInterceptor(userRepository, jwtTokenProvider, redisTemplate))
                .withSockJS();
    }
}
```

### 주요 기능

- **메시지 브로커 설정**:
    - `/topic`: 모든 구독자에게 메시지를 브로드캐스트하기 위한 토픽
    - `/queue`: 특정 사용자에게 메시지를 전송하기 위한 큐
    - `/app`: 클라이언트에서 서버로 메시지를 전송할 때 사용하는 접두어
    - `/user`: 특정 사용자에게 메시지를 전송할 때 사용하는 접두어

- **엔드포인트 등록**:
    - `/ws`: 웹소켓 연결을 위한 엔드포인트
    - SockJS 지원으로 웹소켓을 지원하지 않는 브라우저에서도 동작
    - 인증 인터셉터 등록을 통한 웹소켓 연결 인증 처리

## WebSocketAuthInterceptor

웹소켓 연결 시 인증을 처리하는 인터셉터입니다.

```java
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public WebSocketAuthInterceptor(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().toString() : "알 수 없음";
        String uri = request.getURI().toString();

        log.info("웹소켓 핸드셰이크 시작: 요청 경로={}, 요청 IP={}", uri, ip);

        try {
            // 쿠키에서 Access Token 추출
            String accessToken = extractAccessToken(request);
            if (accessToken == null) {
                log.warn("웹소켓 인증 실패: 액세스 토큰 없음 (IP={})", ip);
                throw new TokenNotFoundException();
            }

            log.debug("액세스 토큰 추출 성공");

            // 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(accessToken)) {
                log.warn("웹소켓 인증 실패: 유효하지 않은 토큰 (IP={})", ip);
                throw new InvalidTokenException();
            }

            log.debug("토큰 유효성 검증 성공");

            // 토큰에서 이메일 추출
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            log.debug("토큰에서 이메일 추출 성공: {}", email);

            // 이메일이 존재하는지 확인
            userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("웹소켓 인증 실패: 존재하지 않는 사용자 - 이메일={}, IP={}", email, ip);
                        return new WebSocketAuthenticationException("사용자를 찾을 수 없습니다.");
                    });

            // WebSocket 세션에 이메일 저장
            attributes.put("email", email);

            log.info("웹소켓 핸드셰이크 성공: 사용자 Email={}, IP={}", email, ip);
            return true;
        } catch (Exception e) {
            log.error("웹소켓 핸드셰이크 실패: 요청 경로={}, 요청 IP={}, 원인={}", uri, ip, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        String uri = request.getURI().toString();
        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().toString() : "알 수 없음";

        if (exception != null) {
            log.error("웹소켓 연결 실패: 요청 경로={}, 요청 IP={}, 원인={}", uri, ip, exception.getMessage(), exception);
            return;
        }

        // 명시적으로 attributes를 Map으로 변환
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            Object attributeObject = servletRequest.getServletRequest().getAttribute("attributes");

            if (attributeObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>) attributeObject;

                if (attributes.containsKey("email")) {
                    String email = (String) attributes.get("email");

                    String redisKey = "WebSocket:User:" + email;

                    Long remainingTime = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS); // TTL 확인
                    if (remainingTime == null || remainingTime <= 600) { // 10분 이하이거나 없을 때 갱신
                        redisTemplate.opsForValue().set(redisKey, "CONNECTED");
                        redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
                        log.info("Redis TTL 갱신: 사용자 Email={}, 남은 TTL={}초", email, remainingTime);
                    }

                    log.info("웹소켓 연결 성공: 사용자 Email={}, IP={}", email, ip);
                } else {
                    log.warn("웹소켓 연결 성공했으나 'email'이 attributes에 없음 (IP={})", ip);
                }
            } else {
                log.error("웹소켓 연결 성공했으나 attributes가 Map 타입이 아님 (IP={})", ip);
            }
        } else {
            log.error("웹소켓 요청이 ServletServerHttpRequest 타입이 아님 (IP={})", ip);
        }
    }

// 코드 내용 중략...

    /**
     * 요청에서 액세스 토큰 추출
     *
     * @param request 서버 HTTP 요청
     * @return 추출된 액세스 토큰 또는 null
     */
    private String extractAccessToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            Cookie[] cookies = servletRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("ACCESS_TOKEN".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        return null;
    }
}
```