# Inquiry → User FK→ID 전환 — 잔여 정밀 계획

> 상태: **토대 완료**(user 배치 조회 API), 본 전환은 다음 단계. 안전망: `InquirySearchCharacterizationTest`(검색 투영). ⚠️ 커맨드 플로우(생성/수정/답변)는 테스트 없음 → 전환 전 특성화 테스트 추가 권장.

## 완료된 토대 (c57e564)
- `UserSummary(record)` + `UserQueryService.findUserSummaries(ids)`/`findUserSummary(id)` (fetch join, named interface "query" 포함)

## 목표
`Inquiry.user`(@ManyToOne User)를 `Long userId`로 전환하고, inquiry가 user 엔티티/리포지토리를 직접 참조하지 않게 한다. DB 컬럼 `user_id`는 유지(매핑만 변경, 마이그레이션 불필요).

## touch point (매핑 완료)
| # | 파일 | 변경 |
|---|------|------|
| 1 | `inquiry/entity/Inquiry.java` | `@ManyToOne User user` → `@Column(name="user_id") Long userId`. User import 제거 |
| 2 | `inquiry/dto/InquiryCreateRequestDto.java` | `toEntity(User)` → `toEntity(Long userId)`. User import 제거 |
| 3 | `inquiry/dto/InquiryResponseDto.java` | `from(Inquiry, images)`·`from(Inquiry)`에서 `inquiry.getUser().*` 제거 → `userId`만 채우고, email·profileName·name은 `UserSummary` 파라미터로 받도록 시그니처 변경(`from(Inquiry, images, UserSummary)`) |
| 4 | `inquiry/dto/InquirySearchResultDto.java` | 이미 13인자 생성자 보유. 단 user 필드(email·profileName·name)를 **투영 후 enrich**하려면 setter/wither 추가 필요(현재 생성자로만 채움) |
| 5 | `inquiry/repository/InquiryRepositoryImpl.java` | 4개 쿼리에서 **QUser 조인 제거**. `inquiry.user.id.eq(...)` → `inquiry.userId.eq(...)`. 투영은 userId까지만, email/profile은 서비스 enrich. `leftJoin(inquiry.user, user)`·`leftJoin(user.profile)` 삭제 |
| 6 | `inquiry/service/query/InquiryQueryServiceImpl.java` | 검색 결과의 userId들을 모아 `userQueryService.findUserSummaries(ids)`로 **배치 enrich**. `userRepository`(line 80 findById) → `UserQueryService` 또는 제거 |
| 7 | `inquiry/service/command/InquiryCommandServiceImpl.java` | `userRepository.findById` 제거(생성 시 userId 직접 사용). 소유권 체크 `inquiry.getUser().getId()` → `inquiry.getUserId()`. 응답: `userQueryService.findUserSummary(inquiry.getUserId())`로 enrich. 의존 `UserRepository` → `UserQueryService` |
| 8 | `inquiry/controller/{command,query}/*` | 이미 `UserQueryService` 주입 중. User import 제거 점검 |
| 9 | `inquiry/package-info.java` | `allowedDependencies = {"user :: query"}` (entity/repository 비참조 달성 후) |

## 권장 순서 (순차 커밋)
1. **커맨드 플로우 특성화 테스트 추가**(생성/수정/답변 — 현재 무테스트) — 안전망
2. Inquiry 엔티티 FK→userId (#1) + 컴파일 에러 따라가며 #2·#3 시그니처 변경
3. Repository 쿼리에서 QUser 제거 + 서비스 배치 enrich (#5·#6)
4. Command 응답 enrich (#7)
5. allowedDependencies `{"user :: query"}` (#9) + verify inquiry 위반 0 확인 + 두 특성화 테스트 통과

## 검증
- `InquirySearchCharacterizationTest`: 검색 결과의 userId·email·profileName·name이 동일하게 채워지는지(enrich 경로로 바뀌어도 값 보존)
- 커맨드 특성화 테스트: 생성/답변 응답의 user 정보 보존
- `ModularityTests`: inquiry user 접근 위반 0건

## 주의 (성능)
검색은 `leftJoin(user.profile)` 한 번이던 것이 "inquiry 조회 + userId 배치 조회" 2쿼리로 바뀐다(N+1 아님, 배치 1회). 페이지 단위라 허용 범위.
