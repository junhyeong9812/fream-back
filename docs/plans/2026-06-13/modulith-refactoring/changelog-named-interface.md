# changelog: user 조회 API named interface 노출

> 대상 diff: `user/service/query/package-info.java`(신규 @NamedInterface), `faq`·`inspection` package-info(allowedDependencies 조정).
> 목적: 모듈 간 내부 타입 접근 위반을, 대상 모듈이 공개 API를 named interface로 노출하고 소비자가 그 인터페이스만 의존하도록 전환해 해소.

**검증 상태**: 통과 — `ModularityTests` PASSED, compileJava 그린. **faq·inspection 모듈 경계 위반 0건(완전 clean)** 확인.

## 1. 판단 항목 (J)

### J-1: user.service.query 를 named interface로 노출 — `user/service/query/package-info.java`

- **왜**: 타 모듈은 user의 entity/repository(내부)가 아니라 조회 서비스(UserQueryService)를 통해 사용자 정보를 얻어야 한다. 이 패키지를 공개 API로 선언.
- **코드**:
  ```
  @NamedInterface("query")
  package com.fream.back.domain.user.service.query;

  import org.springframework.modulith.NamedInterface;
  ```
- **근거 출처**: 비순환 소비자 import 분석(faq·inspection·event·notice가 user.service.query 사용).

### J-2: faq·inspection 의존을 named interface로 좁힘 — `faq`·`inspection` package-info

- **왜**: `allowedDependencies = {"user"}`는 user의 **default 인터페이스(base 패키지)만** 허용 → named interface 접근은 여전히 위반. named interface를 쓰려면 `{"user :: query"}`로 명시해야 함.
- **핵심 발견(학습)**: `"모듈명"` vs `"모듈명 :: 인터페이스명"` 구분 — 전자는 default 인터페이스, 후자는 특정 named interface. 대상이 named interface를 노출하면 소비자도 그 형식으로 참조해야 한다.
- **코드**: `allowedDependencies = {"user :: query"}`
- **결과**: faq·inspection은 user.service.query만 사용 → 위반 0건(완전 clean).

## 2. 기계적 변경 (M) / 3. 생성물 (G)
- 없음.

## 남은 일 (이 증분이 드러낸 것)
- **event**: user::query + **product 내부**(Brand 엔티티·BrandQueryService) 사용 → product가 brand API를 노출하거나 event가 product 의존을 줄여야 clean.
- **notice**: user::query + **notification 내부**(NotificationCommandService 등) → notification은 **이벤트화(Phase 3)**로 결합 제거가 더 적합.
- **inquiry**: user.repository 직접 접근(스멜) → UserQueryService 사용으로 리팩토링 필요.
- **chatQuestion**: faq.service.query(→faq도 named interface 노출 가능) + user.entity(리팩토링 필요).
- **공통**: entity/repository 직접 접근은 노출(결합 승인)보다 **소비자를 조회 API/DTO로 이전**하거나 **이벤트화**가 정도(正道).
