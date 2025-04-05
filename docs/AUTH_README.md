# Spring Boot OAuth2.0 구현 가이드

## 1. 개요

이 문서는 Spring Boot 기반의 웹 애플리케이션에서 OAuth2.0을 사용한 소셜 로그인(Google, Naver, Kakao) 구현에 대한 기술 문서입니다. JWT(JSON Web Token)를 함께 사용하여 인증 시스템을 구축하는 방법을 설명합니다.

### 주요 기능
- 소셜 로그인 (Google, Naver, Kakao)
- JWT 기반 인증
- Redis를 활용한 토큰 관리
- 회원가입/로그인 통합 프로세스
- 추가 정보 입력을 위한 플로우

## 2. 기술 스택

- **Spring Boot 3.4.1** - 웹 애플리케이션 프레임워크
- **Spring Security** - 인증 및 권한 부여 시스템
- **Spring OAuth2 Client** - OAuth2.0 클라이언트 기능
- **Java JWT(Auth0)** - JWT 토큰 생성 및 검증
- **Spring Data Redis** - 토큰 저장 및 관리
- **Spring Data JPA** - 데이터 액세스
- **MySQL** - 데이터베이스
- **Lombok** - 코드 간소화

## 3. 의존성 설정

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'com.auth0:java-jwt:3.18.2'
    // ... 기타 의존성
}
```

## 4. 설정 파일

### 4.1 application.yml

이 설정 파일에서 데이터베이스, 보안, OAuth2 클라이언트 등 애플리케이션의 주요 구성을 정의합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/freamdb?serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  # OAuth2 설정
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: https://www.pinjun.xyz/api/login/oauth2/code/google
            scope:
              - email
              - profile
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: https://www.pinjun.xyz/api/login/oauth2/code/naver
            authorization-grant-type: authorization_code
            scope:
              - name
              - email
              - profile_image
            client-name: Naver
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: https://www.pinjun.xyz/api/login/oauth2/code/kakao
            authorization-grant-type: authorization_code
            scope:
              - profile_nickname
              - profile_image
              - account_email
            client-name: Kakao
            client-authentication-method: client_secret_post
        provider:
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

jwt:
  secret: ${JWT_SECRET}
  expiration: 1800000      # 액세스 토큰 만료 (30분)
  refreshExpiration: 86400000  # 리프레시 토큰 만료 (24시간)
```

## 5. 엔티티 설계

### 5.1 User 엔티티

사용자 정보를 저장하는 엔티티로, OAuth 연결 정보를 포함합니다.

```java
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 사용자 ID

    @Column(nullable = false, unique = true)
    private String email; // 이메일 주소

    @Column(nullable = false)
    private String password; // 비밀번호

    @Column(nullable = false, unique = true)
    private String referralCode; // 고유 추천인 코드

    @Column(nullable = false)
    private String phoneNumber; // 전화번호 추가

    @Enumerated(EnumType.STRING)
    private ShoeSize shoeSize; // 신발 사이즈 (Enum)

    private boolean termsAgreement; // 이용약관 동의 여부

    private boolean phoneNotificationConsent; // 전화 알림 수신 동의 여부
    private boolean emailNotificationConsent; // 이메일 수신 동의 여부
    private boolean optionalPrivacyAgreement; // 선택적 개인정보 동의 여부 추가

    @Builder.Default
    private boolean isVerified = false; // 본인인증 완료 여부

    @Column
    private String ci; // 연계정보 (Connecting Information)

    @Column
    private String di; // 중복가입확인정보 (Duplication Information)

    @Enumerated(EnumType.STRING)
    private Role role; // USER, ADMIN 등으로 역할 구분

    private Integer sellerGrade; // 판매자 등급 (1~5)

    private Integer age; // 나이

    @Enumerated(EnumType.STRING)
    private Gender gender; // 성별 (Enum)

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile; // 프로필 (1:1 관계)

    // 관련 엔티티와의 연관관계 생략...

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OAuthConnection> oauthConnections = new ArrayList<>();
    
    // OAuth 연결 추가 메서드
    public void addOAuthConnection(String provider, String providerId) {
        // 이미 해당 프로바이더에 대한 연결이 있는지 확인
        boolean connectionExists = this.oauthConnections.stream()
                .anyMatch(conn -> conn.getProvider().equals(provider));

        if (!connectionExists) {
            OAuthConnection connection = OAuthConnection.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .connectedAt(LocalDateTime.now())
                    .build();

            connection.setUser(this);
            this.oauthConnections.add(connection);
        }
    }

    // OAuth 연결 확인 메서드
    public boolean hasOAuthConnection(String provider) {
        return this.oauthConnections.stream()
                .anyMatch(conn -> conn.getProvider().equals(provider));
    }

    // 본인인증 상태 업데이트
    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    // 기타 사용자 정보 업데이트 메서드 생략...
}
```

### 5.2 OAuthConnection 엔티티

소셜 로그인 제공자와의 연결 정보를 저장하는 엔티티입니다.

```java
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String provider;     // "google", "naver" 등

    @Column(nullable = false)
    private String providerId;   // 제공자별 고유 ID

    private String accessToken;  // (선택) 액세스 토큰 저장

    private LocalDateTime connectedAt;  // 연결 시간

    // 연관관계 편의 메서드
    public void setUser(User user) {
        this.user = user;
    }
}
```

### 5.3 Gender 열거형

```java
public enum Gender {
    MALE, FEMALE, OTHER
}
```

### 5.4 Role 열거형

```java
public enum Role {
    USER, ADMIN, SELLER
}
```

## 6. Security 설정

### 6.1 SecurityConfig 클래스

Spring Security의 기본 설정을 오버라이드하여 JWT 기반의 인증 시스템을 구성합니다.

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //JWT 필터 생성
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, authRedisService);

        http
                // 1) 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(whiteListPaths()).permitAll()
                        .anyRequest().authenticated())

                // 2) CSRF 비활성화 (REST API의 경우)
                .csrf(csrf -> csrf.disable())

                // 3) 폼 로그인 비활성화
                .formLogin(form -> form.disable())

                // 4) HTTP 기본 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())

                // 5) 세션을 사용하지 않도록 설정
                .sessionManagement(session -> session.disable())

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                //커스텀 JWT필터 추가
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증 없이 접근 가능한 경로들
     */
    private String[] whiteListPaths() {
        return new String[] {
                "/**", "/ws/**",
                "/api/**",
                "/oauth2/**",          // OAuth 관련 경로 허용
                "/login/oauth2/**",    // OAuth 로그인 관련 경로 허용
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 6.2 암호화 설정

```java
@Configuration
public class EncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## 7. JWT 인증 시스템

### 7.1 JwtTokenProvider

JWT 토큰 생성, 검증, 파싱을 담당하는 클래스입니다.

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;  // JWT 서명 비밀키

    @Value("${jwt.expiration}")
    private long accessTokenValidityMs; // AccessToken 만료(밀리초)

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenValidityMs; // RefreshToken 만료(밀리초)

    private final AuthRedisService authRedisService;

    /**
     * AccessToken + RefreshToken 동시 발급
     */
    public TokenDto generateTokenPair(String email, Integer age, Gender gender, String ip, Role role) {
        log.info("토큰 생성 시작: 이메일={}, 나이={}, 성별={}, IP={}, 권한={}", email, age, gender, ip, role);

        try {
            long now = System.currentTimeMillis();

            // Access Token 생성 (role 정보 추가)
            Date accessExpiry = new Date(now + accessTokenValidityMs);
            String accessToken = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .withExpiresAt(accessExpiry)
                    .withClaim("role", role.name()) // 권한 정보 추가
                    .sign(Algorithm.HMAC512(secretKey));

            // Refresh Token 생성
            Date refreshExpiry = new Date(now + refreshTokenValidityMs);
            String refreshToken = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .withExpiresAt(refreshExpiry)
                    .sign(Algorithm.HMAC512(secretKey));

            // Redis에 저장 (role 정보도 함께 저장)
            authRedisService.addAccessToken(accessToken, email, age, gender, accessTokenValidityMs, ip, role);
            authRedisService.addRefreshToken(refreshToken, email, refreshTokenValidityMs);

            log.info("토큰 생성 완료: 이메일={}, 액세스 토큰 만료={}, 리프레시 토큰 만료={}",
                    email, accessExpiry, refreshExpiry);

            return new TokenDto(accessToken, refreshToken);
        } catch (JWTCreationException e) {
            log.error("토큰 생성 실패: 이메일={}, 오류={}", email, e.getMessage(), e);
            throw new TokenCreationException(e);
        }
    }

    // role 정보 추출 메서드
    public Role getRoleFromToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            String roleName = decoded.getClaim("role").asString();
            log.debug("토큰에서 권한 추출: {}", roleName);
            return Role.valueOf(roleName);
        } catch (Exception e) {
            log.error("토큰에서 권한 추출 중 오류: {}", e.getMessage(), e);
            throw new InvalidTokenException("토큰에서 권한을 추출하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * JWT 서명 + 만료시간 검증
     */
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            log.debug("토큰 검증 성공");
            return true;
        } catch (TokenExpiredException e) {
            log.warn("만료된 토큰: {}", e.getMessage());
            throw new ExpiredTokenException();
        } catch (JWTVerificationException e) {
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            throw new InvalidTokenException();
        } catch (Exception e) {
            log.error("토큰 검증 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InvalidTokenException("토큰 검증 중 오류가 발생했습니다.");
        }
    }

    /**
     * 토큰에서 subject(email) 추출
     */
    public String getEmailFromToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            String email = decoded.getSubject();
            log.debug("토큰에서 이메일 추출: {}", email);
            return email;
        } catch (TokenExpiredException e) {
            log.warn("만료된 토큰에서 이메일 추출 시도: {}", e.getMessage());
            throw new ExpiredTokenException();
        } catch (JWTVerificationException e) {
            log.warn("유효하지 않은 토큰에서 이메일 추출 시도: {}", e.getMessage());
            throw new InvalidTokenException();
        } catch (Exception e) {
            log.error("토큰에서 이메일 추출 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InvalidTokenException("토큰에서 이메일을 추출하는 중 오류가 발생했습니다.");
        }
    }
}
```

### 7.2 JwtAuthenticationFilter

모든 요청에 대해 JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정하는 필터입니다.

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException
    {
        // 1) 쿠키에서 AccessToken 찾기
        String accessToken = getCookieValue(request, "ACCESS_TOKEN");

        // 2) 유효성 검증
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            // 3) Redis에 존재하는지(화이트리스트)
            if (authRedisService.isAccessTokenValid(accessToken)) {
                // 4) 토큰에서 이메일, 나이, 성별 등
                String email = jwtTokenProvider.getEmailFromToken(accessToken);
                Integer age = authRedisService.getAgeByAccessToken(accessToken);
                Gender gender = authRedisService.getGenderByAccessToken(accessToken);

                // 예) 권한을 넣고 싶다면 GrantedAuthority 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, null);

                // 나이/성별 등 추가 정보를 details에 저장
                UserInfo userInfo = new UserInfo(age, gender);
                authentication.setDetails(userInfo);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
        } else {
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 특정 이름의 값 추출
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (c.getName().equals(cookieName)) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 임의의 사용자 정보 객체 (SecurityContextHolder에 details로 담을 수 있음)
     */
    public static class UserInfo {
        private final Integer age;
        private final Gender gender;

        public UserInfo(Integer age, Gender gender) {
            this.age = age;
            this.gender = gender;
        }

        public Integer getAge() { return age; }
        public Gender getGender() { return gender; }
    }
}
```

## 8. OAuth2 구현

### 8.1 CustomOAuth2UserService

OAuth2 공급자로부터 인증된 사용자 정보를 가져와 처리하는 서비스입니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final ProfileCommandService profileCommandService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        log.info("OAuth2 Provider: {}", registrationId);
        log.info("Full OAuth2 Attributes: {}", oAuth2User.getAttributes());

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        try {
            OAuthAttributes attributes = OAuthAttributes.of(
                    registrationId, userNameAttributeName, oAuth2User.getAttributes());

            log.info("OAuth2 로그인 시도: 제공자={}, 이메일={}", registrationId, attributes.getEmail());

            User user = saveOrUpdate(attributes, registrationId);

            // 사용자의 데이터베이스 ID를 사용
            Map<String, Object> modifiedAttributes = new HashMap<>(attributes.getAttributes());
            modifiedAttributes.put("id", user.getId().toString());

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                    modifiedAttributes,
                    registrationId.equals("naver") || registrationId.equals("kakao") ? "id" : attributes.getNameAttributeKey());
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("OAuth2 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 유저 정보 저장 또는 업데이트
     */
    @Transactional
    private User saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        Optional<User> existingUser = userRepository.findByEmail(attributes.getEmail());

        // 프로바이더 ID 추출
        String providerId = extractProviderId(registrationId, attributes);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // 현재 OAuth 제공자와 아직 연결되어 있지 않다면 새로 연결
            if (!user.hasOAuthConnection(registrationId)) {
                user.addOAuthConnection(registrationId, providerId);
            }

            log.info("기존 사용자 OAuth2 정보 업데이트: {}, 제공자: {}", attributes.getEmail(), registrationId);
            return user;
        } else {
            log.info("새 OAuth2 사용자 등록: {}, 제공자: {}", attributes.getEmail(), registrationId);

            // 비밀번호 암호화
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

            // 새 사용자 생성 (기본 정보만 설정)
            User user = User.builder()
                    .email(attributes.getEmail())
                    .password(randomPassword)
                    .referralCode(generateUniqueReferralCode())
                    .phoneNumber("") // OAuth 로그인에서는 전화번호를 제공하지 않음
                    .termsAgreement(true) // 기본 이용약관 동의
                    .role(Role.USER)
                    .isVerified(false) // 아직 추가 정보 입력이 필요함
                    .build();

            User savedUser = userRepository.save(user);

            // OAuth 연결 추가
            savedUser.addOAuthConnection(registrationId, providerId);

            // 기본 프로필 생성
            profileCommandService.createDefaultProfile(savedUser);

            return savedUser;
        }
    }

    // providerId 추출 및 유니크 코드 생성 메서드들...
    private String extractProviderId(String registrationId, OAuthAttributes attributes) {
        try {
            if ("google".equals(registrationId)) {
                return attributes.getAttributes().get(attributes.getNameAttributeKey()).toString();
            } else if ("naver".equals(registrationId)) {
                Map<String, Object> response = (Map<String, Object>) attributes.getAttributes().get("response");
                return response.get("id").toString();
            } else if ("kakao".equals(registrationId)) {
                // 카카오는 ID가 최상위 속성으로 제공됨
                return attributes.getAttributes().get("id").toString();
            } else {
                return UUID.randomUUID().toString(); // 기본값 또는 다른 로직
            }
        } catch (Exception e) {
            log.error("제공자 ID 추출 중 오류 발생: {}, 제공자: {}", e.getMessage(), registrationId, e);
            // 에러 발생 시 무작위 UUID 반환
            return UUID.randomUUID().toString();
        }
    }

    private String generateUniqueReferralCode() {
        String referralCode;
        do {
            // 8자리 랜덤 문자열 생성
            referralCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.findByReferralCode(referralCode).isPresent()); // 중복 체크
        return referralCode;
    }
}

    private String generateUniqueReferralCode() {
        String referralCode;
        do {
            // 8자리 랜덤 문자열 생성
            referralCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.findByReferralCode(referralCode).isPresent()); // 중복 체크
        return referralCode;
    }
}
```

### 8.2 OAuth2AuthenticationSuccessHandler

OAuth2 인증 성공 시 처리하는 핸들러입니다. JWT 토큰을 생성하고 쿠키에 설정합니다.

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 인증 성공 처리");

        try {
            // OAuth2 인증 정보 가져오기
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            log.info("OAuth2 로그인 성공 - 제공자: {}, 속성: {}", registrationId, oAuth2User.getAttributes());

            // 이메일 가져오기 - 로그인 제공자별로 다르게 처리
            String email = extractEmail(registrationId, oAuth2User.getAttributes());

            // 이메일 검증
            if (email == null || email.isEmpty()) {
                log.error("OAuth2 인증 성공했지만 이메일을 찾을 수 없음");
                response.sendRedirect("/login?error=email_not_found");
                return;
            }

            // 사용자 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

            // 클라이언트 IP 주소 가져오기
            String ipAddress = getClientIp(request);

            // 새로 가입한 사용자이고 필수 정보가 없는 경우 추가 정보 입력 페이지로 리다이렉트
            boolean needsAdditionalInfo = !user.isVerified() || user.getAge() == null || user.getGender() == null;

            if (needsAdditionalInfo) {
                // 임시 세션 ID나 토큰을 생성하여 추가 정보 입력 페이지로 전달
                String tempToken = generateTempToken(user.getEmail());
                log.info("사용자 추가 정보 입력 필요: {}", email);
                response.sendRedirect("/oauth/complete-signup?token=" + tempToken);
                return;
            }

            // JWT 토큰 생성
            TokenDto tokenDto = jwtTokenProvider.generateTokenPair(
                    user.getEmail(),
                    user.getAge(),
                    user.getGender(),
                    ipAddress,
                    user.getRole() != null ? user.getRole() : Role.USER
            );

            // 쿠키에 토큰 설정
            setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), 30 * 60); // 30분
            setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), 24 * 60 * 60); // 24시간

            log.info("사용자 로그인 성공: {}, 제공자: {}", email, registrationId);

            // 프론트엔드 페이지로 리다이렉트
            response.sendRedirect("http://www.pinjun.xyz");
        } catch (Exception e) {
            log.error("OAuth2 인증 중 오류 발생", e);
            response.sendRedirect("http://www.pinjun.xyz/login?error=oauth_failed");
        }
    }

    // 이메일 추출 메서드
    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            // 네이버 처리
            if (attributes.containsKey("response")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseObj = (Map<String, Object>) attributes.get("response");
                return (String) responseObj.get("email");
            }
        } else if ("kakao".equals(registrationId)) {
            // 카카오 처리
            if (attributes.containsKey("kakao_account")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                return (String) kakaoAccount.get("email");
            }
        } else {
            // 구글 등 다른 제공자
            return (String) attributes.get("email");
        }
        return null;
    }

    // 임시 토큰 생성 메서드
    private String generateTempToken(String email) {
        String plainToken = email + "|" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(plainToken.getBytes());
    }

    // IP 주소 가져오기
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    // 쿠키 설정
    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
```
### 8.3 OAuthAttributes

OAuth2 공급자로부터 받은 사용자 속성 정보를 매핑하는 클래스입니다.

```java
@Slf4j
@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
                           String name, String email, String picture) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return ofGoogle(userNameAttributeName, attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        } else if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        // Google 사용자 속성 매핑
        if (attributes == null) {
            throw new IllegalArgumentException("Google OAuth attributes are null");
        }

        return OAuthAttributes.builder()
                .name(attributes.containsKey("name") ? (String) attributes.get("name") : null)
                .email(attributes.containsKey("email") ? (String) attributes.get("email") : null)
                .picture(attributes.containsKey("picture") ? (String) attributes.get("picture") : null)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // Naver 사용자 속성 매핑
        log.info("Naver OAuth Attributes: {}", attributes);

        Object responseObj = attributes.get("response");
        if (!(responseObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Naver OAuth response structure");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) responseObj;

        return OAuthAttributes.builder()
                .name(response.containsKey("name") ? (String) response.get("name") : null)
                .email(response.containsKey("email") ? (String) response.get("email") : null)
                .picture(response.containsKey("profile_image") ? (String) response.get("profile_image") : null)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        log.info("Kakao OAuth Attributes: {}", attributes);

        // attributes에서 kakao_account 객체 추출 (이메일, 성별 등의 정보가 있음)
        Object kakaoAccountObj = attributes.get("kakao_account");
        if (!(kakaoAccountObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Kakao OAuth response structure - kakao_account missing");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoAccountObj;

        // kakao_account에서 profile 객체 추출 (닉네임, 프로필 이미지 등의 정보가 있음)
        Object profileObj = kakaoAccount.get("profile");
        if (!(profileObj instanceof Map)) {
            throw new IllegalArgumentException("Invalid Kakao OAuth response structure - profile missing");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) profileObj;

        String email = kakaoAccount.containsKey("email") ? (String) kakaoAccount.get("email") : null;
        String nickname = profile.containsKey("nickname") ? (String) profile.get("nickname") : null;
        String profileImage = profile.containsKey("profile_image_url") ? (String) profile.get("profile_image_url") : null;

        return OAuthAttributes.builder()
                .name(nickname)
                .email(email)
                .picture(profileImage)
                .attributes(attributes)  // 전체 원본 attributes 유지
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private String extractProviderId(String registrationId, OAuthAttributes attributes) {
        log.info("Extracting Provider ID - Registration ID: {}", registrationId);
        log.info("Attributes for extraction: {}", attributes.getAttributes());

        try {
            if ("google".equals(registrationId)) {
                return attributes.getAttributes().get(attributes.getNameAttributeKey()).toString();
            } else if ("naver".equals(registrationId)) {
                // 네이버는 response 객체 안에 정보가 있음
                Object responseObj = attributes.getAttributes().get("response");
                if (responseObj instanceof Map) {
                    Map<String, Object> response = (Map<String, Object>) responseObj;
                    return response.get("id").toString();
                } else {
                    log.error("Naver response is not a Map: {}", responseObj);
                    throw new IllegalArgumentException("Invalid Naver OAuth response");
                }
            } else {
                return UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            log.error("Error extracting provider ID", e);
            throw e;
        }
    }
}
```

## 9. 인증 흐름

### 9.1 OAuth2 로그인 흐름

1. 사용자가 소셜 로그인 버튼(Google, Naver, Kakao)을 클릭합니다.
2. 해당 서비스의 OAuth2 인증 페이지로 리다이렉트됩니다.
3. 사용자가 해당 서비스에 로그인하고 권한을 허용합니다.
4. 서비스는 인증 코드를 발급하고 우리 서비스의 콜백 URI로 리다이렉트합니다.
5. Spring Security OAuth2 클라이언트가 인증 코드를 통해 액세스 토큰을 요청합니다.
6. 액세스 토큰을 사용해 사용자 정보를 가져옵니다(`CustomOAuth2UserService.loadUser()`).
7. 사용자 정보를 기반으로 로그인 처리 또는 회원가입을 자동으로 수행합니다.
    - 이메일이 이미 존재한다면 기존 사용자로 로그인 처리
    - 이메일이 존재하지 않는다면 새 사용자로 등록
8. 로그인 성공 후 JWT 토큰을 생성하고 쿠키에 설정합니다(`OAuth2AuthenticationSuccessHandler`).
9. 최초 로그인이거나 추가 정보가 필요한 경우 추가 정보 입력 페이지로 리다이렉트합니다.
10. 그렇지 않으면 메인 페이지로 리다이렉트합니다.

### 9.2 JWT 인증 흐름

1. 클라이언트는 쿠키에 저장된 ACCESS_TOKEN을 모든 요청에 포함합니다.
2. 서버는 `JwtAuthenticationFilter`를 통해 토큰을 검증합니다.
    - 토큰 서명 검증
    - 토큰 만료 시간 검증
    - Redis에서 토큰의 유효성 확인(블랙리스트 체크)
3. 검증이 완료되면 인증 정보를 `SecurityContext`에 설정합니다.
4. 인증된 사용자는 보호된 리소스에 접근할 수 있습니다.
5. 액세스 토큰이 만료되면 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받아야 합니다.

### 9.3 추가 정보 입력 흐름

1. OAuth2 인증 후, 사용자에게 추가 정보가 필요하다면 `/oauth/complete-signup` 페이지로 리다이렉트합니다.
2. 사용자는 추가 정보(나이, 성별 등)를 입력합니다.
3. 입력 완료 후, 서버는 사용자 정보를 업데이트하고 `isVerified` 플래그를 `true`로 설정합니다.
4. JWT 토큰을 새로 발급하고 쿠키에 설정합니다.
5. 메인 페이지로 리다이렉트합니다.

## 10. 보안 고려사항

### 10.1 HTTPS 사용

모든 OAuth2 인증 요청과 JWT 토큰 교환은 HTTPS를 통해 이루어져야 합니다. 특히 쿠키에 저장되는 JWT 토큰은 `secure` 플래그가 설정되어야 합니다.

### 10.2 토큰 관리

- 액세스 토큰의 유효 기간은 짧게 설정합니다(30분 ~ 2시간).
- 리프레시 토큰의 유효 기간은 적절히 설정합니다(하루 ~ 2주).
- Redis를 사용하여 토큰의 화이트리스트 또는 블랙리스트를 관리합니다.
- 로그아웃 시 Redis에서 토큰을 무효화합니다.

### 10.3 CSRF 보호

REST API는 상태를 저장하지 않으므로 CSRF 보호가 기본적으로 비활성화되어 있습니다. 그러나 쿠키 기반 인증을 사용할 때는 CSRF 공격에 취약할 수 있으므로 추가적인 보호 조치가 필요할 수 있습니다.

### 10.4 쿠키 보안

- `HttpOnly` 플래그를 설정하여 JavaScript를 통한 쿠키 접근을 방지합니다.
- `SameSite=None` 및 `Secure` 플래그를 설정하여 크로스 사이트 요청 시 쿠키가 전송되도록 합니다.
- 프론트엔드와 백엔드가 다른 도메인을 사용하는 경우, CORS 설정을 적절히 구성해야 합니다.

## 11. 디버깅 및 문제 해결

### 11.1 OAuth2 디버깅

- Spring Security 로그 레벨을 DEBUG로 설정하여 OAuth2 인증 흐름을 자세히 관찰합니다.
- 각 OAuth2 공급자의 로그인 시도 및 속성을 로깅하여 문제를 식별합니다.

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.fream: INFO
```

### 11.2 일반적인 문제

1. **콜백 URI 미스매치**: OAuth2 공급자의 콜백 URI 설정이 애플리케이션의 설정과 일치하지 않는 경우.
2. **권한 부족**: OAuth2 공급자에게 요청하는 권한(scope)이 충분하지 않아 필요한 정보를 가져오지 못하는 경우.
3. **토큰 만료**: JWT 토큰이 만료되어 인증이 실패하는 경우.
4. **CORS 오류**: 프론트엔드와 백엔드의 도메인이 다른 경우 발생할 수 있는 CORS 관련 오류.

## 12. 확장 및 개선 방안

### 12.1 다양한 소셜 로그인 제공자 추가

현재 구현은 Google, Naver, Kakao를 지원하지만, Facebook, Apple, GitHub 등 더 많은 로그인 공급자를 추가할 수 있습니다.

### 12.2 보안 강화

- 다단계 인증(MFA) 지원 추가
- 로그인 기록 및 이상 징후 감지
- IP 기반 접근 제한

### 12.3 사용자 경험 개선

- 소셜 계정 연결/해제 기능 제공
- 프로필 정보 동기화 옵션 제공
- 로그인 상태 유지 기능 개선

## 13. 결론

이 문서에서는 Spring Boot 애플리케이션에서 OAuth2를 사용한 소셜 로그인 구현 방법과 JWT를 통한 인증 시스템 구축 방법을 설명했습니다. 주요 구성 요소로는 OAuth2 클라이언트 설정, JWT 토큰 생성 및 검증, Redis를 활용한 토큰 관리 등이 있습니다.

User 엔티티와 OAuthConnection 엔티티의 관계를 활용하여 하나의 사용자 계정에 여러 소셜 로그인을 연결할 수 있는 구조를 구현했습니다. 이러한 구현을 통해 사용자는 다양한 소셜 계정으로 쉽게 로그인할 수 있으며, 서버는 JWT 기반의 안전한 인증 시스템을 제공할 수 있습니다.

이 구현은 확장성과 보안성을 고려했으며, 다양한 개선 방안을 통해 더욱 강화될 수 있습니다.
