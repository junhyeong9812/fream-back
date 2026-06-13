# changelog: 비순환 모듈 allowedDependencies 잠금

> 대상 diff: 비순환 9개 모듈의 `domain/*/package-info.java`. 순차 3커밋(451fe20·753893d·fd9efb5).
> 전략: SCC 밖 9개 모듈의 모듈 레벨 의존을 명시·한정해 경계를 실제 강제하기 시작.

**검증 상태**: 통과 — 각 증분마다 `ModularityTests` 실행. 무의존 3개 위반 0건, 의존 6개는 의존 모듈 집합이 허용 목록과 정확히 일치(허용 외 의존 0건). compileJava 그린.

## 커버리지 셀프체크
- J: accessLog·monitoring·weather·faq·inquiry·inspection·event·chatQuestion·notice package-info (9)
- □ 완료

---

## 1. 판단 항목 (J)

### J-1: 무의존 모듈 완전 잠금 — accessLog·monitoring·weather

- **왜**: 타 도메인 의존이 전혀 없는 모듈은 `allowedDependencies = {}`로 "의존 금지"를 강제 가능 → 진짜 verify-clean.
- **검증**: verify() 출력에 3개 모듈 위반 0건.
- **코드**: `@ApplicationModule(displayName = "...", allowedDependencies = {})`
- **근거 출처**: 의존 grep(0건) + verify 확인.

### J-2: 단일/다중 의존 모듈 의존 한정 — faq·inquiry·inspection·event·chatQuestion·notice

- **왜**: 각 모듈이 실제 의존하는 모듈만 `allowedDependencies`에 명시 → 신규 모듈 의존이 생기면 verify 실패(회귀 방지).
- **선언**:
  | 모듈 | allowedDependencies |
  |------|---------------------|
  | faq / inquiry / inspection | `{"user"}` |
  | event | `{"product", "user"}` |
  | chatQuestion | `{"faq", "user"}` |
  | notice | `{"notification", "user"}` |
- **검증**: verify()에서 각 모듈의 의존 모듈 집합이 허용 목록과 정확히 일치(허용 외 의존 0).
- **근거 출처**: 모듈별 import grep + verify 대조.
- **⚠️ 남은 위반(설계 의도된 잔여)**: 이 6개는 대상 모듈(user·product·faq·notification)의 **내부 타입**(예: `user.entity.User`, `user.service.query.UserQueryService`)에 직접 접근 → verify가 "depends on ... Allowed targets: ..." 또는 "non-exposed type" 위반으로 보고. **allowedDependencies는 모듈 레벨 허용만 통제**하므로 이 내부 접근은 사라지지 않는다.
- **리뷰 연습 포인트**:
  - (경계 렌즈) 이 위반을 0으로 만들려면? → 대상 모듈이 `@NamedInterface`로 사용되는 타입을 API로 노출하거나, 의존을 이벤트/공개 API로 전환(다음 단계).

## 2. 기계적 변경 (M) / 3. 생성물 (G)
- 없음.

## 다음 단계 (이 작업이 드러낸 것)
- **named interface 노출**: 가장 많이 참조되는 `user` 모듈부터 사용되는 API 타입을 `@NamedInterface`로 노출하면 다수 모듈의 내부 접근 위반이 한 번에 감소.
- SCC(10개) 순환 해소는 Phase 3(이벤트화)부터 — 이번 잠금은 SCC 밖에서 경계를 먼저 굳힌 것.
