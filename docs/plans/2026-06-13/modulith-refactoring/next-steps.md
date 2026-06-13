# NEXT STEPS — 모듈리스 리팩토링 잔여 로드맵

> 작성일: 2026-06-13 (Phase 0 완료 시점)
> 기준 ground truth: `modulith-verify-report.txt` (100 cycle · 10모듈 SCC) · `task.md` §2 의존성 분석
> 목표 아키텍처: Spring Modulith(단일 배포) · 경계부터 전체 선언 · 이벤트 드리븐 디커플링

이 문서는 **Phase 0 이후 해야 할 일 전체**를 실행 가능한 단위로 적는다. 각 항목은 독립 검증 가능한 페이즈로 쪼개고, 페이즈마다 빌드/테스트 그린을 유지한다(strangler).

---

## 현재 상태 (Phase 0 완료)
- ✅ 그린 빌드 (`./gradlew compileJava`), 잠재버그 8건 해소
- ✅ Spring Modulith 도입 + 순환 ground truth 확보
- ✅ JPA 테스트 슬라이스 + SCC 특성화 테스트 2건(Order↔Payment↔User)
- ✅ 제품 문서 + codex 교차검증

---

## A. 안전망 확장 (SCC 특성화 테스트 잔여)

리팩토링이 끊을 크로스 도메인 FK를 끊기 전에 고정. 각 항목 = 엔티티 정독 + @DataJpaTest 특성화.

| # | 관계 | 고정할 불변식 | 비고 |
|---|------|--------------|------|
| A-1 | sale ↔ payment | payment.sale_id (order와 배타), `assignSale`가 order=null로 만드는지 | Payment 추상→구체 서브클래스 |
| A-2 | order ↔ shipment | order_shipment.order_id (NOT NULL unique), cascade | OrderShipment 엔티티 정독 |
| A-3 | sale ↔ shipment | seller_shipment.sale_id (NOT NULL unique) | SellerShipment 정독 |
| A-4 | order ↔ warehouseStorage | warehouse.order_id, 상태 전이 | WarehouseStorage 정독 |
| A-5 | order ↔ sale (입찰) | OrderBid ↔ SaleBid 매칭 관계 | bid 엔티티들 정독 |
| A-6 | sale → product/user | sale.product_size_id, seller_id | ProductSize·User FK |
| A-7 | user 집계 | User ↔ Profile/Address/PaymentInfo/Interest cascade | identity 분리 전 고정 |

> 패턴은 `OrderPaymentRelationshipTest` 재사용. 공통 픽스처(`newUser` 등)를 `test/.../support/TestEntities`로 추출 검토.

---

## B. Phase 1 — 경계 전체 선언 (boundaries-first)

1. 전 도메인 루트에 `package-info.java` + `@ApplicationModule` 선언 (초기엔 패키지 이동 없이 선언만).
2. `allowedDependencies` 명시로 의도된 의존만 허용.
3. `ModularityTests.verify()`를 **실패 상태로 유지**하되, 위반 수를 지표화 → 위반 백로그 확정(§task.md §5 카탈로그를 verify 실데이터로 교체).
4. `Documenter`로 Before 다이어그램 저장(포트폴리오용).

> 결정 필요: (a) 모듈 = 도메인 1:1 vs 모듈 맵(identity/catalog/trade/...) 그룹핑. (b) trade 5개를 1모듈로 둘지.

---

## C. Phase 2 — 공유커널(shared) 정리
- `global` → 진짜 공유물(BaseEntity·ResponseDto·ErrorCode·공통 util)만 `shared`로, 나머지는 도메인 귀속.
- **security ↔ user 결합 분리**: 인증 컨텍스트(SecurityUtils, 42파일)는 shared, user 조회는 `identity` 공개 API로.
- `global`이 domain 역참조하지 않도록(현재 18파일 위반).
- BaseTimeEntity 감사 정합성 점검: `@EntityListeners(AuditingEntityListener)`가 붙었으나 `@EnableJpaAuditing`·`@CreatedDate` 부재 — 현재는 @PrePersist로만 채워짐. 의도 확인 후 일원화.

---

## D. Phase 3 — 🟢 서비스 호출 순환 해소 (이벤트 전환, 저위험 우선)
- **notification 이벤트화 (1순위)**: user/sale/shipment/payment/notice → notification 직접 주입 제거.
  - 발행자: 각 도메인이 도메인 이벤트(예: `PaymentCompletedEvent`) 발행(`ApplicationEventPublisher`).
  - 수신자: `notification`이 `@ApplicationModuleListener`로 수신(트랜잭션 커밋 후 비동기).
  - 순환 #10(user↔notification) 우선 해소.
- 도입 시 `spring-modulith-starter-jpa`(이벤트 발행 레지스트리)로 이벤트 유실 방지.

---

## E. Phase 4 — 🟡 조회/혼합 결합 해소 (공개 API화)
- product↔user(#6): user FK(`Interest`) userId화, product→user 조회는 identity API.
- product↔style(#7): 상호 조회 단방향화.
- user↔payment(#11)·user↔style(#12)·user↔address(#9): identity 집계 경계 재설정.

---

## F. Phase 5 — 🔴 거래 코어 정리 (order 완전 재설계, 고위험·마지막)
- **order 재설계**: query 측 미구현(OrderQueryService/OrderStatusDto) 정식 구현 포함.
- trade 모듈 내 order/payment/sale/shipment/warehouse 간 엔티티 FK → ID + 이벤트로 전환(#1~5,#8).
- 결제완료 사가(saga)를 이벤트 체인으로 명확화, 트랜잭션 경계 재정의.
- 안정화 후 trade 내부 서브모듈 분할 검토.

---

## G. Phase 6 — 검증 & 문서화
- `verify()` 전면 통과(위반 0).
- `@ApplicationModuleTest` 슬라이스 테스트 확충.
- `Documenter` After 다이어그램 → README/포트폴리오(Before/After 비교).

---

## H. 정리 백로그 (틈틈이)
- **`_backup` 잔재 빈 4개 제거**: `GeoIPService_backup`·`UserAccessLogConsumer_backup`·`UserAccessLogProducer_backup`·`UserAccessLogQueryService_backup` (활성 @Service — 동일 토픽 중복 소비 등 잠재버그 점검 후 제거).
- **MySQL 전용 columnDefinition 이식성**: `User.isActive`의 `BIT(1) DEFAULT 1` 등 — 운영 스키마 영향 검토 후 portable화 또는 테스트 전략 유지 결정.
- **full boot 검증**: 인증 와이어링(Phase 0 #6) 런타임 기동 — Testcontainers(MySQL/Redis/Kafka/ES) 또는 모킹으로 `@SpringBootTest` 부팅.
- **`FreamBackApplicationTests` 정리**: 인프라 없이 실패하는 기본 컨텍스트 테스트를 조건부 비활성/분리해 `./gradlew build` 그린화.
- **logs/ 추적 제외**: 현재 `logs/*.log`가 git 추적됨 → `.gitignore` 정리 검토.

---

## 미해결 결정 (Phase 1 진입 전)
1. 모듈 입도: 도메인 1:1 vs 모듈 맵 그룹핑(identity/catalog/trade/feed/support/notification/platform).
2. 패키지 물리 이동 vs 현 패키지 유지 + `@ApplicationModule`만.
3. trade 5개 도메인: 초기 1모듈 유지 vs 분리 시도.
4. 운영 동등성 테스트(Testcontainers) 도입 시점.
