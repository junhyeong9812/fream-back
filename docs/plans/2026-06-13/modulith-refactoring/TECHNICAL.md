# TECHNICAL: 모듈리스 Phase 0 — 동작 모델

> diff 비종속 동작 모델. 다른 프로젝트에도 들고 갈 수 있는 개념·메커니즘·불변조건·실패모드. 절차 다이어그램은 OVERVIEW 소유.

## 알아야 하는 개념

### 개념 1: Spring Modulith Application Module
① Spring Modulith는 `@SpringBootApplication`(또는 지정 base package)의 **직속 하위 패키지**를 각각 하나의 "application module"로 본다. ② 본 작업은 `ApplicationModules.of("com.fream.back.domain")`로 base를 도메인 패키지에 고정해 20개 도메인을 모듈로 인식시켰다. ③ 이 base를 잘못 잡으면(예: `com.fream.back`) `domain`/`global` 단 2개 모듈로 보여 도메인 간 순환이 안 드러난다.

### 개념 2: 모듈 경계 위반과 순환(SCC)
① 모듈 A가 다른 모듈 B의 internal 타입을 참조하면 위반, 서로 참조하면 순환. ② Modulith의 `verify()`는 바이트코드 정적 분석(jMolecules/ArchUnit 기반)으로 이를 탐지 — Spring 컨텍스트를 띄우지 않는다. ③ 순환을 모르면 모듈을 물리 분리하는 순간 컴파일이 막힌다. 본 작업은 10개 도메인이 단일 SCC임을 확인.

### 개념 3: @DataJpaTest 슬라이스
① JPA 레이어만 부팅하는 테스트 슬라이스(엔티티·레포·EntityManager). 기본적으로 datasource를 임베디드로 **교체**하고 각 테스트를 트랜잭션으로 감싸 롤백한다. ② 외부 인프라(Redis/Kafka/ES) 없이 엔티티 매핑·레포 쿼리를 검증하는 데 적합 — 본 프로젝트의 안전망 토대. ③ `@Configuration`(예: QueryDslConfig)은 자동 로드되지 않아 `@Import` 필요, 안 하면 JPAQueryFactory 빈 부재로 커스텀 레포가 깨진다.

## 동작 방식

- **verify()의 비파괴 캡처**: `verify()`는 위반 시 예외(Violations)를 던진다. 리팩토링 초기엔 위반이 정상이므로 `try/catch`로 메시지만 출력해 빌드를 깨지 않고 ground truth를 얻는다. 정적 분석이라 인프라 불필요·고속.
- **@DataJpaTest + replace=NONE**: 기본은 임베디드 DB로 강제 교체하지만, `@AutoConfigureTestDatabase(replace=NONE)`를 주면 `application-test.yml`의 datasource(H2 `MODE=MySQL`)를 그대로 쓴다. 엔티티의 MySQL 전용 DDL을 H2가 수용하도록 하는 핵심 스위치.
- **Spring Data @Query 부팅 검증**: `@Query` JPQL은 빈 생성 시 EntityManagerFactory에 대해 의미 검증된다. 따라서 미사용 쿼리라도 속성명이 틀리면(`createdAt` vs `createdDate`) 컨텍스트 로딩 자체가 실패한다 — 컴파일은 통과하지만 기동에서 터지는 부류.

## 불변조건 / 계약

- `ApplicationModules.of(base)`의 base는 **도메인 패키지**여야 도메인=모듈 매핑이 성립. (깨지면 모듈 2개로 축소, 순환 미탐지)
- `Payment.user`는 NOT NULL — Payment는 항상 소유자(User)를 가진다. (깨지면 결제 주체 불명, FK 무결성 붕괴) → `OrderPaymentRelationshipTest`가 고정.
- `Order.payment`/`OrderShipment`/`WarehouseStorage`/`OrderBid`는 `mappedBy` + cascade ALL — Order가 영속 라이프사이클의 주인. 소유측 FK는 상대 테이블(payment.order_id 등).

## 상태와 소유권

- 시간 필드의 source of truth: `BaseTimeEntity.createdDate`(@PrePersist 설정, updatable=false)·`modifiedDate`(@PreUpdate). `@EntityListeners(AuditingEntityListener)`가 붙어 있으나 `@CreatedDate`/`@LastModifiedDate`가 없고 `@EnableJpaAuditing`도 부재 → 실제 채움은 JPA 라이프사이클 콜백이 담당(리스너는 무용지물·무해).
- 모듈 경계 ground truth: `modulith-verify-report.txt`(14433줄) — verify() 원본 출력 보관.

## 외부 경계와 의존성

- **테스트 DB(H2 MODE=MySQL)**: 신뢰 수준 = 매핑·관계·기본 쿼리 검증엔 충분, 운영 MySQL과 100% 동일 아님. 실패 모드 = MySQL 특이 타입/default/함수 동작은 H2가 다르게 처리해 결함을 가릴 수 있음 → 운영 동등성은 Testcontainers MySQL로 별도 확보 필요(후속).
- **Spring Modulith(런타임)**: starter-core만 추가 — 빈 자동등록 영향 최소. (jpa 스타터 추가 시 이벤트 발행 레지스트리 테이블이 필요해지므로 도입은 이벤트 전환 단계에서.)
- **인증(SecurityConfig→AuthService)**: AuthService 의존(UserRepository·PasswordEncoder·AuthRedisService·JwtTokenProvider)은 모두 SecurityConfig 밖(특히 PasswordEncoder는 EncoderConfig)에서 정의 → 빈 순환참조 없음(codex 확인). full boot 미검증이 잔여 리스크.

## 실패 모드 메커니즘

- **javac 조기 중단**: QueryDSL 어노테이션 프로세싱 라운드에서 컴파일 에러가 나면 javac가 일찍 멈춰, 보고된 2건 뒤에 더 많은 에러가 가려진다. → 대응: 깨진 내부 import를 정적 스캔으로 선발굴 + 한 건 고치고 재컴파일을 반복(단조 수렴).
- **package 선언 ≠ 파일 위치**: 파일을 옮기다 만 잔재. import와 package 선언은 서로 일치하는데 파일만 엉뚱한 디렉터리에 있으면 컴파일 불가. → 대응: 파일을 선언된 패키지 위치로 이동(0줄 import 수정).
- **@Query 속성 오타**: 컴파일은 통과, 컨텍스트 로딩에서 `UnknownPathException`. → 증상: 앱 기동 실패. 대응: 엔티티 실제 속성으로 교정.
- **MySQL 전용 DDL on H2**: `BIT(1) DEFAULT 1` 같은 columnDefinition이 H2 순정 파서에서 syntax error → users 테이블 생성 실패 → 모든 FK가 "table not found" 연쇄. → 대응: H2 MODE=MySQL.

## 함정

- `@DiscriminatorColumn`/`@Inheritance(SINGLE_TABLE)`로 `Payment`는 추상 — 테스트에서 구체 서브클래스(`GeneralPayment` 등)로 인스턴스화해야 하고, Lombok `@Builder`는 서브클래스 자체 필드만 빌드한다(부모 order/user/paidAmount는 `assignXxx`/`setPaidAmount` 메서드로 설정).
- `GeneralPayment` 생성자의 `status` 파라미터는 받기만 하고 PaymentStatus에 반영하지 않는다(죽은 인자) — 상태는 기본값 PENDING 유지.

## 해당 없음 사유
- (없음 — 위 절 모두 적용)
