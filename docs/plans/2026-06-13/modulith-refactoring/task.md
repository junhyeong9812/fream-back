# fream-back 모듈리스(Spring Modulith) 리팩토링 — 분석 & 계획

- 작성일: 2026-06-13
- 대상: `fream-back` (단일 Spring Boot 3.4.1 / Java 17 모놀리스, 791개 클래스)
- 목표 아키텍처: **Spring Modulith** (단일 배포 + 모듈 경계 강제 + 이벤트 드리븐 통신)
- 진행 전략: **경계부터 전체 선언** (전 도메인에 모듈 경계 선언 → 위반 가시화 → 순환부터 해소)
- 산출물 형태: 본 분석/계획 문서 (코드 변경은 후속 단계)

---

## 1. 배경 & 목표

첫 포트폴리오 프로젝트로, 도메인 간 구조와 코드 결합도가 높아 테스트·유지보수가 어렵다.
다음을 목표로 점진적 리팩토링을 진행한다.

1. **모듈화** — 도메인을 Bounded Context 단위 모듈로 정리, 경계를 컴파일/테스트로 강제
2. **결합도 분리** — 도메인 간 직접 호출(순환 포함)을 이벤트 드리븐으로 전환
3. **테스트 용이성** — 모듈 단위 슬라이스 테스트가 가능한 구조

> 비목표(현 단계): 물리적 멀티모듈 분리, MSA 분리. 단일 배포(모듈리스)를 유지한다.

---

## 2. 현황 분석

### 2.1 구조

- 패키지: `com.fream.back.domain.<20개 도메인>` + `com.fream.back.global`(공유 73개)
- 도메인 내부는 이미 `controller/command·query`, `service/command·query` 로 **CQRS형 분리**가 되어 있음 (의도는 양호)
- 규모(파일 수): user 117 · product 106 · style 67 · order 54 · chatQuestion 43 · payment 39 · event 35 · accessLog 33 · faq 30 · address 25 · sale 24 · inquiry 24 · warehouseStorage 22 · shipment 20 · notice 20 · inspection 20 · weather 19 · notification 14 · monitoring 5

### 2.2 도메인 간 의존성 매트릭스 (import 기준 파일 수)

```
출발\도착  user addr noti order pay prod sale ship ware style
address      4    -    -    -    -    -    -    -    -    -
chatQ        6    -    -    -    -    -    -    -    -    -   (+faq 2)
event        1    -    -    -    -    4    -    -    -    -
faq          1    -    -    -    -    -    -    -    -    -
inquiry      7    -    -    -    -    -    -    -    -    -
inspection   1    -    -    -    -    -    -    -    -    -
notice       1    -    1    -    -    -    -    -    -    -
notification 3    -    -    1    -    -    -    -    -    -
order        7    1    -    -    7    7    3    4    3    -
payment      5    -    1    3    -    -    2    -    -    -
product     13    -    -    3    -    -    -    -    -    3
sale         4    -    1    3    1    5    -    3    -    -
shipment     1    -    3    2    -    -    3    -    2    -
style       23    -    -    3    -    1    -    -    -    -
user         -    1    2    -    1    1    -    -    -    1
warehouse    2    -    -    2    -    -    4    -    -    -
```

### 2.3 순환 의존성 (모듈 경계의 핵심 장애물) — **12쌍**

| # | 순환쌍 | 주요 위반 지점 | 성격 |
|---|--------|----------------|------|
| 1 | order ↔ payment | `Order.java`↔`Payment.java`(엔티티), 양쪽 CommandService/KafkaConsumer | 🔴 엔티티+서비스 |
| 2 | order ↔ product | `OrderItem/OrderBid.java`→product, `ProductQueryService`→order | 🔴 엔티티+서비스 |
| 3 | order ↔ sale | `OrderBid.java`↔`SaleBid.java`, 양쪽 CommandService | 🔴 엔티티+서비스 |
| 4 | order ↔ shipment | `Order.java`↔`OrderShipment.java` | 🔴 엔티티 |
| 5 | order ↔ warehouseStorage | `Order.java`↔`WarehouseStorage.java` | 🔴 엔티티 |
| 6 | product ↔ user | product 다수→user, `User.java`→`Interest` | 🟡 주로 서비스+1엔티티 |
| 7 | product ↔ style | `ProductQueryService`↔`StyleRepositoryCustomImpl` | 🟡 조회 결합 |
| 8 | sale ↔ payment | `Sale.java`↔`Payment.java` | 🔴 엔티티 |
| 9 | user ↔ address | `User.java`↔`Address.java` | 🔴 엔티티 |
| 10 | user ↔ notification | UserCommandService/FollowCommandService ↔ NotificationCommandService | 🟢 서비스 호출 |
| 11 | user ↔ payment | `User.java`→`PaymentInfo`, payment 다수→user | 🟡 엔티티+서비스 |
| 12 | user ↔ style | `Profile.java`→style, style 다수→user/Profile | 🟡 엔티티+서비스 |

> **관찰**: order·payment·sale·shipment·warehouseStorage 5개가 양방향 엔티티 FK로 한 덩어리(거래 코어)를 이룬다. DB 스키마 결합이라 초기에 쪼개기 어렵다.

### 2.4 결합의 3가지 층위 (분리 난이도)

| 층위 | 근거(전수) | 전환 방법 |
|------|-----------|-----------|
| 🔴 JPA 엔티티 FK 직접 참조 | `User/Profile` 엔티티 타도메인 import 154회, 거래코어 5개 양방향 `@OneToOne/@ManyToOne` | FK→ID 참조화, 집계 경계 재설정 (가장 어려움) |
| 🟡 타 도메인 Service 직접 주입 | style→user 10, sale/order→user 3 등 동기 호출 | 응답 불필요 → **이벤트 발행**, 응답 필요 → **명시적 API 인터페이스** |
| 🟢 Exception/DTO/Repository 공유 | user exception 49, repository 35회 import | 공유커널 이동 또는 모듈 공개 API화 |

### 2.5 기존 메시징 인프라

- **Kafka 운영 중**: accessLog(UserAccessLog), order(OrderProcessingEvent), payment(PaymentCompletedEvent), product/style(조회수 ViewEvent) → 비동기 경험 있음
- **Spring ApplicationEvent(인프로세스)는 거의 미사용** (weather 1곳) → 모듈 내부 통신 전환의 가장 큰 개선 여지
- 정리 대상 잔재: `UserAccessLogConsumer_backup.java`, `UserAccessLogProducer_backup.java`

### 2.6 리스크 (선결 과제)

1. ⚠️ **테스트 안전망 부재** — `src/test`에 컨텍스트 로딩 테스트 1개(`FreamBackApplicationTests`)뿐. 회귀 탐지 그물이 없음.
2. ⚠️ **공유커널(global) 오염** — global 18개 파일이 domain을 역참조. 특히 `security` 레이어가 `user` 도메인에 의존. 깨끗한 shared kernel이 아님.
3. ⚠️ **거래 코어 엔티티 결합** — order/payment/sale/shipment/warehouse 5개 양방향 FK. 모듈 분리 시 가장 큰 비용.

---

## 3. 목표 아키텍처 (Spring Modulith)

### 3.1 모듈 맵 (Bounded Context)

| 모듈 | 포함 도메인 | 역할 | 비고 |
|------|------------|------|------|
| **identity** | user, address | 계정/프로필/인증 주체 | 모두가 의존하는 중심. 안정화 1순위 |
| **catalog** | product (+elasticsearch) | 상품/검색 | |
| **trade** | order, payment, sale, shipment, warehouseStorage | 거래 라이프사이클 | 초기엔 **1개 모듈로 유지**, 내부 정리 후 분할 검토 |
| **feed** | style | SNS 피드 | identity·catalog·trade 의존 |
| **support** | faq, inquiry, notice, chatQuestion, inspection | 고객지원/CS | |
| **notification** | notification | 알림 sink | **이벤트 수신 전용으로 전환** |
| **platform** | accessLog, monitoring, weather, event | 부가/인프라/프로모션 | |
| **shared(공유커널)** | global 일부 (BaseEntity, ErrorCode, ResponseDto, 공통 util) | 전 모듈 공유 | domain 역참조 제거 필요 |

### 3.2 통신 방식 원칙

1. **상태 변경 통지(응답 불필요)** → `ApplicationEventPublisher` + `@ApplicationModuleListener` (인프로세스 이벤트, 트랜잭션 커밋 후 비동기)
   - 예: 결제완료 → 알림/창고/배송 처리, 팔로우 → 알림
2. **즉시 조회(응답 필요)** → 대상 모듈이 노출한 **공개 API 인터페이스**만 호출 (Named Interface)
   - 예: 주문 생성 시 상품 가격 조회 → `catalog`의 `ProductApi.getPrice(id)`
3. **모듈 간 엔티티 직접 참조 금지** → FK 대신 **ID 값**으로 참조, 필요한 데이터는 이벤트 페이로드 또는 조회 API로
4. **모듈 외부 진짜 비동기/내구성 필요** → 기존 **Kafka** 유지 (조회수, 액세스로그 등)

### 3.3 Spring Modulith 핵심 메커니즘 활용

- `package-info.java` + `@ApplicationModule` 로 모듈 선언, `allowedDependencies` 로 허용 의존 명시
- `ApplicationModules.of(...).verify()` 테스트로 경계 위반/순환 **컴파일타임에 가깝게 차단**
- `@ApplicationModuleListener` (= `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` + 트랜잭션) 로 이벤트 처리
- **Event Publication Registry** (`spring-modulith-starter-jpa`) 로 이벤트 유실 방지(미완료 이벤트 재발행)
- `@ApplicationModuleTest` 로 모듈 슬라이스 테스트 (테스트 용이성 목표 직결)
- `Documenter` 로 모듈 의존 다이어그램 자동 생성(PlantUML/C4) → 포트폴리오 문서화

---

## 4. 전환 로드맵 ("경계부터 전체 선언")

핵심 아이디어: **먼저 전 도메인에 모듈 경계를 선언**하여 `verify()`가 모든 위반을 뱉게 만든 뒤, 그 목록을 백로그 삼아 순환부터 차례로 해소한다. (위반을 끄지 않고 "가시화 → 하나씩 제거")

### Phase 0 — 안전망 & 인프라 (선결)
- [ ] 의존성 추가: `spring-modulith-starter-core`, `-starter-jpa`(이벤트 레지스트리), `-starter-test`, `-actuator`, `-docs`(BOM으로 버전 관리)
- [ ] 핵심 플로우 **특성화 테스트(characterization test)** 최소셋 작성: 주문 생성·결제 완료·스타일 좋아요·알림 발행 등 (현재 회귀 그물이 없으므로 필수)
- [ ] `ApplicationModules.of(FreamBackApplication.class).verify()` 테스트 추가 (이 시점엔 **실패**가 정상 — 위반 목록 확보)
- [ ] `_backup.java` 등 잔재 파일 제거

### Phase 1 — 경계 전체 선언 + 위반 가시화
- [ ] 패키지를 모듈 맵(§3.1)에 맞게 정리 (`domain.user`+`domain.address` → `identity` 등). 초기엔 패키지 이동 없이 `@ApplicationModule`만 선언하는 점진안도 가능
- [ ] 각 모듈 루트에 `package-info.java` + `@ApplicationModule(allowedDependencies=...)` 선언
- [ ] `verify()` 결과를 위반 백로그로 정리 (순환 12쌍이 핵심 타깃)
- [ ] `Documenter`로 현재 의존 그래프 스냅샷 저장 (Before)

### Phase 2 — 공유커널(shared) 정리
- [ ] `global` → 진짜 공유물(BaseEntity, ResponseDto, ErrorCode, 공통 util)만 `shared`로, 나머지는 해당 모듈로 귀속
- [ ] **security ↔ user 결합 분리**: 인증 컨텍스트(SecurityUtils, 42개 파일 사용)는 shared로, user 조회는 `identity`의 공개 API로
- [ ] shared가 domain을 역참조하지 않도록 정리 (현재 18개 위반)

### Phase 3 — 🟢 서비스 호출 순환 해소 (이벤트 전환, 저위험)
- [ ] **notification 이벤트화 (1순위)**: user/sale/shipment/payment/notice → notification 직접 주입 제거, 각자 도메인 이벤트 발행 → `notification`이 `@ApplicationModuleListener`로 수신 (순환 #10 해소)
- [ ] user ↔ notification, user 관련 서비스 순환 우선 정리

### Phase 4 — 🟡 조회/혼합 결합 해소 (공개 API화)
- [ ] product ↔ user (#6): user FK(`Interest`)는 userId로, product→user 조회는 identity API로
- [ ] product ↔ style (#7): 상호 조회를 한쪽 공개 API/이벤트로 단방향화
- [ ] user ↔ payment (#11), user ↔ style (#12), user ↔ address (#9): identity 집계 경계 재설정

### Phase 5 — 🔴 거래 코어 정리 (고위험, 마지막)
- [ ] trade 모듈 내부에서 order/payment/sale/shipment/warehouse 간 엔티티 FK 점검
- [ ] 모듈 간(예: trade ↔ catalog) 엔티티 참조를 ID+이벤트로 전환 (#1~5, #8)
- [ ] 트랜잭션 경계 재정의, 결제완료 사가(saga) 흐름을 이벤트 체인으로 명확화
- [ ] 안정화 후 trade 내부 서브모듈 분할 검토

### Phase 6 — 검증 & 문서화
- [ ] `verify()` 전면 통과
- [ ] `@ApplicationModuleTest` 슬라이스 테스트 확충
- [ ] `Documenter` After 다이어그램 → README/포트폴리오 반영 (Before/After 비교)

---

## 5. 순환 해소 카탈로그 (백로그)

| # | 순환 | 1차 전략 | Phase |
|---|------|---------|-------|
| 10 | user ↔ notification | notification을 이벤트 수신 전용으로 (발행자가 이벤트 push) | 3 |
| 6 | product ↔ user | FK→userId, 조회는 identity API | 4 |
| 7 | product ↔ style | 단방향화(이벤트 or 공개 조회 API) | 4 |
| 9 | user ↔ address | address를 identity 모듈 내부로 흡수 검토 | 4 |
| 11 | user ↔ payment | PaymentInfo 소유권 정리(identity vs trade), userId 참조 | 4/5 |
| 12 | user ↔ style | Profile→style 역참조 제거, 이벤트화 | 4 |
| 1 | order ↔ payment | 결제완료 이벤트 체인, 엔티티 FK→ID | 5 |
| 2 | order ↔ product | 주문 시 상품 스냅샷/ID 참조, catalog API 조회 | 5 |
| 3 | order ↔ sale | 입찰 매칭 흐름 이벤트화 | 5 |
| 4 | order ↔ shipment | 배송 생성 이벤트화 | 5 |
| 5 | order ↔ warehouse | 창고보관 이벤트화 | 5 |
| 8 | sale ↔ payment | 정산 흐름 정리 | 5 |

---

## 6. 검증 전략

- **머지 전 최소 안전선**: `ApplicationModules.verify()` + 특성화 테스트 통과, `./gradlew build` 성공
- 각 Phase는 **위반을 늘리지 않는다**(단조 감소). verify 위반 수를 지표로 추적
- 고위험(Phase 5) 변경은 이벤트 흐름 통합 테스트로 결제/주문 시나리오 회귀 확인
- 모든 단계는 점진적(strangler) — 한 번에 한 순환씩, 빌드 그린 유지

---

## 7. 다음 액션 (제안)

1. **Phase 0 착수**: Spring Modulith 의존성 추가 + `verify()` 테스트 + 핵심 4개 플로우 특성화 테스트
2. 그 후 **Phase 1**: 전 도메인 경계 선언 → 위반 백로그 확정
3. 이후 §5 카탈로그 순서대로 순환 해소

> 미해결 질문: (a) 패키지를 모듈 맵대로 물리 이동할지 vs 현 패키지 유지하고 `@ApplicationModule`만 선언할지, (b) trade 5개 도메인을 1모듈로 둘지 초기부터 분리 시도할지 — Phase 1 진입 시 결정.

---

## 8. Phase 0a 실행 결과 (2026-06-13)

### 8.1 한 일
- build.gradle에 Spring Modulith 도입: BOM `spring-modulith-bom:1.3.12`(Boot 3.4 대응 라인) + `starter-core`(impl) + `starter-test`(test)
- `src/test/.../ModularityTests.java` 추가: `ApplicationModules.of("com.fream.back.domain")`로 모듈/위반 캡처 (verify 실패를 빌드 실패로 두지 않고 메시지 캡처)
- `./gradlew test` 실행 → **Spring Modulith 의존성은 정상 해결**

### 8.2 🚨 결정적 발견: main 브랜치가 컴파일되지 않음 (기존 문제)
`verify()`에 도달하기 전 `compileJava`에서 빌드 실패. git status clean이므로 **커밋된 코드 자체가 빌드 불가**. 모듈리스 작업의 절대 선결과제.

내부 import 정적 전수 검사 결과, 컴파일 차단 이슈 **3개 클러스터** (Q* 클래스 20여 개는 QueryDSL 빌드 생성물이라 오탐 제외, static-wildcard 4개도 정상):

| # | 위치 | 증상 | 근본 원인 | 사용처 |
|---|------|------|----------|--------|
| 1 | `accessLog/aop/` 아스펙트 3개 | 파일은 `aop/`에 있으나 `package ...aop.aspect;` 선언 | 파일 위치 ≠ package | `AccessLogAopConfiguration` |
| 2 | `global/config/security/redis/IpBlocking*` 2개 | 파일은 `config/security/redis/`에 있으나 `package global.security.filter/redis;` 선언 | 파일 위치 ≠ package | `SecurityConfig`, `IpBlockingFilter` |
| 3 | `order/controller/query/OrderQueryController` | 미구현 `OrderStatusDto`·`OrderQueryService` import (필드는 이미 주석) | 미구현 기능 죽은 import | (자기 자신) |

**루트 원인**: 패키지 이동/리네임 리팩토링이 중단되어 파일 위치·package 선언·import가 동기화되지 않은 잔재. §2의 결합도 난잡함과 동일한 병의 다른 증상.

**한계**: 정적 스캔은 "import 해소 불가" 유형만 포착. 위 3개 수정 후 재컴파일하면 메서드 시그니처 등 다른 컴파일 에러가 추가로 드러날 수 있음.

### 8.3 방향 갱신 (사용자 입력 반영)
- **order 도메인은 완전 재설계 방향** — order는 순환의 허브(payment·product·sale·shipment·warehouse·user·address 의존 + 다수가 역의존)이자 query 측이 미구현/깨짐. 모듈리스 리팩토링의 **중심**으로 다룬다.
- **도메인 간 연결관계 전면 개선** — §3~5의 이벤트 드리븐 전환을 12쌍 순환 전체에 적용(부분 패치 아님). boundaries-first 전략이 이를 포괄: verify()가 모든 불법 엣지를 열거 → 0으로 수렴.

### 8.4 다음 단계 (재정렬)
1. ✅ **그린 빌드 확보(선결)** — 완료 (§8.5)
2. **안전망**: 그린 빌드 위에서 핵심 플로우 특성화 테스트 (대규모 재설계 전 회귀 그물)
3. ✅ **verify() ground truth 확보** — 완료 (§8.6)
4. **재설계**: order 중심으로 전 도메인 연결관계 이벤트 드리븐 전환 (Phase 3~5 재구성)

### 8.5 그린 빌드 확보 결과 (Phase 0b 완료)
`./gradlew compileJava` **BUILD SUCCESSFUL**. 정적 스캔(import)이 못 잡은 시그니처 에러까지 반복 컴파일로 발굴해 총 **5개 컴파일 차단 이슈** 해소:

| 수정 | 파일 | 내용 | 성격 |
|------|------|------|------|
| 1 | `accessLog/aop/aspect/*` (3) | `aop/` → `aop/aspect/`로 이동(package 선언 일치) | 구조 이동 |
| 2 | `global/security/{filter,redis}/IpBlocking*` (2) | `config/security/redis/` → 선언된 패키지 위치로 이동 | 구조 이동 |
| 3 | `OrderQueryController` | 미구현 `OrderStatusDto`·`OrderQueryService` 죽은 import 제거 | 죽은 코드 |
| 4 | `OrderQueryController:62` | `getUpdatedAt()` → `getModifiedDate()` (BaseTimeEntity 실 getter) | 잘못된 심볼 |
| 5 | `AddressLoggingAspect:305~` | 람다 캡처용 `final int index = i;` 도입 | 람다 final |
| 6 | `SecurityConfig:40` | `LoginAuthenticationFilter`가 `AuthService` 1개 받도록 변경됨 → SecurityConfig에 `AuthService` 주입 + 호출 교정 | ⚠️ **인증 와이어링** |

- 모든 수정 **로직 보존**(구조 이동·심볼 교정). #6만 인증 영역(stakes 상승) — 필터가 이미 `AuthService.login()`을 쓰도록 작성돼 있어 *의도 복원*이며, `AuthService`는 `@Service`(주입 가능). 잔여 리스크: Spring 기동 시 순환참조 — `AuthService` 의존(UserRepository·PasswordEncoder·AuthRedisService·JwtTokenProvider)은 모두 SecurityConfig 밖 정의라 순환 가능성 낮음. **인프라 부재로 앱 기동 검증은 미수행(후속 필요)**.
- 추가 인프라 변경: `build.gradle` test에 `testLogging.showStandardStreams` 추가(진단용).

### 8.6 Spring Modulith verify() ground truth
`ModularityTests` 실행 결과 (`modulith-verify-report.txt`, 14433줄 보관):
- **탐지 모듈 19개**, **Cycle detected 100건**
- **순환에 엮인 10개 모듈 = 사실상 단일 거대 SCC**: `address · user · payment · sale · shipment · notification · order · product · style · warehouseStorage`
  → §2.3의 grep 추정(12쌍)이 공식 도구로 확증됨. order/notification이 SCC의 거의 모든 경로에 등장 = 허브.
- **`_backup` 잔재 빈 4개** 확인(활성 @Service/@Component): `GeoIPService_backup · UserAccessLogConsumer_backup · UserAccessLogProducer_backup · UserAccessLogQueryService_backup` → 정리 대상.

> 미작성(후속): 이 증분의 제품 산출물(OVERVIEW·changelog·learned·TECHNICAL)과 인증 와이어링(#6) 기동 검증 — 안전망 단계와 묶어 처리 예정.

### 8.7 안전망 테스트 인프라 확보 (Phase 0c)
`PersistenceSmokeTest`(@DataJpaTest) **PASSED** — 전체 JPA 엔티티 모델이 H2에서 스키마 생성 + 모든 JPA 레포(QueryDSL 커스텀 impl 포함) 정상 구성. SCC 엔티티 특성화 테스트의 토대 완성.

구축 과정에서 발견·해결한 **추가 잠재 버그**(모두 앱 기동/테스트 차단 — 같은 "리팩토링 잔재" 계열):

| # | 문제 | 처리 | 비고 |
|---|------|------|------|
| 7 | `User` 엔티티가 MySQL 전용 `BIT(1) DEFAULT 1` columnDefinition 사용 → H2 스키마 생성 실패 | `application-test.yml` H2 `MODE=MySQL`로 우회(엔티티 미변경) | **이식성/테스트용이성 이슈** — 운영 MySQL 결합 |
| 8 | `PaymentRepository.findPaymentsCreatedSince` JPQL이 `p.createdAt` 참조(속성은 `createdDate`) → Spring Data 기동 검증 실패 | `p.createdDate`로 수정 | **앱 기동 차단 버그** (미사용이어도 검증됨) |

테스트 설정: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace=NONE)`(test 프로파일 H2 MySQL모드 사용) + `@Import(QueryDslConfig)` + `@ActiveProfiles("test")`.

### 8.9 SCC 특성화 테스트 — 첫 배치 (Order↔Payment↔User)
`OrderPaymentRelationshipTest` 2건 **PASSED**:
- `orderAndPayment_bidirectionalRelationship_persistsAndLoads` — order↔payment 양방향 + payment→user cascade 영속/재로딩 고정
- `payment_userIsMandatory_notNullConstraintHolds` — payment.user_id NOT NULL 불변식 고정

확립된 패턴(재사용): `@DataJpaTest` + `@AutoConfigureTestDatabase(replace=NONE)` + `@ActiveProfiles("test")` + `@Import(QueryDslConfig)` + `TestEntityManager`. 엔티티는 빌더 + `assignXxx` 편의 메서드로 구성.

**SCC 잔여 커버리지(다음 배치)**: sale↔payment, sale↔shipment(seller_shipment), order↔sale(bid 매칭), order↔shipment(order_shipment), order↔warehouseStorage, sale→product/user — 각 엔티티 정독 + 픽스처 필요.

> 참고: 기본 `./gradlew test`(전체)는 `FreamBackApplicationTests`(@SpringBootTest)가 인프라(Redis/Kafka/ES) 필요로 실패. 안전망은 @DataJpaTest 슬라이스로 운용하며, full boot 검증은 별도 과제(인프라 모킹 or Testcontainers).

### 8.10 Phase 0 문서·리뷰 정리 (완료)
- 제품 산출물 작성: `OVERVIEW.md` · `changelog.md` · `learned.md` · `TECHNICAL.md` · `review-log.md` + `docs/measurement-log.md` 1행
- **codex 교차검증 1회** (중간 stakes): 보안 스캔 0건 통과 → 패킷(소스 diff+테스트) 전송 → **고확신 정확성 결함 0건**. SecurityConfig 순환참조 없음·버그수정 타당·Modulith 호환 확인. (`codex-review-input.md`/`codex-review-output.md` 보관)
- 머지 전 최소 안전선(core §4.3): 테스트 통과 ☑ / diff self-review ☑ / 가역성 ☑ / public contract 영향 없음 ☑ / 반증질문(순환참조·NOT NULL) ☑
- 잔여 리스크: full boot 미검증(인프라 부재), H2≠MySQL 동등성 → Testcontainers 후속.
- ⚠️ 부수 변경(커밋 비대상): `logs/*.log`(테스트 실행 오염)·`gradlew`(chmod +x) — 커밋 시 제외 필요.

### 8.8 누적 함의 (중요)
컴파일 6건 + 기동차단 2건 = **커밋된 main은 빌드도 기동도 안 되던 상태**. "기존 동작을 보존"하는 특성화 테스트의 전제(작동하는 베이스라인)가 약함 → 안전망은 *현 동작 캡처*가 아니라 **"부팅 가능한 베이스라인을 만들며 핵심 불변식을 고정"**하는 성격으로 진행한다.
