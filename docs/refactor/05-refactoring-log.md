# 05. 리팩토링 로그 (구현 내역)

> 상세 changelog·계획은 `docs/plans/2026-06-13/modulith-refactoring/`. 여기는 요약.

## PR 단위 진행
| PR | 제목 | 핵심 |
|----|------|------|
| #1 | Phase 0 그린 빌드 + Spring Modulith | 컴파일/기동 차단 8건 수정, Modulith 도입, JPA 슬라이스 안전망 |
| #3 | CI/CD 간소화 | Dockerfile 베이스 교체(제거된 openjdk→temurin), CI 활성·통과, 배포 제거 |
| #2 | Phase 1 경계 전체 선언 | 19개 도메인 `@ApplicationModule` + Before 다이어그램 |
| #4 | 비순환 모듈 allowedDependencies 잠금 | accessLog/monitoring/weather=`{}`, faq/inquiry/inspection/event/chatQuestion/notice 의존 한정 |
| #5 | user 조회 API named interface | `user.service.query` 노출 → faq·inspection clean |
| #6 | inquiry 검색 쿼리 버그 수정 + 안전망 | Projections 생성자 불일치(13/14인자) 수정, 특성화 테스트 |
| #7 | user 배치 조회 API | `UserSummary` + `findUserSummaries/findUserSummary`(fetch join) |
| #8 | **Inquiry FK→ID 전환** | `Inquiry.user`→`userId`, 검색/응답 user 요약 enrich, inquiry clean |
| #9 | chatQuestion 계획 문서 | touch point 매핑 |
| #10 | **chatQuestion FK→ID 전환** | ChatQuestion/GPTUsageLog FK→userId, faq named interface, chatQuestion clean |

## 수정한 잠재버그 (총 9건 — 모두 "중단된 리팩토링 잔재")
1. 패키지 선언≠파일 위치(accessLog 아스펙트 3, IpBlocking 2) → 이동
2. `OrderQueryController` 죽은 import(미구현 OrderStatusDto/OrderQueryService)
3. `getUpdatedAt()` → `getModifiedDate()`(BaseTimeEntity 실 getter)
4. `AddressLoggingAspect` 람다 캡처(final)
5. `SecurityConfig` 인증 와이어링(LoginAuthenticationFilter ← AuthService)
6. `PaymentRepository` JPQL `createdAt`→`createdDate`(기동 차단)
7. `User.isActive` MySQL 전용 DDL → 테스트 H2 MySQL 모드 우회
8. `InquirySearchResultDto` 생성자 투영 불일치(런타임 ExpressionException)

> 발견 경위: 커밋된 main이 **빌드·기동 불가** 상태였고, 정적 import 스캔 + 반복 컴파일 + JPA 슬라이스 부팅으로 발굴.

## 확립한 자산/패턴
- **FK→ID + 요약 API enrich** 패턴(02 문서) — inquiry·chatQuestion에 적용, 거래코어에 재사용 예정.
- user 공개 API: `findUserSummary(s)`, `findUserIdByEmail`, `isAdmin`, `getRoleName`, `UserSummary`.
- @NamedInterface 노출: `user::query`, `faq::query`, `faq::dto`.
- 테스트: `@DataJpaTest` + H2 MySQL 모드 슬라이스, 도메인별 특성화 테스트.
- 검증: codex 교차검증(Phase 0), 모든 커밋/PR 한국어.

## 남은 로드맵 (요약)
1. event/notice clean화 — product/notification named interface 노출 또는 이벤트화.
2. **notification 이벤트화(Phase 3)** — 순환 #10 제거(직접 주입 → ApplicationEvent).
3. **거래 코어(SCC)** — order/payment/sale/shipment/warehouseStorage FK→ID + 결제완료 사가 이벤트화.
4. **bid/order 모델 재설계** — [06 문서](06-bid-order-issue.md).
5. 공유커널 정리(global이 domain 역참조 18곳), security↔user 분리.
- 상세: `docs/plans/2026-06-13/modulith-refactoring/next-steps.md`
