# 날씨 시스템 (Weather System)

## 개요

날씨 시스템은 실시간 기상 정보를 외부 API로부터 가져와 저장하고 제공하기 위해 설계된 종합적인 모듈입니다. 이 시스템은 온도, 강수 확률, 강수량, 비, 눈과 같은 기상 데이터를 수집하여 애플리케이션 사용자에게 현재 및 오늘의 기상 정보를 제공합니다. 데이터는 24시간마다 자동으로 업데이트되며, 서버 시작 시 초기 데이터가 로드됩니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.weather/
├── config/               # 초기화 및 스케줄링 설정
├── controller/           # 데이터 조회를 위한 컨트롤러
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티
├── repository/           # 데이터 접근 계층
└── service/
    ├── command/          # 데이터 가져오기 및 저장을 위한 서비스
    └── query/            # 데이터 조회를 위한 서비스
```

## 주요 구성 요소

### 데이터 수집

1. **WeatherDataInitializer**: 서버 시작 시 초기 날씨 데이터를 가져옵니다
2. **WeatherDataScheduler**: 24시간마다 날씨 데이터를 주기적으로 업데이트합니다
3. **WeatherDataCommandService**: 외부 API로부터 날씨 데이터를 가져와 처리하는 인터페이스
4. **WeatherDataCommandServiceImpl**: 날씨 데이터를 가져와 저장하는 구현체

### 데이터 조회

1. **WeatherQueryController**: 날씨 데이터 조회를 위한 엔드포인트 제공
2. **WeatherQueryService**: 날씨 데이터 조회를 위한 비즈니스 로직 인터페이스
3. **WeatherQueryServiceImpl**: 현재 시점 및 오늘의 날씨 데이터 조회 구현체
4. **WeatherDataRepository**: 사용자 정의 쿼리 메서드가 포함된 데이터베이스 접근

## 수집되는 데이터

각 날씨 데이터 항목에는 다음 정보가 포함됩니다:

- **시간 정보**: 데이터의 타임스탬프(timestamp)
- **온도**: 섭씨 단위의 기온(temperature)
- **강수 관련 정보**:
    - 강수 확률(precipitationProbability): 비나 눈이 내릴 확률(%)
    - 강수량(precipitation): 총 강수량(mm)
    - 비(rain): 비의 양(mm)
    - 눈(snowfall): 눈의 양(cm)
- **메타데이터**: 데이터가 저장된 시간(retrievedAt)

## 엔드포인트

### Query API

```
GET /weather/query/current
```
현재 시점과 가장 가까운 날씨 데이터를 반환합니다. 일치하는 데이터가 없을 경우 204 No Content를 반환합니다.

**응답 예시:**
```json
{
  "timestamp": "2025-03-05T10:00:00",
  "temperature": 15.2,
  "precipitationProbability": 30.0,
  "precipitation": 0.5,
  "rain": 0.5,
  "snowfall": 0.0
}
```

```
GET /weather/query/today
```
오늘(0시 ~ 23:59:59) 전체 시간대의 날씨 데이터를 반환합니다.

**응답 예시:**
```json
[
  {
    "timestamp": "2025-03-05T00:00:00",
    "temperature": 5.4,
    "precipitationProbability": 10.0,
    "precipitation": 0.0,
    "rain": 0.0,
    "snowfall": 0.0
  },
  {
    "timestamp": "2025-03-05T01:00:00",
    "temperature": 4.8,
    "precipitationProbability": 5.0,
    "precipitation": 0.0,
    "rain": 0.0,
    "snowfall": 0.0
  },
  ...
]
```

## 데이터 업데이트 주기

이 시스템은 다음과 같은 방식으로 날씨 데이터를 유지합니다:

1. **초기화**: 서버가 시작될 때 `WeatherDataInitializer`를 통해 24시간 날씨 데이터를 자동으로 가져옵니다.
2. **정기 업데이트**: `WeatherDataScheduler`에 의해 24시간마다 자동으로 데이터를 갱신합니다.
3. **데이터 중복 방지**: 이미 존재하는 타임스탬프의 데이터는 업데이트되고, 없는 경우에만 새로 생성됩니다.

## 외부 API 연동

이 시스템은 [Open-Meteo](https://api.open-meteo.com) API를 사용하여 날씨 데이터를 가져옵니다. API 호출은 다음 파라미터를 포함합니다:
- 위도(latitude): 36.5
- 경도(longitude): 127.75
- 시간별 데이터(hourly): temperature_2m, precipitation_probability, precipitation, rain, snowfall
- 시간대(timezone): auto

## 데이터베이스 스키마

`weather_data` 테이블에는 다음이 포함됩니다:

| 열                       | 타입         | 설명                      |
|--------------------------|--------------|---------------------------|
| id                       | BIGINT       | 기본 키                    |
| timestamp                | TIMESTAMP    | 날씨 데이터의 시간          |
| temperature              | DOUBLE       | 온도(°C)                  |
| precipitation_probability| DOUBLE       | 강수 확률(%)               |
| precipitation            | DOUBLE       | 강수량(mm)                |
| rain                     | DOUBLE       | 비(mm)                    |
| snowfall                 | DOUBLE       | 눈(cm)                    |
| retrieved_at             | TIMESTAMP    | 데이터가 저장된 시간        |

## 구현 참고사항

### 개발자를 위한 안내

1. **새 날씨 속성 추가**:
    - `WeatherDataDto`, `WeatherData` 엔티티에 필드 추가
    - `WeatherApiResponse` DTO에 해당 필드 매핑 추가
    - `convertToDto` 및 `updateWeatherData` 메서드 업데이트

2. **사용자 정의 날씨 쿼리**:
    - `WeatherDataRepository`에 새 쿼리 메서드 추가
    - `WeatherQueryService` 인터페이스에 메서드 선언
    - `WeatherQueryServiceImpl`에 메서드 구현
    - `WeatherQueryController`에서 REST 엔드포인트로 노출

### 설정 요구사항

1. **RestTemplate 설정**:
    - HTTP 요청을 처리하기 위한 `RestTemplate` 빈이 필요합니다.
    - 애플리케이션 구성 클래스에서 다음과 같이 정의할 수 있습니다:
   ```java
   @Bean
   public RestTemplate restTemplate() {
       return new RestTemplate();
   }
   ```

2. **스케줄링 활성화**:
    - 메인 애플리케이션 클래스 또는 설정 클래스에 `@EnableScheduling` 어노테이션이 필요합니다.

## 문제 해결

1. **외부 API 연결 문제**: Open-Meteo API에 연결할 수 없는 경우, 네트워크 연결 및 API 엔드포인트 URL을 확인하세요.

2. **데이터 검색 시 No Content 응답**: 아직 데이터가 로드되지 않았거나 요청된 시간대의 데이터가 없는 경우입니다. 서버 로그를 확인하여 `fetchAndStore24HourWeatherData` 메서드가 정상적으로 실행되었는지 확인하세요.

3. **비정상 데이터 값**: 저장된 데이터 값이 예상과 다른 경우, Open-Meteo API의 응답 형식이 변경되었는지 확인하고 필요에 따라 `WeatherApiResponse` DTO를 업데이트하세요.