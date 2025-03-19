# JWT와 Redis를 연계한 인증 시스템 구현

## 1. 배경 및 문제점

### JWT 토큰 관리의 한계
- **민감 정보 노출 위험**: JWT는 단순 인코딩 방식으로, 쉽게 디코딩되어 페이로드 내 민감 정보가 노출될 수 있음
- **토큰 무효화의 어려움**: JWT는 발급 후 유효 기간 내 즉시 무효화가 어려움(로그아웃 처리 등)
- **사용자 정보 접근 시 DB 부하**: 인증 필요한 모든 요청마다 사용자 정보를 DB에서 조회 시 성능 저하
- **토큰 만료 관리 복잡성**: 다양한 만료 조건(로그아웃, 계정 정지 등) 관리가 어려움

## 2. 해결 방안: JWT와 Redis 연계 시스템

### 핵심 아키텍처
1. **JWT 페이로드 최소화**
    - 토큰에는 식별자(email)만 포함하여 노출 위험 최소화
   ```java
   String accessToken = JWT.create()
       .withSubject(email)
       .withIssuedAt(new Date())
       .withExpiresAt(accessExpiry)
       .sign(Algorithm.HMAC512(secretKey));
   ```

2. **Redis 기반 토큰 관리**
    - Redis에 토큰-사용자정보 매핑 저장
   ```java
   public void addAccessToken(String accessToken, String email, Integer age, Gender gender, long expirationMillis, String ip) {
       String key = "access:" + accessToken;
       Map<String, String> fields = new HashMap<>();
       fields.put("email", email);
       fields.put("age", String.valueOf(age));
       fields.put("gender", gender.toString());
       if (ip != null) {
           fields.put("ip", ip);
       }
       redisTemplate.opsForHash().putAll(key, fields);
       redisTemplate.expire(key, Duration.ofMillis(expirationMillis));
   }
   ```

3. **화이트리스트 방식 토큰 검증**
    - Redis에 등록된 토큰만 유효하게 처리
   ```java
   public boolean isAccessTokenValid(String accessToken) {
       return redisTemplate.hasKey("access:" + accessToken);
   }
   ```

4. **인증 필터 구현**
    - JWT 검증 + Redis 검증으로 이중 보안
   ```java
   if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
       if (authRedisService.isAccessTokenValid(accessToken)) {
           String email = jwtTokenProvider.getEmailFromToken(accessToken);
           Integer age = authRedisService.getAgeByAccessToken(accessToken);
           Gender gender = authRedisService.getGenderByAccessToken(accessToken);
           
           // Security Context에 인증 정보 설정
           UsernamePasswordAuthenticationToken authentication = 
               new UsernamePasswordAuthenticationToken(email, null, null);
           authentication.setDetails(new UserInfo(age, gender));
           SecurityContextHolder.getContext().setAuthentication(authentication);
       }
   }
   ```

## 3. 주요 컴포넌트

### JwtTokenProvider
- JWT 토큰 생성, 검증, 사용자 정보 추출 담당
- Redis 서비스와 연계하여 토큰 관리

### AuthRedisService
- Redis에 토큰 정보 저장 및 관리
- 토큰 유효성 검증 및 사용자 정보 조회
- 로그아웃 처리(토큰 삭제)

### JwtAuthenticationFilter
- 요청마다 JWT 토큰 검증 및 인증 처리
- SecurityContext에 사용자 정보 설정

### SecurityConfig
- Spring Security 설정 및 JWT 필터 등록
- 보안 규칙 및 인증 예외 경로 설정

## 4. 인증 프로세스 흐름

### 로그인 과정
1. 사용자 로그인 요청 처리
   ```java
   public TokenDto login(LoginRequestDto loginRequestDto, String ip) {
       // 사용자 검증
       User user = userRepository.findByEmail(loginRequestDto.getEmail())
           .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
       
       // 비밀번호 검증
       if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
           throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
       }
       
       // 토큰 발급
       return jwtTokenProvider.generateTokenPair(user.getEmail(), user.getAge(), user.getGender(), ip);
   }
   ```

2. Access/Refresh 토큰 쌍 생성 및 Redis 저장
3. 토큰을 쿠키로 클라이언트에 전달
   ```java
   setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), accessTokenExpireSeconds);
   setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), refreshTokenExpireSeconds);
   ```

### 인증 검증 과정
1. 요청마다 쿠키에서 JWT 토큰 추출
2. JWT 서명 및 만료 검증
3. Redis에서 토큰 유효성 확인
4. 인증 정보 SecurityContext에 설정

### 로그아웃 과정
1. 토큰을 Redis에서 삭제하여 즉시 무효화
   ```java
   public void removeAccessToken(String accessToken) {
       redisTemplate.delete("access:" + accessToken);
   }
   
   public void removeRefreshToken(String refreshToken) {
       redisTemplate.delete("refresh:" + refreshToken);
   }
   ```
2. 클라이언트 쿠키에서 토큰 제거
   ```java
   expireCookie(response, "ACCESS_TOKEN");
   expireCookie(response, "REFRESH_TOKEN");
   ```

## 5. 구현 이점

### 보안 강화
- JWT에 민감 정보 제외로 토큰 탈취 시에도 개인정보 유출 최소화
- 로그아웃 즉시 토큰 무효화 가능

### 성능 최적화
- Redis 메모리 캐싱으로 인증 과정에서 DB 조회 최소화
- 사용자 인증 정보 빠른 접근으로 응답 시간 개선

### 확장성 향상
- 분산 환경에서 Redis를 공유 인증 저장소로 활용
- 다중 서버 구성에서도 일관된 인증 상태 유지

### 유연한 인증 관리
- 로그아웃, 계정 정지, 강제 토큰 만료 등 다양한 시나리오 지원
- Redis TTL 기능으로 자동 만료 처리 용이

## 6. 디자인 패턴 및 코드 구조

### 계층 분리
- 인증 관련 로직을 컴포넌트별로 명확히 분리
- 토큰 생성/검증, Redis 저장/조회, 인증 필터 처리 등

### SecurityContext 활용
- Spring Security의 SecurityContext를 활용한 인증 정보 관리
- 사용자 정보(나이, 성별 등)를 UserInfo 객체로 캡슐화하여 details에 저장

### Cookie 기반 토큰 전송
- HttpOnly, Secure, SameSite 옵션으로 보안 강화
- XSS 공격으로부터 토큰 보호

## 7. 결론

JWT와 Redis를 연계한 인증 시스템은 JWT의 장점(무상태성, 확장성)을 유지하면서 단점(즉시 무효화 어려움, 정보 노출 위험)을 보완합니다. Redis의 메모리 기반 고속 처리 능력으로 인증 성능을 최적화하고, 분산 환경에서도 일관된 인증 상태를 유지할 수 있습니다. 세션 방식의 장점과 JWT의 장점을 결합한 하이브리드 접근 방식으로, 보안성과 성능을 모두 고려한 인증 아키텍처입니다.