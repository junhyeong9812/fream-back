# 01. 프로젝트 개요

## 무엇을 하는 서비스인가
fream-back은 **한정판 상품 거래 플랫폼**(KREAM류) 백엔드다. 구매자와 판매자가 **입찰(bid)**로 가격을 제시하고, 매칭되면 **주문(order)/판매(sale)**가 성사된다. 검수·창고보관·배송·결제·정산, SNS 스타일 피드, 고객지원(문의·FAQ·챗봇), 알림, 접근로그/모니터링 등을 포함한다.

## 기술 스택
- Spring Boot 3.4.1 / Java 17
- JPA(Hibernate) + QueryDSL, MySQL(운영) / H2(테스트, MySQL 호환 모드)
- Redis, Kafka, Elasticsearch, Spring Security(JWT), Spring Batch
- **Spring Modulith 1.3.x** (모듈 경계 검증/문서화 — 이번 리팩토링에서 도입)
- 빌드: Gradle. CI: GitHub Actions(빌드 + 슬라이스 테스트 + Docker 이미지 빌드)

## 전체 구조
```
com.fream.back
├── FreamBackApplication        (진입점)
├── domain/                     (19개 도메인 = 모듈 단위)
│   ├── user, address           → identity(계정/프로필/인증 주체)
│   ├── product (+elasticsearch)→ catalog(상품/검색)
│   ├── order, payment, sale,
│   │   shipment, warehouseStorage → trade(거래 라이프사이클)
│   ├── style                   → feed(SNS)
│   ├── faq, inquiry, notice,
│   │   chatQuestion, inspection → support(CS)
│   ├── notification            → 알림(이벤트 sink)
│   └── accessLog, monitoring,
│       weather, event          → platform(부가/인프라/프로모션)
└── global/                     (공유: BaseEntity, 응답/에러, 보안, util, config)
```

- 각 도메인은 내부적으로 `controller(command/query)`, `service(command/query)`, `repository`, `entity`, `dto`, `exception`, `aop` 로 구성(도메인별 CQRS형 분리).
- `domain.<x>` 직속 패키지 = Spring Modulith의 application module 1개.

## 도메인 그룹(목표 바운디드 컨텍스트)
실제 패키지는 도메인 1:1 모듈이지만, 논리적 그룹은 위 트리의 화살표(→) 오른쪽과 같다. 그룹 단위 물리 통합은 순환 해소 이후 검토(현재는 도메인 1:1 유지).

## 더 보기
- 모듈 구조·경계: [02. 아키텍처](02-architecture.md)
- 도메인별 상세: [03. 도메인 설명](03-domains.md)
