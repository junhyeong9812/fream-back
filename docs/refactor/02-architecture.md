# 02. 아키텍처 (모듈리스)

## 모듈 정의
- 모듈 base 패키지 = `com.fream.back.domain`. 직속 하위 패키지(user, order, …) 각각이 **하나의 application module**.
- 각 모듈 루트에 `package-info.java` + `@ApplicationModule(displayName, allowedDependencies)` 선언.
- `global`은 모듈 base 밖이라 모듈 시스템이 추적하지 않는 공유 영역(BaseEntity·응답/에러·보안·util).

## 경계 강제 메커니즘
- `ModularityTests`(`src/test`)가 `ApplicationModules.of("com.fream.back.domain")`로 모듈을 분석.
  - `verify()`: 순환·내부 타입 접근 등 경계 위반 탐지(정적 분석, 스프링 컨텍스트 불필요).
  - `Documenter`: 모듈 의존 다이어그램(C4 PlantUML) 생성 → `docs/plans/.../diagrams/before/components.puml`.
- 위반은 빌드를 깨지 않고 캡처(리팩토링 진행 중이므로). 위반 수를 0으로 수렴시키는 게 목표.

## allowedDependencies — 의존 명시
- `@ApplicationModule(allowedDependencies = {...})`로 그 모듈이 의존 가능한 대상을 화이트리스트.
- 형식: `"모듈명"`(그 모듈의 default API=base 패키지) 또는 `"모듈명 :: 인터페이스명"`(특정 @NamedInterface).
- 무의존 모듈은 `allowedDependencies = {}` (의존 금지 강제).

## @NamedInterface — 모듈 공개 API
- 기본적으로 모듈의 base 패키지 타입만 "공개", 하위 패키지(entity·service·repository 등)는 "내부".
- 타 모듈이 써야 하는 패키지를 `@NamedInterface("이름")`로 노출.
- 예: `user.service.query`(=`user :: query`)에 `UserQueryService`·`UserSummary` 노출. `faq.service.query`(`faq :: query`), `faq.dto`(`faq :: dto`).

## 모듈 간 통신 패턴 (적용 원칙)
1. **상태 통지(응답 불필요)** → ApplicationEvent + `@ApplicationModuleListener`(트랜잭션 커밋 후 비동기). (예정: notification)
2. **즉시 조회(응답 필요)** → 대상 모듈의 공개 API(@NamedInterface) 호출. **엔티티 직접 참조 금지**.
3. **엔티티 FK 대신 ID 참조** → 필요한 표시 데이터는 **요약 API로 enrich**(배치로 N+1 회피).
4. **진짜 비동기/내구성** → 기존 Kafka 유지(조회수·액세스로그 등).

### 확립된 "FK→ID + enrich" 패턴 (inquiry·chatQuestion에 적용)
```
[이전] 모듈A 엔티티 → 모듈B 엔티티 (@ManyToOne, 조인)
[이후] 모듈A 엔티티 → Long bId (FK 컬럼 유지, JPA 매핑만 ID로)
       리포지토리: 조인 제거, bId만 조회
       서비스: 모듈B 공개 요약 API로 표시 데이터 enrich
              - 목록: findSummaries(ids) 배치 1회
              - 단건: findSummary(id)
       컨트롤러: 엔티티 대신 findIdByEmail / isAdmin 등 ID/판정 API
```
재사용 자산: `UserQueryService.findUserSummary/findUserSummaries/findUserIdByEmail/isAdmin/getRoleName`, `UserSummary(record)`.

## 경계 강제 현황 (2026-06)
| 구분 | 모듈 | 상태 |
|------|------|------|
| 무의존 잠금(`{}`) | accessLog · monitoring · weather | ✅ clean |
| 단일/다중 의존 + named interface | faq · inspection · inquiry · chatQuestion | ✅ clean |
| 의존 선언만(내부접근 위반 잔존) | event · notice | 🟡 대상 모듈 named interface 노출 필요 |
| SCC(거대 순환) | order · payment · sale · shipment · warehouseStorage · product · user · style · notification · address | 🔴 이벤트화/FK→ID 진행 대상 |

## 테스트 인프라
- `@DataJpaTest` + H2 `MODE=MySQL`(엔티티의 MySQL 전용 DDL 수용) + `@AutoConfigureTestDatabase(replace=NONE)` + `@Import(QueryDslConfig)`.
- 슬라이스 특성화 테스트로 리팩토링 회귀 감지. `FreamBackApplicationTests`(full context)는 외부 인프라 필요로 CI에서 제외.

## CI/CD
- `.github/workflows/ci-cd.yml`: 빌드(`-x test`) + 인프라 불필요 슬라이스 테스트 + Docker 이미지 빌드(푸시/배포 없음).
- Dockerfile 베이스: `eclipse-temurin:17-jdk-jammy`(제거된 openjdk 이미지 대체).
- 배포(EC2 SSH)는 secrets·실서버 의존으로 제거(재활성화 주석 보존).
