# changelog: Inquiry→User FK→ID 전환 (첫 🔴 엔티티 결합 해소)

> 대상: inquiry 모듈 전반(엔티티·3 DTO·리포지토리·2 서비스·2 컨트롤러) + UserQueryService 보강. 토대: PR #7(UserSummary API).

**검증 상태**: 통과 — 안전 슬라이스 테스트 8건 PASSED, `InquirySearchCharacterizationTest`로 작성자 정보 보존 확인, **ModularityTests에서 inquiry 모듈 위반 0건(완전 clean)**.

## 핵심 변경 (J)
| # | 파일 | 변경 |
|---|------|------|
| J-1 | `Inquiry.java` | `@ManyToOne User user` → `@Column(name="user_id") Long userId`. DB 컬럼 유지 → 마이그레이션 불필요 |
| J-2 | `InquiryRepositoryImpl.java` | 4개 조회의 `QUser` 조인 제거, `searchResultProjection`으로 userId만 투영. `inquiry.user.id` → `inquiry.userId` |
| J-3 | `InquiryQueryServiceImpl.java` | `enrichAuthors(Page)`: 검색 결과 userId 수집 → `UserQueryService.findUserSummaries` 배치 1회로 작성자 enrich(N+1 회피). getInquiry는 `findUserSummary` 단건 |
| J-4 | `InquiryCommandServiceImpl.java` | `userRepository.findById` 제거, `toEntity(userId)`. 응답은 `findUserSummary`로 enrich. 소유권 체크 `getUser().getId()` → `getUserId()` |
| J-5 | `InquiryResponseDto`/`InquirySearchResultDto` | `from`에 `UserSummary` 주입(또는 `applyAuthor`로 enrich). user 엔티티 직접 접근 제거 |
| J-6 | 컨트롤러 2개 | `findByEmail(email).getId()`(User 경유) → `UserQueryService.findUserIdByEmail/isAdmin` |
| J-7 | `UserQueryService` | `findUserIdByEmail`·`isAdmin` 추가(엔티티 직접 참조 대체용 read API) |
| J-8 | `inquiry/package-info.java` | `allowedDependencies = {"user :: query"}` |

## 결과
- inquiry가 user의 **엔티티·리포지토리를 전혀 참조하지 않음** → user::query(노출 API)만 의존 → verify 위반 0.
- clean 모듈: accessLog·monitoring·weather·faq·inspection + **inquiry**(6개).
- 패턴 확립: 엔티티 FK → ID참조 + 대상 모듈 요약 API enrich. 다른 🔴 결합(chatQuestion, 거래코어)에 재사용 가능.

## 주의
- 검색 경로가 "조인 1쿼리" → "inquiry 조회 + 작성자 배치 조회(2쿼리)"로 바뀜(N+1 아님). 페이지 단위라 허용.
- 권한 체크가 endpoint당 findByEmail 1회 → findUserIdByEmail+isAdmin 2회. 필요 시 결합 메서드로 최적화 가능(후속).
