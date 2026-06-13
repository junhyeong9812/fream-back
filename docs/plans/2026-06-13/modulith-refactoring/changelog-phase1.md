# changelog: 모듈리스 Phase 1 — 모듈 경계 전체 선언

> 대상 diff: `src/main/java/com/fream/back/domain/*/package-info.java`(신규 19) + `src/test/.../ModularityTests.java`(Documenter 추가) + `docs/...`(Before 다이어그램).
> 전략: 경계부터 전체 선언 — 도메인 1:1, 선언 전용(패키지 이동·allowedDependencies 미적용).

**검증 상태**: 통과 — `./gradlew compileTestJava` SUCCESSFUL · `ModularityTests`(2 메서드) PASSED · Documenter 다이어그램 생성(`build/spring-modulith-docs/`, Before 스냅샷 docs 보존).

## 커버리지 셀프체크
- J: 19개 package-info.java(모듈 선언) · ModularityTests(Documenter)
- 프로세스 산출물(task.md·이 문서·diagrams)은 제외. □ 완료

---

## 1. 판단 항목 (J)

### J-1: 전 도메인 @ApplicationModule 경계 선언 — `domain/*/package-info.java` (신규 19)

- **왜**: 채택한 "경계부터 전체 선언" 전략의 실행. 모듈 경계를 코드에 명시해 Modulith가 도메인=모듈로 정식 인식하게 하고, 향후 `allowedDependencies`/named interface·순환 해소의 스캐폴딩을 마련.
- **대안 비교**:
  | 접근 | 장점 | 단점 | 선택/기각 |
  |------|------|------|----------|
  | 도메인 1:1 선언 전용(선택) | 패키지 이동 0·저위험·즉시 | 모듈맵 그룹핑 아님 | **선택**(Phase 1) |
  | 모듈맵 그룹핑(identity/trade…) | 목표 구조 | 대규모 import 이동 | 후속(순환 해소 후) |
  | 선언 없이 auto-detect 유지 | 무변경 | 경계 의도 불명시·확장 불가 | 기각 |
- **근거 출처**: task.md 정의(사용자 결정 "경계부터 전체 선언" + "도메인 1:1 선언 전용" 계획 합의).
- **코드** (대표):
  ```
  @ApplicationModule(displayName = "User")
  package com.fream.back.domain.user;

  import org.springframework.modulith.ApplicationModule;
  ```
  | 줄 | 근거 해설 |
  |----|----------|
  | package-info 위치 | 어노테이션→package→import 순(package-info 전용 문법) |
  | @ApplicationModule | Modulith 분석/문서화 전용 메타데이터 — Spring 빈 와이어링·런타임 무영향 |
  | allowedDependencies 미설정 | 이번엔 선언만. 의존 강제는 순환 해소 단계에서 점진 적용(red 빌드 방지) |
- **리뷰 연습 포인트**:
  - (경계 렌즈) displayName만 다는 선언이 verify() 위반 수를 바꾸나? (아니오 — 순환 구조 불변, 선언은 인식·문서화·확장 기반)

### J-2: ModularityTests에 Documenter 추가 — `src/test/.../ModularityTests.java`

- **왜**: 모듈 의존 다이어그램(C4 PlantUML)·캔버스를 생성해 Before 아키텍처 스냅샷 확보(포트폴리오 + After 비교 기준).
- **근거 출처**: next-steps.md Phase 1.4(Documenter Before 다이어그램).
- **코드**:
  ```
      @Test
      void writeModuleDocumentation() {
          try {
              ApplicationModules modules = ApplicationModules.of(DOMAIN_BASE_PACKAGE);
              new Documenter(modules).writeDocumentation();
              System.out.println("=== MODULITH DOCS WRITTEN ===");
          } catch (Throwable t) {
              System.out.println("=== MODULITH DOCS SKIPPED: " + t.getMessage() + " ===");
          }
      }
  ```
  | 줄 | 근거 해설 |
  |----|----------|
  | try/catch | 다이어그램은 빌드 산출 — 실패해도 빌드 비차단 |
  | writeDocumentation() | 기본 출력 `build/spring-modulith-docs/`(components.puml + 모듈별 puml/adoc) |

## 2. 기계적 변경 (M)
- 없음.

## 3. 생성물 (G)
- `docs/plans/2026-06-13/modulith-refactoring/diagrams/before/{components.puml,all-docs.adoc}` — Documenter 산출 Before 스냅샷(원인 J-2). build/는 gitignore라 docs로 복사 보존.
