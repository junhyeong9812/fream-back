# changelog: 모듈리스 Phase 0 — 그린 빌드 + Spring Modulith 도입 + 안전망

> 대상 diff: 이번 세션 `git diff HEAD -- build.gradle src/main src/test src/main/resources/application-test.yml` + 신규 테스트 3종.
> 제외(부수 변경, 커밋 비대상): `logs/*.log`(테스트 실행 오염), `gradlew`(chmod +x 모드변경), `logs/*.2026-02-21.log`(롤링 산출).

**검증 상태**: 통과 — `./gradlew compileJava` SUCCESSFUL · `ModularityTests`·`PersistenceSmokeTest`·`OrderPaymentRelationshipTest` 모두 PASSED · codex 교차검증 고확신 결함 0건. **미수행**: full `@SpringBootTest` 기동(인프라 부재), 운영 MySQL 동등성.

## 커버리지 셀프체크
- J: build.gradle · SecurityConfig · OrderQueryController · PaymentRepository · AddressLoggingAspect · application-test.yml · ModularityTests(신규) · PersistenceSmokeTest(신규) · OrderPaymentRelationshipTest(신규)
- M: accessLog 아스펙트 3개 이동 · IpBlocking 2개 이동
- 프로세스 산출물(task.md·이 문서·learned·review-log·measurement-log)은 커버리지 제외. □ 전 변경 파일 분류 완료

---

## 1. 판단 항목 (J)

### J-1: Spring Modulith 도입 — 모듈 경계 검증 하네스 — `build.gradle`, `src/test/.../ModularityTests.java`

- **왜**: 모듈리스 리팩토링의 "경계부터 전체 선언" 전략을 실행하려면, grep 추정이 아니라 공식 도구로 순환/위반을 추출하는 ground truth가 필요. 단일 배포 유지(모듈리스)에 가장 부합.
- **대안 비교**:
  | 접근 | 장점 | 단점 | 선택/기각 |
  |------|------|------|----------|
  | Spring Modulith (선택) | Boot 통합·verify()·Documenter·이벤트 레지스트리 | 새 의존성 | **선택** — 모듈리스 목표 정합 |
  | ArchUnit 직접 작성 | 세밀 제어 | 규칙 수기 작성·모듈 개념 부재 | 기각 — 재발명 |
  | Gradle 멀티모듈 | 강한 물리 경계 | 순환 12쌍에 초기 비용 과다 | 기각(현 단계) |
- **근거 출처**: task.md 정의(사용자 결정 "Spring Modulith / 경계부터 전체 선언") + WebSearch(1.3↔Boot3.4) + codex 확인.
- **코드** (build.gradle):
  ```
  dependencyManagement {
  	imports {
  		mavenBom "org.springframework.modulith:spring-modulith-bom:1.3.12"
  	}
  }

  dependencies {
  	implementation 'org.springframework.modulith:spring-modulith-starter-core'
  	testImplementation 'org.springframework.modulith:spring-modulith-starter-test'
  ```
  | 줄 | 근거 해설 |
  |----|----------|
  | BOM 1.3.12 | Boot 3.4 대응은 1.3.x 라인(1.4는 3.5). 버전 일원화 위해 BOM import |
  | starter-core(impl)/test | core는 ApplicationModules API, test는 verify/Documenter |
- **리뷰 연습 포인트**:
  - (외부 경계 렌즈) 새 의존성이 런타임 빈을 자동 등록해 기동에 영향을 주는가? (core는 거의 무해, jpa 스타터 추가 시 이벤트 레지스트리 테이블 필요)

### J-2: SecurityConfig 인증 필터 와이어링 복구 — `global/config/security/SecurityConfig.java:35,42`

- **왜**: `LoginAuthenticationFilter`가 `AuthService` 1개를 받도록 리팩토링됐으나 SecurityConfig만 옛 4-인자 호출을 유지 → 컴파일 차단. 필터는 이미 `authService.login()`을 호출하도록 작성됨(의도 복원).
- **대안 비교**:
  | 접근 | 장점 | 단점 | 선택/기각 |
  |------|------|------|----------|
  | SecurityConfig에 AuthService 주입(선택) | 필터 설계 의도 복원·최소 diff | 보안 영역 변경 | **선택** |
  | 필터를 옛 4-인자로 되돌림 | SecurityConfig 무변경 | 필터 본문(authService.login)도 되돌려야 함·역행 | 기각 |
- **근거 출처**: 기존 코드 패턴(LoginAuthenticationFilter 생성자·본문) + codex 교차검증(순환참조 없음 확인).
- **코드**:
  ```
      private final AuthService authService;
  ...
          LoginAuthenticationFilter loginFilter = new LoginAuthenticationFilter(authService);
  ```
  | 줄 | 근거 해설 |
  |----|----------|
  | L35 | @RequiredArgsConstructor가 AuthService를 생성자 주입에 포함 |
  | L42 | 단일 인자 생성자로 교정. AuthService는 @Service, 의존(UserRepository·PasswordEncoder·AuthRedisService·JwtTokenProvider)은 전부 SecurityConfig 밖 정의 → 순환 없음 |
- **리뷰 연습 포인트**:
  - (권한 경계 렌즈) AuthService를 SecurityConfig가 주입받을 때 빈 초기화 순서/순환참조 위험은? (codex: 없음)
  - (실패모드 렌즈) `passwordEncoder` 필드는 이제 LoginFilter에서 미사용 — 죽은 주입으로 남김(무해), 후속 정리 대상

### J-3: 기동/컴파일 차단 심볼 수정 3건 — `OrderQueryController:62`, `PaymentRepository:71`, `AddressLoggingAspect:306`

- **왜**: 모두 "중단된 리팩토링" 잔재 — 엔티티/속성명이 바뀌었으나 참조가 동기화 안 됨. 런타임 의미는 *원래 의도*를 복원.
- **근거 출처**: 컴파일러 에러 + JPA 슬라이스 부팅 검증(PersistenceSmokeTest) + codex 확인.
- **변경 표**:
  | 파일:라인 | 전 → 후 | 근거 |
  |----------|---------|------|
  | OrderQueryController:62 | `order.getUpdatedAt()` → `order.getModifiedDate()` | `BaseTimeEntity`는 createdDate/modifiedDate 보유, getUpdatedAt 부재 |
  | OrderQueryController:3,7 | 죽은 import 2개(`OrderStatusDto`,`OrderQueryService`) 제거 | 미구현 클래스(필드는 이미 주석처리) |
  | PaymentRepository:71 | JPQL `p.createdAt` → `p.createdDate` | Payment가 BaseTimeEntity 상속, createdAt 속성 부재. @Query는 미사용이어도 Spring Data 기동 시 검증 → 부팅 차단 버그 |
  | AddressLoggingAspect:306 | 루프 내 `final int index = i` 도입, 람다 `idx==i`→`idx==index` | 람다는 effectively final만 캡처 가능. 동작은 의도와 동일 |
- **리뷰 연습 포인트**:
  - (데이터 의미 렌즈) `findPaymentsCreatedSince`가 `createdDate` 기준으로 바뀌며 모니터링 의미가 맞는가? (createdDate = 최초 생성, 의도 부합)

### J-4: 테스트 인프라 — H2 MySQL 호환 모드 + JPA 슬라이스 스모크 — `application-test.yml:4`, `src/test/.../PersistenceSmokeTest.java`

- **왜**: `User.isActive`가 MySQL 전용 `BIT(1) DEFAULT 1` columnDefinition을 써서 H2 스키마 생성 실패. 엔티티를 안 건드리고 H2를 MySQL 호환 모드로 구동해 전 엔티티 모델·레포(QueryDSL 포함) 부팅을 검증.
- **대안 비교**:
  | 접근 | 장점 | 단점 | 선택/기각 |
  |------|------|------|----------|
  | H2 MODE=MySQL (선택) | 무인프라·빠름 | MySQL 완전 동등 아님 | **선택**(슬라이스용) |
  | Testcontainers MySQL | 운영 동등 | Docker 필요·느림 | 후속(운영 동등성 검증 시) |
  | 엔티티 columnDefinition 수정 | 근본 해결 | 운영 스키마 영향·범위 확대 | 기각(현 단계, 동작 보존 원칙) |
- **근거 출처**: 테스트 실패 로그(`BIT(1) ... syntax error`) + codex(우회 적절·운영 동등성 별도 필요).
- **코드** (application-test.yml):
  ```
    datasource:
      # MODE=MySQL: 엔티티에 MySQL 전용 columnDefinition(BIT(1) DEFAULT 1 등)이 있어 H2를 MySQL 호환 모드로 구동
      url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
  ```
  | 줄 | 근거 해설 |
  |----|----------|
  | MODE=MySQL | H2가 MySQL DDL 방언(BIT, enum 등) 수용 |
  | PersistenceSmokeTest | @DataJpaTest+replace=NONE으로 test 프로파일 URL 사용, @Import(QueryDslConfig)로 JPAQueryFactory 공급 |
- **리뷰 연습 포인트**:
  - (데이터 정합성 렌즈) MODE=MySQL이 운영 MySQL과 다른 동작(타입 강제·default)으로 결함을 가릴 지점은? → 운영 동등성은 Testcontainers로 별도 확보 필요

### J-5: SCC 특성화 테스트 — Order↔Payment↔User — `src/test/.../OrderPaymentRelationshipTest.java`

- **왜**: 리팩토링이 이 크로스 도메인 FK 순환을 ID/이벤트로 끊을 예정. 끊기 전 영속·연관 동작과 불변식(Payment는 항상 User 보유)을 고정해 회귀 그물 확보. 작동 베이스라인이 없던 상태라 "현 동작 캡처"가 아니라 "부팅 가능 베이스라인 + 불변식 고정" 성격.
- **근거 출처**: task.md 안전망 결정(사용자) + 엔티티 정독(Order/Payment/User/GeneralPayment).
- **코드** (핵심 단언):
  ```
          Order reloaded = em.find(Order.class, order.getId());
          assertThat(reloaded.getPayment()).isNotNull();
          assertThat(reloaded.getPayment().getOrder().getId()).isEqualTo(reloaded.getId());
          assertThat(reloaded.getPayment().getUser().getId()).isEqualTo(buyer.getId());
  ```
  | 줄 | 근거 해설 |
  |----|----------|
  | 양방향 단언 | Order.payment(mappedBy=order, cascade ALL)↔Payment.order — 소유측은 Payment의 order_id |
  | NOT NULL 테스트 | payment.user 미지정 시 flush 예외 — user_id NOT NULL 불변식 |
- **리뷰 연습 포인트**:
  - (테스트 전략 렌즈) 이 특성화가 FK→ID 전환 후에도 의미가 있나? — 전환 시 단언 대상이 바뀌므로 "전환 가이드"로 갱신 예정

## 2. 기계적 변경 (M — 동작 동일)

- `accessLog/aop/{AccessLogExceptionAspect,AccessLogMethodLoggingAspect,AccessLogPerformanceAspect}.java` → `aop/aspect/`로 이동. **동작 동일 근거**: 파일이 선언한 `package ...aop.aspect`와 importer(AccessLogAopConfiguration)가 이미 일치, 물리 위치만 맞춤(내용 무변경, rename 100%).
- `global/config/security/redis/{IpBlockingFilter,IpBlockingRedisService}.java` → 각각 `global/security/filter/`, `global/security/redis/`로 이동. **동작 동일 근거**: 파일이 선언한 package(`global.security.filter`/`redis`)와 SecurityConfig·IpBlockingFilter의 import가 이미 일치, 물리 위치만 맞춤.

## 3. 생성물 (G)

- 없음.
