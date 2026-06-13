# 학습 기록 (Learned)

> 작성일: 2026-06-13
> 관련 산출물: docs/plans/2026-06-13/modulith-refactoring/task.md
> 작업 요약: Spring Modulith 도입 + 빌드·기동 불가 main을 그린화 + JPA 슬라이스 안전망 구축 (모듈리스 Phase 0)

---

## 1. 사용된 라이브러리

| 라이브러리 | 버전 | 용도 | 왜 선택했는가 |
|-----------|------|------|-------------|
| Spring Modulith | 1.3.12 (BOM) | 모듈 경계/순환 정적 검증, 문서화 | Boot 통합·verify()·Documenter·이벤트 레지스트리. 단일 배포 유지(모듈리스)에 정합 |
| Spring Boot Test (DataJpaTest) | Boot 3.4.1 관리 | JPA 슬라이스 테스트 | 외부 인프라 없이 엔티티·레포 검증 |
| H2 (MySQL 모드) | Boot 관리 | 테스트 임베디드 DB | MySQL 전용 DDL을 호환 모드로 수용 |
| AssertJ / JUnit 5 | Boot 관리 | 단언/테스트 러너 | Boot 스타터 기본 |

---

## 2. 핵심 함수 / 메서드

### Spring Modulith

| 함수/메서드 | 시그니처 | 역할 | 사용 위치 |
|------------|---------|------|----------|
| `ApplicationModules.of` | `static ApplicationModules of(String basePackage)` | basePackage 직속 하위 패키지를 모듈로 탐지 | `ModularityTests:21` |
| `ApplicationModules.verify` | `ApplicationModules verify()` | 경계 위반/순환 검증, 위반 시 예외 | `ModularityTests:27` |
| `ApplicationModules.forEach` | `void forEach(Consumer)` | 탐지 모듈 순회(출력) | `ModularityTests:24` |

**사용 예시:**
```
ApplicationModules modules = ApplicationModules.of(DOMAIN_BASE_PACKAGE);

System.out.println("=== DETECTED MODULES START ===");
modules.forEach(module -> System.out.println(module.toString()));
System.out.println("=== DETECTED MODULES END ===");

try {
    modules.verify();
    System.out.println("=== NO MODULITH VIOLATIONS ===");
} catch (Throwable t) {
    System.out.println("=== MODULITH VIOLATIONS START ===");
    System.out.println(t.getMessage());
    System.out.println("=== MODULITH VIOLATIONS END ===");
}
```
- 출처: `src/test/java/com/fream/back/ModularityTests.java:21-34`

**코드 설명:**
> `ApplicationModules.of(base)` — base 직속 하위 패키지를 모듈로 잡는다. 도메인 패키지를 줘야 도메인=모듈.
> `verify()` — 위반 시 throw. 초기엔 위반이 정상이라 try/catch로 메시지만 캡처(빌드 비파괴).

### TestEntityManager (안전망)

| 함수/메서드 | 시그니처 | 역할 | 사용 위치 |
|------------|---------|------|----------|
| `persist` | `void persist(Object)` | 영속화(미flush) | `OrderPaymentRelationshipTest:42,60` |
| `flush` | `void flush()` | DB 반영(제약 검증 트리거) | 동 56,73 |
| `clear` | `void clear()` | 1차 캐시 비움(재조회 강제) | 동 57 |
| `find` | `<E> E find(Class<E>, Object)` | PK 조회 | 동 59 |

---

## 3. 어노테이션 / 데코레이터

| 어노테이션 | 소속 | 역할 | 적용 대상 |
|-----------|------|------|----------|
| `@DataJpaTest` | spring-boot-test | JPA 슬라이스 부팅(레포/EM, 트랜잭션 롤백) | 테스트 클래스 |
| `@AutoConfigureTestDatabase(replace=NONE)` | spring-boot-test | 임베디드 DB 자동교체 비활성 → test 프로파일 datasource 사용 | 테스트 클래스 |
| `@Import(QueryDslConfig.class)` | spring-context | 슬라이스에 JPAQueryFactory 빈 공급 | 테스트 클래스 |
| `@ActiveProfiles("test")` | spring-test | application-test.yml 활성화 | 테스트 클래스 |

**동작 원리:**
@DataJpaTest는 @Configuration 일반 빈을 로드하지 않으므로 QueryDSL 커스텀 레포가 의존하는 JPAQueryFactory가 없다 → @Import로 주입. replace=NONE이 없으면 @DataJpaTest가 datasource를 임의 임베디드로 바꿔 MODE=MySQL 설정이 무시된다.

---

## 4. 수정 전/후 코드 비교

### 파일: `global/config/security/SecurityConfig.java`
**수정 전:**
```
        LoginAuthenticationFilter loginFilter = new LoginAuthenticationFilter(
                userRepository, passwordEncoder, jwtTokenProvider, authRedisService);
```
**수정 후:**
```
        private final AuthService authService;
        ...
        LoginAuthenticationFilter loginFilter = new LoginAuthenticationFilter(authService);
```
**변경 이유:** 필터가 AuthService 단일 의존으로 리팩토링됐는데 호출부만 옛 시그니처 → 컴파일 차단. 의도된 주입 복원.

### 파일: `payment/repository/PaymentRepository.java`
**수정 전:** `@Query("SELECT p FROM Payment p WHERE p.createdAt >= :since")`
**수정 후:** `@Query("SELECT p FROM Payment p WHERE p.createdDate >= :since")`
**변경 이유:** Payment(BaseTimeEntity 상속)에 createdAt 속성 없음. @Query는 기동 시 검증되어 미사용이어도 부팅 차단.

(나머지 수정 전/후는 changelog J-3 표 참조)

---

## 5. 동작 구조

### 실행 흐름 (안전망 특성화 테스트)
```
@DataJpaTest 컨텍스트 부팅 (test 프로파일, H2 MySQL모드)
  → TestEntityManager 주입
    → newUser() 빌더로 유효 User 구성 → persist
    → Order.builder() + GeneralPayment.builder() → assignUser/assignPayment(양방향)
    → em.persist(order) (cascade ALL → payment)
    → em.flush() (제약 검증) → em.clear()
    → em.find(Order) → 연관 재로딩
  ← AssertJ 단언(양방향·FK·불변식)
```

### 컴포넌트별 역할
| 컴포넌트 | 파일 | 역할 |
|----------|------|------|
| ModularityTests | src/test/.../ModularityTests.java | 모듈/순환 ground truth 추출 |
| PersistenceSmokeTest | src/test/.../PersistenceSmokeTest.java | 전 엔티티 스키마·레포 부팅 검증 |
| OrderPaymentRelationshipTest | src/test/.../domain/characterization/ | SCC 엔티티 관계·불변식 고정 |

---

## 6. 디자인 패턴

| 패턴 | 적용 위치 | 왜 사용했는가 | 구조 |
|------|----------|-------------|------|
| Characterization Test | OrderPaymentRelationshipTest | 리팩토링 전 동작 고정(회귀 그물) | 현재 동작을 단언으로 박제 |
| Single Table Inheritance | Payment(추상)+Card/Account/General | 결제 수단 다형 | @Inheritance(SINGLE_TABLE)+@DiscriminatorColumn |
| 양방향 연관 편의 메서드 | Order.assignPayment 등 | 양측 동기화 캡슐화 | 한쪽 set 시 상대도 set |

---

## 7. 설정 / 컨벤션

| 항목 | 값 | 이유 |
|------|---|------|
| modulith base package | `com.fream.back.domain` | 도메인=모듈 매핑 |
| H2 모드 | `MODE=MySQL` | MySQL 전용 DDL 수용 |
| Modulith 버전 | BOM 1.3.12 | Boot 3.4 대응 라인 |

---

## 8. 테스트에서 사용된 것들

### 테스트 프레임워크
| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| JUnit 5 / AssertJ | Boot 관리 | 러너/단언 |
| spring-modulith-starter-test | 1.3.12 | ApplicationModules 검증 |

### 픽스처 / 팩토리
| 이름 | 유형 | 생성 대상 | 사용 위치 |
|------|------|----------|----------|
| `newUser(email, referralCode)` | private 헬퍼 | 유효 최소 User(필수 4필드) | OrderPaymentRelationshipTest |

### Assertion 메서드
| 메서드 | 소속 | 검증 내용 |
|--------|------|----------|
| `assertThat(x).isNotNull()/.isEqualTo()` | AssertJ | 연관 로딩·FK 일치 |
| `assertThatThrownBy(...).isInstanceOf(Exception.class)` | AssertJ | NOT NULL 제약 위반 |

**대표 테스트 코드:**
```
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class OrderPaymentRelationshipTest {
    @Autowired
    private TestEntityManager em;
```
- 출처: `src/test/java/com/fream/back/domain/characterization/OrderPaymentRelationshipTest.java:26-34`

---

## 9. 새로 알게 된 것

- **`@Query`는 미사용이어도 앱 기동을 막을 수 있다** — Spring Data가 빈 생성 시 JPQL을 검증. "컴파일 OK ≠ 기동 OK".
- **package 선언과 파일 위치가 따로 노는 잔재**가 컴파일을 막는다 — git mv로 위치만 맞추면 import 0줄 수정으로 해결.
- **javac는 어노테이션 프로세싱 중 첫 에러에서 일찍 멈춰** 에러를 다 안 보여준다 → 정적 import 스캔으로 선발굴이 유효.
- **Spring Modulith verify()는 컨텍스트를 안 띄운다**(정적 분석) — 인프라 없이 순환 추출 가능.
- **@DataJpaTest는 @Configuration을 안 읽는다** — QueryDSL 쓰면 @Import 필수.

---

## 10. 더 공부할 것

| 주제 | 왜 공부해야 하는가 | 참고 자료 |
|------|-----------------|----------|
| Spring Modulith `@ApplicationModule`/named interface/이벤트 | Phase 1+에서 경계 선언·이벤트 전환 핵심 | docs.spring.io/spring-modulith |
| Testcontainers MySQL | 운영 DB 동등성 검증(H2 모드 한계 보완) | testcontainers.org |
| Spring Modulith Event Publication Registry | 인프로세스 이벤트 내구성(유실 방지) | Modulith ref §events |
