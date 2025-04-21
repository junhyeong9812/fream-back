# REST 클라이언트 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 외부 API 통신을 위한 설정을 포함합니다.

## RestTemplateConfig

외부 API 호출을 위한 RestTemplate 설정 클래스입니다.

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 타임아웃 설정
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃 5초
        factory.setReadTimeout(10000);   // 읽기 타임아웃 10초
        restTemplate.setRequestFactory(factory);
        
        // 오류 핸들러 추가
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                // 4xx, 5xx 오류를 특별히 처리하고 싶은 경우 오버라이드
                return super.hasError(response);
            }
        });
        
        // 인터셉터 추가
        restTemplate.getInterceptors().add(loggingInterceptor());
        
        return restTemplate;
    }
    
    // 요청/응답 로깅 인터셉터
    @Bean
    public ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            // 요청 로깅
            log.debug("REST 요청: {} {}", request.getMethod(), request.getURI());
            
            // 실행 및 응답 반환
            ClientHttpResponse response = execution.execute(request, body);
            
            // 응답 로깅
            log.debug("REST 응답: {} {}", response.getStatusCode(), response.getStatusText());
            
            return response;
        };
    }
    
    // 특정 API용 RestTemplate (필요 시)
    @Bean
    @Qualifier("paymentApiTemplate")
    public RestTemplate paymentApiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 결제 API 전용 설정
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 연결 타임아웃 3초
        factory.setReadTimeout(15000);    // 읽기 타임아웃 15초
        restTemplate.setRequestFactory(factory);
        
        // 결제 API 전용 인터셉터
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // API 키 인증 헤더 추가
            request.getHeaders().add("X-Payment-Api-Key", "${payment.api.key}");
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
}
```

### 주요 기능

- **HTTP 요청 및 응답 처리**: 외부 API와의 통신을 위한 RestTemplate 빈을 제공합니다.
- **타임아웃 설정**: 연결 및 읽기 타임아웃을 설정하여 무한 대기를 방지합니다.
- **오류 처리**: 필요시 커스텀 오류 핸들링을 제공합니다.
- **로깅 인터셉터**: API 호출 로깅을 통한 디버깅 및 모니터링을 지원합니다.
- **목적별 템플릿**: 필요시 특정 API용 전용 RestTemplate을 구성할 수 있습니다.

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class ExternalApiService {
    private final RestTemplate restTemplate;
    
    // 기본 API 호출
    public ExternalDataDto fetchExternalData(String resourceId) {
        String url = "https://api.external-service.com/resources/" + resourceId;
        
        try {
            return restTemplate.getForObject(url, ExternalDataDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("리소스를 찾을 수 없습니다: " + resourceId);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("외부 API 호출 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalApiException("외부 API 호출 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            throw new RuntimeException("외부 API 호출 중 예상치 못한 오류가 발생했습니다", e);
        }
    }
    
    // POST 요청 예시
    public void sendNotification(NotificationDto notification) {
        String url = "https://api.notification-service.com/send";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<NotificationDto> request = new HttpEntity<>(notification, headers);
        
        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ExternalApiException("알림 전송 실패: " + response.getStatusCode());
        }
    }
    
    // 요청 파라미터 포함 예시
    public ProductInfoDto getProductInfo(String productId, String locale) {
        String url = "https://api.product-info.com/products/{id}";
        
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("id", productId);
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("locale", locale);
        
        return restTemplate.getForObject(
                builder.buildAndExpand(uriVariables).toUri(),
                ProductInfoDto.class
        );
    }
    
    // exchange 메서드 사용 예시 (응답 헤더 접근 등)
    public ResponseEntity<List<ProductDto>> getProducts(int page, int size) {
        String url = "https://api.product-catalog.com/products";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("page", page)
                .queryParam("size", size);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ParameterizedTypeReference<List<ProductDto>> responseType = 
                new ParameterizedTypeReference<List<ProductDto>>() {};
        
        return restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                responseType
        );
    }
}
```

## PaymentApiService 예시

결제 API용 전용 RestTemplate 사용 예시입니다.

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    @Qualifier("paymentApiTemplate")
    private final RestTemplate paymentApiTemplate;
    
    public PaymentResultDto processPayment(PaymentRequestDto request) {
        String url = "https://api.payment-gateway.com/payments/process";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<PaymentRequestDto> httpEntity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<PaymentResultDto> response = 
                    paymentApiTemplate.postForEntity(url, httpEntity, PaymentResultDto.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                PaymentResultDto result = response.getBody();
                
                // 결제 성공 시 추가 처리
                if (result != null && "SUCCESS".equals(result.getStatus())) {
                    savePaymentResult(result);
                }
                
                return result;
            } else {
                throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("결제 API 호출 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("결제 처리 중 예상치 못한 오류 발생", e);
            throw new PaymentProcessingException("결제 처리 중 예상치 못한 오류가 발생했습니다", e);
        }
    }
    
    private void savePaymentResult(PaymentResultDto result) {
        // 결제 결과 저장 로직
    }
}
```

## OpenFeign 설정 (대안)

RestTemplate 대신 OpenFeign을 사용하여 선언적 HTTP 클라이언트를 구성할 수도 있습니다:

```java
@Configuration
@EnableFeignClients(basePackages = "com.fream.back.global.feign")
public class FeignConfig {
    @Bean
    public Decoder feignDecoder() {
        return new JacksonDecoder();
    }
    
    @Bean
    public Encoder feignEncoder() {
        return new JacksonEncoder();
    }
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomFeignErrorDecoder();
    }
    
    // 커스텀 오류 디코더
    private static class CustomFeignErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();
        
        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.status() == 404) {
                return new ResourceNotFoundException("리소스를 찾을 수 없습니다");
            } else if (response.status() >= 400 && response.status() < 500) {
                return new ClientRequestException("클라이언트 요청 오류: " + response.reason());
            } else if (response.status() >= 500) {
                return new ServerResponseException("서버 응답 오류: " + response.reason());
            }
            
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}

// Feign 클라이언트 인터페이스 예시
@FeignClient(name = "payment-api", url = "${payment.api.url}")
public interface PaymentClient {
    @PostMapping("/payments")
    PaymentResultDto processPayment(@RequestBody PaymentRequestDto request);
    
    @GetMapping("/payments/{id}")
    PaymentStatusDto getPaymentStatus(@PathVariable("id") String paymentId);
    
    @DeleteMapping("/payments/{id}")
    void cancelPayment(@PathVariable("id") String paymentId);
}

// 사용 예시
@Service
@RequiredArgsConstructor
public class PaymentFeignService {
    private final PaymentClient paymentClient;
    
    public PaymentResultDto processPayment(PaymentRequestDto request) {
        try {
            return paymentClient.processPayment(request);
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생", e);
            throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다", e);
        }
    }
}
```