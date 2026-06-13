# review-log: 모듈리스 Phase 0 — 그린 빌드 + Spring Modulith 도입

## 루프 메타

- packet base SHA: `HEAD..(working tree)` — 미커밋 작업트리 diff(소스만, logs/gradlew 제외)
- 입력 격리: codex 임시 디렉터리 packet-only ☑ (review-packet.diff + 신규 테스트 3종, `codex exec --ephemeral -s read-only`)
- 리뷰 형태: **codex 1회 (중간 stakes)** — 위치: 최종 검증(가장 불확실한 지점 = SecurityConfig 인증 와이어링)
- 보안 스캔(외부 전송 게이트): secret/PII 패턴 매칭 **0건** → 자동 통과
- 종료 조건: open(채택·미수정)=0 ☑ AND 이번 루프 신규 채택=0 ☑ — **codex 고확신 결함 0건**

## finding ledger

| id | loop | source | file:line | 요지 (1줄) | disposition | 채택/기각 근거 | status | fixed_in_loop |
|----|------|--------|-----------|-----------|-------------|--------------|--------|---------------|
| — | 1 | codex | — | 고확신 정확성 결함 없음 | — | — | — | — |

## 검토 메모 (codex가 확인해준 것 — finding 아님)

- **SecurityConfig 인증 와이어링** (`SecurityConfig.java:35,42`): AuthService 주입 + `new LoginAuthenticationFilter(authService)`는 순환참조 아님. AuthService 의존(UserRepository·PasswordEncoder·JwtTokenProvider)은 SecurityConfig 밖, PasswordEncoder는 EncoderConfig 제공. 필터는 이미 `authService.login()` 호출 → 의도된 주입 복구.
- **필드명 수정**: `getModifiedDate()`·`p.createdDate`는 BaseTimeEntity 실제 필드와 일치. 람다 `final int index`는 정석 수정.
- **H2 MODE=MySQL**: 적절한 우회. 단 MySQL 완전 동등 아님 → 운영 동등성은 별도 MySQL/Testcontainers 필요(잔여 리스크로 기록).
- **파일 이동·Modulith 버전**: 참조 일치, 1.3.12↔Boot 3.4.1 호환(공식 appendix 확인).

## 잔여 리스크 / 사용자 결정 필요

- **full boot 미검증**: codex도 read-only sandbox라 직접 빌드/기동은 미수행. 인증 와이어링의 런타임 기동(SecurityFilterChain 구성)은 인프라(Redis/Kafka/ES) 확보 시 별도 검증 필요.
- **H2≠MySQL 동등성**: 테스트가 MySQL 특이 동작을 못 잡을 수 있음 → Testcontainers MySQL 도입 검토(후속 과제).
