# 날씨 시스템 (Weather System)

## 개요

날씨 시스템은 실시간 기상 정보를 외부 API로부터 가져와 저장하고 제공하기 위해 설계된 종합적인 모듈입니다. 이 시스템은 온도, 강수 확률, 강수량, 비, 눈과 같은 기상 데이터를 수집하여 애플리케이션 사용자에게 현재 및 오늘의 기상 정보를 제공합니다. 데이터는 24시간마다 자동으로 업데이트되며, 서버 시작 시 초기 데이터가 로드됩니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.weather/
├── api/                  # 외부 API 클라이언트
│   └── impl/             # API 클라이언트 구현체
├── config/               # 초기화 및 스케줄링 설정
├── controller/           # 데이터 조회를 위한 컨트롤러
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티
├── exception/            # 예외 처리
├── repository/           # 데이터 접근 계층
└── service/
    ├── command/          # 데이터 가져오기 및 저장을 위한 서비스
    └── query/            # 데이터 조회를 위한 서비스
```

## 주요 구성 요소

### 외부 API 클라이언트

1. **WeatherApiClient**: 외부 날씨 API 호출을 위한 인터페이스
2. **OpenMeteoWeatherApiClient**: Open-Meteo API 호출 구현체

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

### 예외 처리

1. **WeatherException**: 날씨 도메인의 모든 예외를 처리하는 통합 클래스
    - 팩토리 메서드 패턴을 사용하여 다양한 예외 상황 처리
    - 예: `apiError()`, `dataNotFound()`, `apiParsingError()` 등

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
오늘(0시 ~ 23:59:59) 전체 시간대의 날씨 데이터를 반환합니다. 데이터가 없을 경우 204 No Content를 반환합니다.

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

1. **초기화**: 서버가 시작될 때 `WeatherDataInitializer`를 통해 날씨 데이터를 자동으로 가져옵니다.
2. **정기 업데이트**: `WeatherDataScheduler`에 의해 정해진 간격(기본 24시간)마다 자동으로 데이터를 갱신합니다.
3. **데이터 중복 방지**: 이미 존재하는 타임스탬프의 데이터는 업데이트되고, 없는 경우에만 새로 생성됩니다.
4. **일괄 처리**: 데이터 저장은 saveAll 메서드를 통해 일괄 처리되어 성능을 최적화합니다.

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
| timestamp                | TIMESTAMP    | 날씨 데이터의 시간 (유니크)  |
| temperature              | DOUBLE       | 온도(°C)                  |
| precipitation_probability| DOUBLE       | 강수 확률(%)               |
| precipitation            | DOUBLE       | 강수량(mm)                |
| rain                     | DOUBLE       | 비(mm)                    |
| snowfall                 | DOUBLE       | 눈(cm)                    |
| retrieved_at             | TIMESTAMP    | 데이터가 저장된 시간        |

## 설정 방법

### application.yml 설정

날씨 시스템 관련 설정은 application.yml 파일에서 구성합니다:

```yaml
weather:
  api:
    url: https://api.open-meteo.com/v1/forecast
    params:
      latitude: 36.5
      longitude: 127.75
      hourly: temperature_2m,precipitation_probability,precipitation,rain,snowfall
      timezone: auto
  scheduler:
    interval: 86400000        # 갱신 주기(밀리초) - 24시간
    initial-delay: 10000      # 초기 지연 시간(밀리초) - 10초
  data:
    hours-count: 48           # 한 번에 처리할 시간 데이터 수
```

### RestTemplate 설정

날씨 API 호출을 위한 RestTemplate 설정은 다음과 같이 구성됩니다:

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5초
        factory.setReadTimeout(10000);    // 10초
        return new RestTemplate(factory);
    }
}
```

이 설정을 사용하려면 다음 의존성이 필요합니다:

```groovy
implementation 'org.apache.httpcomponents.client5:httpclient5'
```

## 구현 참고사항

### 개발자를 위한 안내

1. **새 날씨 속성 추가**:
    - `WeatherDataDto`, `WeatherData` 엔티티에 필드 추가
    - `WeatherApiResponse` DTO에 해당 필드 매핑 추가
    - `WeatherDataDto.fromEntity` 메서드 업데이트
    - `updateWeatherData` 메서드 업데이트

2. **사용자 정의 날씨 쿼리**:
    - `WeatherDataRepository`에 새 쿼리 메서드 추가
    - `WeatherQueryService` 인터페이스에 메서드 선언
    - `WeatherQueryServiceImpl`에 메서드 구현
    - `WeatherQueryController`에서 REST 엔드포인트로 노출

3. **DTO 변환 로직**:
    - DTO와 엔티티 간 변환은 `WeatherDataDto.fromEntity` 및 `fromEntities` 정적 메서드를 사용합니다.
    - 예시:
    ```java
    // 단일 엔티티를 DTO로 변환
    WeatherDataDto dto = WeatherDataDto.fromEntity(weatherDataEntity);
    
    // 엔티티 목록을 DTO 목록으로 변환
    List<WeatherDataDto> dtoList = WeatherDataDto.fromEntities(weatherDataEntities);
    ```

4. **예외 처리**:
    - 팩토리 메서드를 사용하여 예외를 생성합니다:
    ```java
    // API 오류
    throw WeatherException.apiError("API 호출 중 오류가 발생했습니다.");
    
    // 데이터 없음
    throw WeatherException.dataNotFound("요청한 시간대의 날씨 데이터가 없습니다.");
    ```

### 스케줄링 활성화

스케줄링 기능을 사용하려면 메인 애플리케이션 클래스 또는 설정 클래스에 `@EnableScheduling` 어노테이션이 필요합니다. 현재는 `WeatherDataScheduler` 클래스에 직접 적용되어 있습니다.

## 최적화 포인트

1. **타임스탬프 유니크 인덱스**: timestamp 필드에 유니크 인덱스를 적용하여 중복 방지
2. **일괄 데이터 처리**: saveAll 메서드를 통한 데이터 일괄 저장으로 DB 부하 감소
3. **RestTemplate 최적화**: 타임아웃 설정을 통한 안정성 확보
4. **외부 환경 설정**: application.yml 통한 설정값 관리로 유연성 증가
5. **트랜잭션 관리**: @Transactional 어노테이션을 통한 데이터 일관성 유지
6. **인터페이스 기반 설계**: API 클라이언트를 인터페이스로 분리하여 테스트 용이성 확보

## 문제 해결

1. **외부 API 연결 문제**: Open-Meteo API에 연결할 수 없는 경우, 네트워크 연결 및 API 엔드포인트 URL을 확인하세요.

2. **데이터 검색 시 No Content 응답**: 아직 데이터가 로드되지 않았거나 요청된 시간대의 데이터가 없는 경우입니다. 서버 로그를 확인하여 `fetchAndStore24HourWeatherData` 메서드가 정상적으로 실행되었는지 확인하세요.

3. **RestTemplate 관련 오류**: HttpComponentsClientHttpRequestFactory 사용 시 의존성이 누락되었다면 'org.apache.httpcomponents.client5:httpclient5' 의존성이 프로젝트에 추가되어 있는지 확인하세요.

4. **스케줄러가 실행되지 않음**: `@EnableScheduling` 어노테이션이 올바르게 적용되었는지, application.yml의 스케줄러 설정값이 올바른지 확인하세요.

5. **비정상 데이터 값**: 저장된 데이터 값이 예상과 다른 경우, Open-Meteo API의 응답 형식이 변경되었는지 확인하고 필요에 따라 `WeatherApiResponse` DTO를 업데이트하세요.