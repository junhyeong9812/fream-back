# 보안 모듈

이 디렉토리는 Fream 백엔드 애플리케이션의 보안 관련 컴포넌트를 포함합니다.

## JwtTokenProvider

JWT 토큰 생성 및 검증을 담당하는 컴포넌트입니다.

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;  

    @Value("${jwt.expiration}")
    private long accessTokenValidityMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenValidityMs;

    private final AuthRedisService authRedisService;
    
    public TokenDto generateTokenPair(String email, Integer age, Gender gender, String ip, Role role);
    public Role getRoleFromToken(String token);
    public boolean validateToken(String token);
    public String getEmailFromToken(String token);
}
```

### 주요 기능

- **토큰 생성**: 액세스 토큰 및 리프레시 토큰을 발급합니다.
- **토큰 검증**: 토큰의 유효성과 만료 여부를 검증합니다.
- **페이로드 추출**: 토큰에서 사용자 이메일, 권한 등을 추출합니다.
- **Redis 연동**: 발급된 토큰을 Redis에 저장하여 관리합니다.

### 사용 예시

```java
// 토큰 생성
TokenDto tokens = jwtTokenProvider.generateTokenPair(
    "user@example.com", 
    30, 
    Gender.MALE, 
    "192.168.1.1", 
    Role.USER
);

// 토큰 검증
boolean isValid = jwtTokenProvider.validateToken(token);

// 토큰에서 이메일 추출
String email = jwtTokenProvider.getEmailFromToken(token);

// 토큰에서 권한 추출
Role role = jwtTokenProvider.getRoleFromToken(token);
```

## JwtAuthenticationFilter

JWT 토큰 기반 인증을 처리하는 필터입니다.

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain)
            throws ServletException, IOException;
            
    private String getCookieValue(HttpServletRequest request, String cookieName);
    
    public static class UserInfo {
        private final Integer age;
        private final Gender gender;
    }
}
```

### 주요 기능

- **토큰 추출**: 쿠키에서 액세스 토큰을 추출합니다.
- **토큰 검증**: 추출한 토큰의 유효성을 검증합니다.
- **인증 객체 생성**: 검증된 토큰 정보로 인증 객체를 생성하고 SecurityContext에 설정합니다.
- **추가 정보 저장**: 사용자의 나이, 성별 등 추가 정보를 인증 객체에 함께 저장합니다.

### 사용 예시

필터는 Spring Security 필터 체인에 자동으로 등록되어 모든 요청에 적용됩니다:

```java
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

## SecurityConfig

Spring Security 설정 클래스입니다.

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception;
    
    private String[] whiteListPaths();
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception;
}
```

### 주요 기능

- **보안 필터 설정**: JWT 인증 필터를 등록합니다.
- **접근 제어**: URL별 접근 권한을 설정합니다.
- **CSRF, 세션 관리**: CSRF 보호 및 세션 관리 정책을 설정합니다.
- **OAuth2 설정**: 소셜 로그인(Google, Naver, Kakao) 설정을 관리합니다.

### 화이트리스트 URL

아래 경로들은 인증 없이 접근 가능합니다:
- `/oauth2/**`: OAuth 관련 경로
- `/login/oauth2/**`: OAuth 로그인 관련 경로
- `/api/**`: API 엔드포인트 (필요시 경로 제한 가능)

## EncoderConfig

비밀번호 인코더를 설정하는 클래스입니다.

```java
@Configuration
public class EncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 주요 기능

- BCrypt 알고리즘을 사용한 안전한 비밀번호 암호화를 제공합니다.

### 사용 예시

```java
// 사용자 비밀번호 암호화
String encodedPassword = passwordEncoder.encode(rawPassword);

// 비밀번호 검증
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

## TokenDto

액세스 토큰과 리프레시 토큰을 묶어서 반환하기 위한 DTO입니다.

```java
@Getter
@AllArgsConstructor
public class TokenDto {
    private String accessToken;
    private String refreshToken;
}
```

### 주요 기능

- JWT 액세스 토큰과 리프레시 토큰을 함께 전달하는 객체입니다.