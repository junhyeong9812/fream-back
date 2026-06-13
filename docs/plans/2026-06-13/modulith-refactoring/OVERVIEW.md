# OVERVIEW: 모듈리스 Phase 0 — 그린 빌드 + Spring Modulith 도입 + 안전망

> 추상 진입점. 이 Phase가 무엇을 했고 어떤 순서·분기로 돌았는가. 딥다이브는 아래 인덱스로.

## 주요 포인트

- **Spring Modulith를 도입해 모듈 경계 위반을 공식 추출한다** — 핵심은 `ApplicationModules.of("com.fream.back.domain").verify()`. 결과: 100 cycle·10모듈 SCC. → 메커니즘 `TECHNICAL §동작 방식`, 선택 이유 `changelog J-1`
- **커밋된 main이 빌드·기동 불가였다** — 컴파일 6건 + 기동차단 2건. 까다로운 곳은 javac가 첫 에러에서 멈춰 한 번에 안 드러남(정적 import 스캔 + 반복 컴파일로 발굴). → `TECHNICAL §실패 모드`, 수정 목록 `changelog J-2,J-3`
- **인증 와이어링 수정이 유일한 보안 접점** — `LoginAuthenticationFilter`가 `AuthService` 주입으로 바뀐 걸 SecurityConfig가 못 따라간 것. 위험은 빈 순환참조(codex: 없음). → `changelog J-2`, `review-log F-meta`
- **테스트는 H2 MySQL 호환 모드로 운용한다** — 엔티티가 MySQL 전용 DDL(`BIT(1) DEFAULT 1`)을 써서 H2 순정 모드 불가. 손실 지점은 운영 MySQL 동등성. → `TECHNICAL §외부 경계`, `changelog J-4`
- **안전망은 @DataJpaTest 슬라이스 + SCC 엔티티 특성화** — Order↔Payment↔User 양방향/불변식 고정. 작동 베이스라인이 없어 "캡처"가 아니라 "부팅 가능 베이스라인 + 불변식 고정". → `changelog J-5`, `learned §8`

## 워크플로우 (절차 + 분기)

```
(Phase 0 진입: 모듈리스 리팩토링 시작)
  │
  ▼
[Modulith 의존성 추가] ──▶ [./gradlew test]
  │
  ▼
  컴파일 OK? ──아니오─▶ [정적 import 스캔으로 깨진 참조 발굴]
  │                         │
  │                         ▼
  │                    [수정] ──▶ [재컴파일] ──반복(잔여 에러)──┐
  │                         ▲────────────────────────────────┘
  │                         │
  │                    컴파일 OK
  │  ◀──────────────────────┘
  ▼
[ModularityTests.verify()] ──▶ 위반 캡처(verify 실패를 메시지로) ──▶ (ground truth: 100 cycle/10모듈 SCC)
  │
  ▼
[PersistenceSmokeTest @DataJpaTest]
  │
  ▼
  스키마 생성 OK? ──아니오(BIT(1) DDL)─▶ [H2 MODE=MySQL] ──▶ 재시도
  │                                                            │
  │  ◀────────────────────────────────────────────────────────┘
  ▼
  레포 부팅 OK? ──아니오(@Query createdAt)─▶ [JPQL 수정] ──▶ 재시도
  │
  ▼
[SCC 특성화 테스트(Order↔Payment↔User)] ──▶ PASSED
  │
  ▼
[codex 교차검증] ──고확신 결함 0건──▶ [제품 문서 작성] ──▶ (Phase 0 완료)
```

## 딥다이브 인덱스

| 알고 싶은 것 | 문서·절 |
|---|---|
| 왜 그렇게 동작하나 (Modulith 모듈탐지·@DataJpaTest 슬라이스·실패모드) | TECHNICAL |
| 이번에 왜 그렇게 바꿨나 (선택·대안·근거) | changelog J-1~J-5, M |
| 무슨 요소를 어떻게 썼나 (Modulith API·@DataJpaTest·TestEntityManager) | learned |
| 리뷰에서 무엇이 오갔나 | review-log |
| 분석·계획·전체 로드맵 | task.md (§1~7), 순환 ground truth `modulith-verify-report.txt` |
