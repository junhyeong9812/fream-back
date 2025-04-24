# REST 클라이언트 설정

이 문서는 Fream 백엔드 애플리케이션의 외부 API 통신을 위한 RestTemplate 설정을 설명합니다.

## RestTemplateConfig

외부 API 호출을 위한 RestTemplate 설정 클래스입니다. 타임아웃 설정과 요청/응답 로깅 기능을 포함합니다.

```java
@Slf4j
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 연결 타임아웃 5초
        factory.setReadTimeout(10000);    // 읽기 타임아웃 10초
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // 로깅 인터셉터 추가
        restTemplate.getInterceptors().add(loggingInterceptor());
        
        return restTemplate;
    }
    
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            // 요청 로깅
            log.debug("REST 요청: {} {}", request.getMethod(), request.getURI());
            
            long startTime = System.currentTimeMillis();
            
            try {
                ClientHttpResponse response = execution.execute(request, body);
                
                long duration = System.currentTimeMillis() - startTime;
                
                // 응답 로깅
                log.debug("REST 응답: {} {} ({}ms)", response.getStatusCode().value(), 
                          response.getStatusText(), duration);
                
                return response;
            } catch (IOException e) {
                log.error("REST 요청 중 오류 발생: {} {}", request.getMethod(), request.getURI(), e);
                throw e;
            }
        };
    }
}
```

## 주요 기능

- **HTTP 요청 및 응답 처리**: 외부 API와의 통신을 위한 RestTemplate 빈을 제공합니다.
- **타임아웃 설정**: 연결 및 읽기 타임아웃을 설정하여 무한 대기를 방지합니다.
- **로깅 인터셉터**: API 요청/응답을 로깅하여 디버깅 및 모니터링을 지원합니다.
    - 요청 메서드 및 URI 로깅
    - 응답 상태 코드 및 처리 시간 로깅
    - 요청 중 발생한 예외 로깅

## 의존성 추가

RestTemplate에 HttpComponentsClientHttpRequestFactory를 사용하려면 다음 의존성이 필요합니다:

Maven:
```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

Gradle:
```groovy
implementation 'org.apache.httpcomponents.client5:httpclient5'
```

## 날씨 API 사용 예시

다음은 이 RestTemplate 설정을 사용하여 외부 날씨 API를 호출하는 예시입니다:

```java
@Service
@RequiredArgsConstructor
public class OpenMeteoWeatherApiClient implements WeatherApiClient {
    private final RestTemplate restTemplate;
    
    @Value("${weather.api.url}")
    private String apiUrl;
    
    @Value("${weather.api.params.latitude}")
    private double latitude;
    
    @Value("${weather.api.params.longitude}")
    private double longitude;
    
    @Value("${weather.api.params.hourly}")
    private String hourly;
    
    @Value("${weather.api.params.timezone}")
    private String timezone;
    
    @Override
    public WeatherApiResponse getWeatherData() {
        try {
            // URL 구성
            String fullUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("hourly", hourly)
                    .queryParam("timezone", timezone)
                    .toUriString();
            
            log.debug("OpenMeteo API 호출: {}", fullUrl);
            return restTemplate.getForObject(fullUrl, WeatherApiResponse.class);
        } catch (RestClientException e) {
            log.error("OpenMeteo API 호출 중 오류 발생: {}", e.getMessage());
            throw WeatherException.apiError("OpenMeteo API 호출 중 오류가 발생했습니다.", e);
        }
    }
}
```

위 예시에서는:
1. RestTemplate 빈을 주입받습니다.
2. 설정 값들을 @Value 어노테이션으로 가져옵니다.
3. UriComponentsBuilder를 사용하여 URL을 구성합니다.
4. RestTemplate을 사용하여 API를 호출하고 결과를 매핑합니다.
5. 예외 처리를 통해 API 호출 오류를 관리합니다.

## 로깅 확인

로깅 인터셉터가 추가되면 외부 API 호출 시 다음과 같은 로그가 출력됩니다:

```
2025-04-24 10:15:30.123 DEBUG 1234 --- [main] c.f.b.g.config.RestTemplateConfig : REST 요청: GET https://api.open-meteo.com/v1/forecast?latitude=36.5&longitude=127.75&hourly=temperature_2m,precipitation_probability,precipitation,rain,snowfall&timezone=auto

2025-04-24 10:15:31.456 DEBUG 1234 --- [main] c.f.b.g.config.RestTemplateConfig : REST 응답: 200 OK (1333ms)
```

오류가 발생하는 경우:
```
2025-04-24 10:15:30.123 DEBUG 1234 --- [main] c.f.b.g.config.RestTemplateConfig : REST 요청: GET https://api.open-meteo.com/v1/forecast?latitude=36.5&longitude=127.75&hourly=temperature_2m,precipitation_probability,precipitation,rain,snowfall&timezone=auto

2025-04-24 10:15:35.456 ERROR 1234 --- [main] c.f.b.g.config.RestTemplateConfig : REST 요청 중 오류 발생: GET https://api.open-meteo.com/v1/forecast?latitude=36.5&longitude=127.75&hourly=temperature_2m,precipitation_probability,precipitation,rain,snowfall&timezone=auto
org.springframework.web.client.ResourceAccessException: I/O error on GET request for "https://api.open-meteo.com/v1/forecast": Connect timed out; nested exception is java.net.SocketTimeoutException: Connect timed out
```

## 로깅 레벨 설정

application.yml 또는 application.properties에서 로깅 레벨을 설정하여 REST 요청/응답 로깅을 활성화할 수 있습니다:

```yaml
logging:
  level:
    com.fream.back.global.config.RestTemplateConfig: DEBUG
```

## 참고 사항

- 로깅 인터셉터는 디버깅 목적으로 사용되므로, 운영 환경에서는 DEBUG 레벨을 사용하지 않거나 필요한 경우에만 활성화하는 것이 좋습니다.
- 민감한 정보(API 키, 인증 토큰 등)가 포함된 요청의 경우, 로그에서 이러한 정보가 노출되지 않도록 주의해야 합니다.
- 대용량 요청/응답 본문은 로깅하지 않는 것이 좋습니다.