# fream-back 리팩토링 가이드 (index)

> fream-back을 **모듈리스(Spring Modulith) + 이벤트/ID 기반 디커플링** 구조로 개선하는 작업의 통합 진입점.
> 작성: 2026-06. 상세 작업 로그는 `docs/plans/2026-06-13/modulith-refactoring/` 참조.

## 이 문서 세트

| 문서 | 내용 |
|------|------|
| [01. 프로젝트 개요](01-project-overview.md) | 무엇을 하는 서비스인가, 기술 스택, 전체 구조 |
| [02. 아키텍처](02-architecture.md) | 모듈리스 구조, 모듈 맵, 모듈 간 통신, 경계 강제 현황, 적용 패턴 |
| [03. 도메인 설명](03-domains.md) | 19개 도메인 각각의 역할·핵심 엔티티·결합 상태 |
| [04. 핵심 플로우](04-flows.md) | 주문/결제, 입찰 매칭, 문의, 챗봇 등 주요 흐름 |
| [05. 리팩토링 로그](05-refactoring-log.md) | 이번에 구현한 내용(PR #1~#10)과 확립한 패턴 |
| [06. bid/order 모델 이슈](06-bid-order-issue.md) | 입찰-주문 결합 문제 분석 + 재설계 권고 |

## 현재 상태 요약 (2026-06 기준)

- ✅ 빌드·기동 **그린**, CI 활성·통과 (이전엔 빌드·기동 불가였음 — 잠재버그 9건 수정)
- ✅ Spring Modulith 도입, 19개 도메인에 모듈 경계 선언
- ✅ **완전 clean 모듈 7개**: accessLog · monitoring · weather · faq · inspection · inquiry · chatQuestion
- 🟡 진행 중: 거래 코어(order·payment·sale·shipment·warehouseStorage) SCC + product·user·style·notification·address
- 📌 알려진 모델 이슈: **bid/order 결합**(06 문서)

## 핵심 원칙

1. **단일 배포 유지(모듈리스)** — 물리 분리/MSA 아님.
2. **모듈 경계는 코드로 강제** — `@ApplicationModule` + `verify()` + CI.
3. **모듈 간 통신**: 엔티티 직접 참조 금지 → **ID 참조 + 공개 API(@NamedInterface) enrich** 또는 **이벤트**.
4. **안전망 위에서 리팩토링** — 변경 전 특성화 테스트.
