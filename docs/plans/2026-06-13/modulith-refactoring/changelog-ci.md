# changelog: CI/CD 간소화 (실패 방지)

> 대상 diff: `.github/workflows/ci-cd.yml`(주석→활성 CI), `docker/Dockerfile`(베이스 이미지), `.dockerignore`(신규).
> 배경: 기존 `ci-cd.yml`이 전체 주석(빌드 깨짐으로 비활성 추정). 빌드가 그린이 된 후 "실패하지 않는" CI/CD로 간소화 요청.

**검증 상태**: 통과(로컬) — `./gradlew clean build -x test` ✅ · 안전 슬라이스 테스트 3건 PASSED ✅ · `docker build -f docker/Dockerfile` ✅(Temurin 베이스로 성공). GitHub Actions 실행은 머지 후 확인.

## 커버리지 셀프체크
- J: ci-cd.yml · docker/Dockerfile · .dockerignore
- □ 완료

---

## 1. 판단 항목 (J)

### J-1: Dockerfile 베이스 이미지 교체 — `docker/Dockerfile:2`

- **왜**: `openjdk:17-jdk-slim`이 Docker Hub에서 **제거됨**(openjdk 공식 이미지 deprecated) → `manifest unknown`으로 docker build 실패. CD 활성화 시 깨지는 핵심 원인.
- **대안 비교**:
  | 접근 | 장점 | 단점 | 선택/기각 |
  |------|------|------|----------|
  | eclipse-temurin:17-jdk-jammy (선택) | openjdk 공식 후속·안정·jdk 유지 | jre보다 큼 | **선택** |
  | eclipse-temurin:17-jre-jammy | 더 작음 | jdk→jre 런타임 변경 | 기각(최소 변경 우선) |
  | amazoncorretto:17 | AWS 친화 | 변경폭 큼 | 기각 |
- **근거 출처**: docker build 로컬 실패 로그(`manifest for openjdk:17-jdk-slim not found`) → Temurin으로 교체 후 build 성공 확인.
- **코드**:
  ```
  FROM eclipse-temurin:17-jdk-jammy
  ```

### J-2: CI 워크플로우 활성화·간소화 — `.github/workflows/ci-cd.yml`

- **왜**: 전체 주석 상태를 "실패하지 않는" 활성 CI로 전환. 사용자 결정 = CI(빌드+안전 테스트)+Docker build, **GHCR push·EC2 배포 제거**(secrets·실서버 의존으로 취약).
- **핵심 결정**:
  | 결정 | 근거 |
  |------|------|
  | `build -x test` + 안전 테스트만 별도 실행 | `./gradlew test` 전체는 `FreamBackApplicationTests`(@SpringBootTest)가 인프라(Redis/Kafka/ES) 필요로 실패 → 인프라 불필요 슬라이스(Modularity/Persistence/특성화)만 |
  | Docker build (push 없음) | 이미지 빌드 유효성만 검증, GHCR_TOKEN 불필요 |
  | 배포 job 제거(주석 참조 보존) | EC2 SSH·secrets·실서버 의존 → 실패 방지. 재활성화 방법은 파일 하단 주석 |
  | actions v4 + `cache: gradle` | 최신 액션·빌드 캐시로 속도 |
- **근거 출처**: 사용자 결정("CI + Docker 이미지 빌드") + 로컬 검증.
- **리뷰 연습 포인트**:
  - (운영 렌즈) `--tests` 인클루전 목록은 새 안전 테스트 추가 시 갱신 필요 — 누락 시 CI 미실행(조용한 커버리지 축소) 위험.

### J-3: .dockerignore 신규 — `.dockerignore`

- **왜**: 빌드 컨텍스트 518MB(.git·logs·docs 등 포함) 축소. Dockerfile은 `build/libs/*.jar`만 COPY하므로 무관 디렉터리 제외.
- **주의**: `build/`는 제외하지 않음(COPY 대상 jar 보존). `.git`·`.gradle`·`logs`·`docs`·`.github`·`src/test`·`*.md` 제외.

## 2. 기계적 변경 (M)
- 없음.

## 3. 생성물 (G)
- 없음.
