# 08. 의사결정 · 문제 로그 (통합)

> 이 리팩토링에서 **발견한 문제**와 **내린 선택(의사결정)**을 한 곳에 모은 추적 로그. 상세는 각 참조 문서/이슈/PR.

## A. 발견한 문제

| # | 문제 | 성격 | 처리/문서 |
|---|------|------|----------|
| P1 | 커밋된 main이 **컴파일 불가**(패키지 선언≠위치 3클러스터, 죽은 import, getter 오타, 람다 final, 인증 와이어링) | 빌드 차단 | 수정(PR #1) / `05-refactoring-log` |
| P2 | **기동 차단**: User MySQL 전용 DDL(H2 불가), PaymentRepository JPQL `createdAt` 오타 | 기동 차단 | H2 MySQL모드·JPQL 수정(PR #1) |
| P3 | CI/CD 전체 주석 + Dockerfile 베이스(openjdk) **삭제됨** | CI/CD 실패 위험 | temurin 교체·CI 간소화(PR #3) |
| P4 | 20 도메인 **순환 12쌍 → 10모듈 SCC**(verify 100 cycle) | 결합 | Modulith 도입·경계 선언(PR #2), 단계 해소 중 |
| P5 | `InquirySearchResultDto` 생성자 투영 불일치 → searchInquiries 런타임 실패 | 잠재버그 | 13인자 생성자(PR #6) |
| P6 | inquiry·chatQuestion이 **User 엔티티 FK 직접 참조** | 결합 | FK→ID + 요약 API enrich(PR #8·#10) |
| P7 | **bid/order 결합**: OrderBid·SaleBid가 Order·Sale 교차 @OneToOne → order↔sale 순환, 입찰/거래 개념 혼재 | 도메인 모델 | 재설계(이슈 #12, `06`·`07`), 구현 중 |
| P8 | global 18곳 domain 역참조(security↔user), 공유커널 오염 | 결합 | 이슈 #16 |
| P9 | 안전망(테스트) 거의 없음, full boot 미검증 | 테스트 | 슬라이스 특성화 테스트 + 이슈 #17 |
| P10 | `_backup` 잔재 빈 4개 | 죽은 코드 | 이슈/정리 백로그 |

> 남은 문제는 이슈 #12~#17로 추적.

## B. 의사결정 로그 (사용자 선택 + 근거)

| 시점 | 결정 사항 | 선택 | 근거/문서 |
|------|----------|------|----------|
| 분석 후 | 목표 아키텍처 | **Spring Modulith**(단일 배포) | MSA 과도, 모듈리스가 목표에 부합 / `task.md`, `02` |
| 〃 | 전환 전략 | **경계부터 전체 선언** → 위반 가시화 → 순환 해소 | verify로 위반 ground truth / `task.md §4` |
| 〃 | 산출물 | 분석+계획 문서 | / `task.md` |
| main 빌드실패 | 처리 | **컴파일 에러 전수 조사 먼저** | 정적 import 스캔 / `task.md §8` |
| Phase0 후 | 다음 | **안전망 테스트 구축** | 회귀 그물 / `task.md` |
| 안전망 후 | 다음 | **Phase1 경계 전체 선언** | / `changelog-phase1` |
| Phase1 후 | 다음 | **비순환 모듈 allowedDependencies 잠금** | / `changelog-allowed-deps` |
| CI 이슈 | CI/CD 범위 | **CI + Docker 이미지 빌드**(push·배포 제거) | secrets·실서버 의존 취약 / `changelog-ci` |
| 모듈 입도 | 입도 | **도메인 1:1, 선언 전용**(물리 이동 X) | 저위험·점진 / `task.md §9` |
| inquiry | 진행 | **안전망 먼저 → FK→ID** | 동작 보존 / `inquiry-fk-refactor-plan`, `changelog-inquiry-fk` |
| chatQuestion | 진행 | **동일 FK→ID 패턴** | / `chatquestion-fk-refactor-plan` |
| bid/order | 접근 | **도메인 개념 정리 후 설계**(기계적 패치 X) | DDD / `06`·`07` |
| trade ① | Bid 개념 | **BUY/SELL 통합 단일 Bid** | 매칭 로직 일원화 / `07` |
| trade ② | Order/Sale | **별개 유지**(직접참조 X, matchId 연결) | / `07` |
| trade ③ | 모듈 경계 | **전용 trade(matching) 모듈 신설** | 입찰/거래 분리 / `07` |
| trade 확정 | 데이터 | **기존 입찰 데이터 없음** → 마이그레이션 불필요 | 사이드 프로젝트 / `07 §6` |
| 〃 | 수량 | **수량 개념 존재** + 즉시구매(올려둔 SELL 즉시 체결) | / `07 §6` |
| 〃 | matchId·동시성 | **UUID + 동시성 처리**(CAS·낙관락) | 중복 체결 방지 / `07 §7` |
| git | 워크플로우 | **작업 브랜치 + PR**, 머지 후 분기 | core §6.5 / 전 PR |
| git | 커밋/PR 언어 | **전부 한국어**, AI trailer 미기재 | 사용자 지시 / memory |

## C. 산출물 위치 (이 모든 게 어디에 있나)
- **이 가이드 세트**: `docs/refactor/01~08` + `README`(index)
- **작업 로그·계획·검증**: `docs/plans/2026-06-13/modulith-refactoring/` — task.md, changelog-*, next-steps, *-plan, OVERVIEW/TECHNICAL/learned/review-log, codex 입출력, verify 리포트, diagrams
- **남은 작업 추적**: GitHub 이슈 #12~#17
- **변경 이력**: PR #1~#20(+) — 각 PR 본문에 변경·검증·다음 단계
