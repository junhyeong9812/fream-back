**Findings**

고확신 정확성 결함은 발견하지 못했습니다.

**검토 메모**

- SecurityConfig 와이어링: [SecurityConfig.java](/home/jun/project/fream-back/src/main/java/com/fream/back/global/config/security/SecurityConfig.java:35)에서 `AuthService`를 주입하고 [line 42](/home/jun/project/fream-back/src/main/java/com/fream/back/global/config/security/SecurityConfig.java:42)에서 `new LoginAuthenticationFilter(authService)`로 넘기는 구조는 현재 의존 그래프상 순환참조로 보이지 않습니다. `AuthService`는 `UserRepository`, `PasswordEncoder`, `JwtTokenProvider`만 의존하고, `PasswordEncoder`는 별도 [EncoderConfig.java](/home/jun/project/fream-back/src/main/java/com/fream/back/global/config/security/EncoderConfig.java:9)에서 제공됩니다.
- 로그인 의미 변경: 현재 필터는 이미 [LoginAuthenticationFilter.java](/home/jun/project/fream-back/src/main/java/com/fream/back/global/config/security/filter/LoginAuthenticationFilter.java:87)에서 `authService.login(loginRequest, clientIp)`를 호출하므로, SecurityConfig 수정 자체는 의도된 생성자 주입을 복구한 변경입니다.
- 필드명 수정: `Order.getModifiedDate()`는 [BaseTimeEntity.java](/home/jun/project/fream-back/src/main/java/com/fream/back/global/entity/BaseTimeEntity.java:17)의 실제 필드와 맞고, `PaymentRepository`의 `p.createdDate`도 [Payment.java](/home/jun/project/fream-back/src/main/java/com/fream/back/domain/payment/entity/Payment.java:18)가 `BaseTimeEntity`를 상속하므로 타당합니다.
- 람다 캡처: [AddressLoggingAspect.java](/home/jun/project/fream-back/src/main/java/com/fream/back/domain/address/aop/AddressLoggingAspect.java:306)의 `final int index = i`는 Java 람다 캡처 컴파일 오류를 바로잡는 정석적인 수정입니다.
- H2 `MODE=MySQL`: [application-test.yml](/home/jun/project/fream-back/src/main/resources/application-test.yml:4)은 [User.java](/home/jun/project/fream-back/src/main/java/com/fream/back/domain/user/entity/User.java:109)의 `BIT(1) DEFAULT 1` DDL을 H2에서 통과시키기 위한 우회로는 적절합니다. 다만 MySQL과 완전 동일한 검증은 아니므로, 운영 DB 특이 동작까지 보장하려면 별도 MySQL/Testcontainers 테스트가 필요합니다.
- 파일 이동: moved package/import는 현재 참조와 맞습니다. 예: [SecurityConfig.java](/home/jun/project/fream-back/src/main/java/com/fream/back/global/config/security/SecurityConfig.java:11), [AccessLogAopConfiguration.java](/home/jun/project/fream-back/src/main/java/com/fream/back/domain/accessLog/config/AccessLogAopConfiguration.java:3).
- Modulith: [build.gradle](/home/jun/project/fream-back/build.gradle:28)의 Spring Modulith `1.3.12` BOM과 starter 추가는 Boot `3.4.1`과 맞습니다. 공식 Spring Modulith compatibility 표도 1.3 계열이 Spring Boot 3.4 기준으로 컴파일됐다고 명시합니다: https://docs.spring.io/spring-modulith/reference/appendix.html

직접 빌드/테스트 실행은 이 세션의 read-only sandbox 제약 때문에 하지 못했고, 파일 대조와 공식 문서 확인 기준의 리뷰입니다.