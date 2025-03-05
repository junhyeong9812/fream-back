# 접근 로그 시스템 (Access Log System)

## 개요

접근 로그 시스템은 애플리케이션과 사용자 상호작용을 추적하고 분석하기 위해 설계된 종합적인 모듈입니다. 이 시스템은 디바이스 정보, 위치 데이터, 사용자 행동 패턴을 포함한 각 방문에 대한 상세 정보를 캡처합니다. 데이터는 애플리케이션 성능에 미치는 영향을 최소화하기 위해 Kafka를 사용하여 비동기적으로 처리됩니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.accessLog/
├── controller/
│   ├── command/          # 로그 항목 생성을 위한 컨트롤러
│   └── query/            # 분석 데이터 조회를 위한 컨트롤러
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티
├── repository/           # 데이터 접근 계층
└── service/
    ├── command/          # 로그 생성을 위한 서비스
    ├── geo/              # 지리적 위치 서비스
    ├── kafka/            # Kafka 프로듀서 및 컨슈머
    └── query/            # 분석 쿼리를 위한 서비스
```

## 주요 구성 요소

### 데이터 수집

1. **UserAccessLogCommandController**: 로그 생성 요청을 받고 HTTP 요청 정보를 추출
2. **UserAccessLogCommandService**: 로그 데이터를 처리하고 Kafka로 전송
3. **UserAccessLogProducer**: 로그 데이터를 이벤트로 패키징하여 Kafka에 발행

### 데이터 처리

1. **UserAccessLogConsumer**: 로그 이벤트를 수신하고, 지리적 위치 데이터로 강화한 후 데이터베이스에 저장
2. **GeoIPService**: MaxMind GeoLite2 데이터베이스를 사용하여 IP 주소를 지리적 위치로 변환

### 데이터 분석

1. **UserAccessLogQueryController**: 분석 데이터를 위한 엔드포인트 제공
2. **UserAccessLogQueryService**: 접근 분석을 위한 비즈니스 로직
3. **UserAccessLogRepository**: 사용자 정의 쿼리 메서드가 포함된 데이터베이스 접근

## 수집되는 데이터

각 접근 로그 항목에는 다음 정보가 포함됩니다:

- **기본 HTTP 데이터**: 참조 URL(Referer), 사용자 에이전트(User-Agent), 페이지 URL
- **디바이스 정보**: OS, 브라우저, 디바이스 유형, 화면 크기, 픽셀 비율
- **네트워크 데이터**: IP 주소, 네트워크 유형
- **사용자 데이터**: 이메일(또는 익명), 브라우저 언어
- **지리적 데이터**: 국가, 지역, 도시(IP 주소에서 추출)
- **타임스탬프**: 접근 시간

## 엔드포인트

### Command API

```
POST /access-log/commands/create
```
새 접근 로그 항목을 생성합니다. 이 엔드포인트는 IP, 사용자 에이전트, 참조자에 대한 HTTP 헤더를 자동으로 추출합니다.

**요청 본문 예시:**
```json
{
  "pageUrl": "/products",
  "email": "user@example.com",
  "deviceType": "desktop",
  "networkType": "wifi",
  "browserLanguage": "ko-KR",
  "screenWidth": 1920,
  "screenHeight": 1080,
  "devicePixelRatio": 1.5
}
```

### Query API

```
GET /access-log/queries/today
```
현재 날짜의 고유 방문자 수를 반환합니다.

```
GET /access-log/queries/week
```
지난 7일간의 일별 방문자 수를 반환합니다.

**응답 예시:**
```json
[
  { "dateString": "2025-02-25", "count": 152 },
  { "dateString": "2025-02-26", "count": 189 },
  { "dateString": "2025-02-27", "count": 201 },
  { "dateString": "2025-02-28", "count": 175 },
  { "dateString": "2025-03-01", "count": 110 },
  { "dateString": "2025-03-02", "count": 105 },
  { "dateString": "2025-03-03", "count": 198 }
]
```

## 비동기 처리

이 시스템은 Kafka를 사용하여 로그 생성과 처리를 분리합니다:

1. **프로듀서 측**: 로그 데이터는 즉시 Kafka로 전송됩니다
2. **컨슈머 측**: 로그 이벤트가 소비되고, 지리적 데이터로 보강된 후 데이터베이스에 저장됩니다

이러한 아키텍처는 다음을 보장합니다:
- 로그 처리로 인해 사용자 경험이 영향을 받지 않음
- 시스템이 높은 트래픽 볼륨을 처리할 수 있음
- 로그 처리를 독립적으로 확장할 수 있음

## IP 지리적 위치

이 시스템은 MaxMind의 GeoLite2 데이터베이스를 사용하여 IP 주소를 지리적 위치로 변환합니다. 데이터베이스 파일은 리소스 디렉토리에 애플리케이션과 함께 패키징되어 있습니다.

## 데이터베이스 스키마

`user_access_log` 테이블에는 다음이 포함됩니다:

| 열                  | 타입             | 설명                             |
|--------------------|-----------------|-----------------------------------|
| id                 | BIGINT          | 기본 키                           |
| access_time        | TIMESTAMP       | 접근 발생 시간                     |
| browser            | VARCHAR         | 브라우저 정보                      |
| browser_language   | VARCHAR         | 사용자 브라우저 언어                |
| city               | VARCHAR         | 도시 (IP에서 추출)                 |
| country            | VARCHAR         | 국가 (IP에서 추출)                 |
| device_pixel_ratio | FLOAT           | 디바이스 픽셀 비율                  |
| device_type        | VARCHAR         | 모바일, 태블릿, 데스크톱 등         |
| email              | VARCHAR         | 사용자 이메일 또는 "Anonymous"      |
| ip_address         | VARCHAR         | 사용자 IP 주소                     |
| is_anonymous       | BOOLEAN         | 사용자가 익명인지 여부              |
| network_type       | VARCHAR         | 연결 유형 (wifi, 4G 등)            |
| os                 | VARCHAR         | 운영 체제                          |
| page_url           | VARCHAR         | 접근한 페이지 URL                  |
| referer_url        | VARCHAR         | 사용자가 어디서 왔는지               |
| region             | VARCHAR         | 지역/도 (IP에서 추출)               |
| screen_height      | INT             | 화면 높이(픽셀)                     |
| screen_width       | INT             | 화면 너비(픽셀)                     |
| user_agent         | VARCHAR         | 전체 사용자 에이전트 문자열           |

## 구현 참고사항

### 개발자를 위한 안내

1. **새 로그 속성 추가**:
    - `UserAccessLogDto`, `UserAccessLogEvent`, `UserAccessLog` 엔티티에 필드 추가
    - 새 필드를 처리하도록 Kafka 프로듀서와 컨슈머 업데이트

2. **사용자 정의 분석 쿼리**:
    - `UserAccessLogRepositoryCustom` 인터페이스에 새 메서드 추가
    - QueryDSL을 사용하여 `UserAccessLogRepositoryImpl`에 메서드 구현
    - `UserAccessLogQueryService`에 해당하는 서비스 메서드 생성
    - `UserAccessLogQueryController`에서 REST 엔드포인트로 노출

### 설정 요구사항

1. **Kafka 설정**:
    - `user-access-log-topic` 토픽이 존재하는지 확인
    - `userAccessLogKafkaListenerContainerFactory` 빈 설정

2. **GeoIP 데이터베이스**:
    - `GeoLite2-City.mmdb`가 클래스패스 리소스에 있는지 확인
    - 정확한 지리적 위치 데이터를 위해 주기적으로 업데이트

## 문제 해결

1. **GeoIP 데이터 누락**: 위치 데이터가 "Unknown"으로 표시되는 경우, GeoLite2 데이터베이스가 올바르게 패키징되어 접근 가능한지 확인하세요.

2. **Kafka 연결 문제**: Kafka 설정을 확인하고 토픽이 존재하는지 확인하세요.

3. **IP 주소 해석**: 이 시스템은 먼저 X-Forwarded-For 헤더를 사용하여 IP를 추출한 다음 원격 주소로 대체합니다. 로드 밸런싱 환경에서는 적절한 프록시 구성을 확인하세요.