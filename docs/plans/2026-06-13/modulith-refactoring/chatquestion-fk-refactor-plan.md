# chatQuestion → User FK→ID 전환 — 정밀 계획 (inquiry 패턴 재사용)

> 상태: 계획만(매핑 완료), 구현은 다음 집중 세션 권장. inquiry(PR #8)에서 검증된 "FK→ID + 요약 API enrich" 패턴 재사용.
> ⚠️ chatQuestion은 테스트 없음 → **안전망(특성화 테스트) 선행 필수**. inquiry보다 표면이 넓다(엔티티 2개 + 아스펙트 role 체크 + faq query·dto 노출).

## 결합 현황 (조사 완료)
- `ChatQuestion.user` : `@ManyToOne User` (user_id NOT NULL) — 자체 `createdAt` 필드 별도 보유
- `GPTUsageLog.user` : `@ManyToOne User` (user_id nullable — null=관리자/시스템)
- `ChatService` : `saveChatQuestion(User user)` → `.user(user)`, `user.getId()`로 레포 조회(findByUserId/countByUserId/findRecent)
- `GPTUsageService` : `logGPTUsage(..., User user, ...)` → `.user(user)`, line 248 `log.getUser().getEmail()`(표시용)
- `ChatSecurityAspect:222` : `user.getRole().name()` (role 체크 — 아스펙트)
- faq 결합 : `faq.service.query.FAQQueryService`, `faq.dto.FAQResponseDto`

## touch point
| # | 파일 | 변경 |
|---|------|------|
| 1 | `faq/service/query/package-info.java` | `@NamedInterface("query")` 노출 (FAQQueryService) |
| 2 | `faq/dto/package-info.java` | `@NamedInterface("dto")` 노출 (FAQResponseDto) — 또는 chatQuestion이 faq dto 의존을 줄이도록 재설계 검토 |
| 3 | `ChatQuestion.java` | `@ManyToOne User` → `Long userId` (NOT NULL) |
| 4 | `GPTUsageLog.java` | `@ManyToOne User` → `Long userId` (nullable) |
| 5 | `ChatService.java` | `saveChatQuestion(Long userId)`, `.userId(userId)`, `user.getId()`→`userId` |
| 6 | `GPTUsageService.java` | `logGPTUsage(Long userId)`, `.userId(userId)`. line 248 표시용 email은 `UserQueryService.findUserSummary(userId)`로 enrich(또는 GPTUsageLog 조회 결과를 배치 enrich) |
| 7 | `ChatSecurityAspect.java` | `user.getRole()` → `UserQueryService.isAdmin(email)` 또는 role 조회 API. 아스펙트의 user 획득 경로 확인 필요 |
| 8 | 컨트롤러(ChatController/GPTUsageController) | User 경유 → `findUserIdByEmail`/`isAdmin` (inquiry와 동일) |
| 9 | `chatQuestion/package-info.java` | `allowedDependencies = {"faq :: query", "faq :: dto", "user :: query"}` |

## 권장 순서 (순차 커밋)
1. **안전망**: ChatService 핵심 플로우(질문 저장/조회) + GPTUsageService 로깅 특성화 테스트
2. faq named interface 노출(#1·#2) — 저위험 선행
3. 엔티티 FK→userId(#3·#4) + 컴파일 따라가며 서비스/컨트롤러(#5·#6·#8)
4. 아스펙트 role 체크 전환(#7) — 신중(횡단 관심사)
5. GPTUsageService 표시용 enrich(#6 line 248)
6. allowedDependencies(#9) + verify chatQuestion 위반 0 확인 + 특성화 테스트 통과

## 재사용 자산 (inquiry에서 확립)
- `UserQueryService.findUserSummary/findUserSummaries/findUserIdByEmail/isAdmin` (이미 존재)
- 패턴: 리포지토리 userId 조회 → 서비스 배치 enrich, 컨트롤러 findUserIdByEmail/isAdmin

## 주의
- `ChatSecurityAspect`의 role 체크는 아스펙트(횡단)라 inquiry엔 없던 케이스 — user 획득 방식 먼저 확인.
- `GPTUsageLog.user`가 nullable(시스템 로그) → enrich/표시 시 null 처리.
