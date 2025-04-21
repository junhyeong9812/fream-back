# 웹 MVC 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 Spring MVC 관련 설정을 포함합니다.

## WebConfig

Spring MVC 웹 설정을 담당하는 클래스입니다.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();

        resolver.setOneIndexedParameters(true); // ✅ 페이지 번호를 1부터 시작하도록 설정
        resolver.setFallbackPageable(PageRequest.of(0, 10)); // ✅ 기본 페이지 크기 10으로 설정
        resolver.setMaxPageSize(2000); // ✅ 최대 페이지 크기

        resolvers.add(resolver);
    }
    
    // CORS 설정 (필요시)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://www.pinjun.xyz")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    // 리소스 핸들러 설정 (정적 리소스 접근용)
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

### 주요 기능

- **페이징 설정**:
    - 페이지 번호를 1부터 시작하도록 설정합니다. (기본값은 0-based)
    - 기본 페이지 크기를 10으로 설정합니다.
    - 최대 페이지 크기를 2000으로 제한합니다.
- **CORS 설정**: 필요 시 API 엔드포인트에 대한 CORS 정책을 구성합니다.
- **정적 리소스 핸들러**: 정적 리소스 접근을 위한 설정을 제공합니다.

### 사용 예시

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    
    @GetMapping
    public ResponseDto<PageDto<ProductDto>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        
        // 서비스 호출 (Pageable을 자동으로 인식)
        Page<ProductDto> productPage = productService.findProducts(
                category, keyword, minPrice, maxPrice, pageable);
        
        // 응답 생성
        PageDto<ProductDto> pageDto = PageUtils.toPageDto(productPage);
        return ResponseDto.success(pageDto);
    }
}
```

클라이언트에서는 다음과 같이 요청할 수 있습니다:
- `GET /api/products?page=1&size=20` - 첫 번째 페이지, 페이지당 20개
- `GET /api/products?page=2&size=10&sort=price,asc` - 두 번째 페이지, 가격 오름차순
- `GET /api/products?category=shoes&minPrice=50000&page=1` - 신발 카테고리, 최소 가격 5만원

## 커스텀 변환기 (Converter) 설정

필요한 경우 문자열을 다른 데이터 타입으로 변환하는 커스텀 변환기를 설정할 수 있습니다.

```java
@Configuration
public class WebConverterConfig {
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addFormatters(FormatterRegistry registry) {
                // String -> Enum 변환기 등록
                registry.addConverter(new StringToEnumConverterFactory());
                
                // String -> LocalDateTime 변환기 등록
                registry.addConverter(new StringToLocalDateTimeConverter());
            }
        };
    }
    
    // String -> Enum 변환을 위한 Factory
    private static class StringToEnumConverterFactory implements ConverterFactory<String, Enum<?>> {
        @Override
        public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
            return new StringToEnumConverter<>(targetType);
        }
        
        private static class StringToEnumConverter<T extends Enum<?>> implements Converter<String, T> {
            private final Class<T> enumType;
            
            public StringToEnumConverter(Class<T> enumType) {
                this.enumType = enumType;
            }
            
            @Override
            public T convert(String source) {
                if (source.isEmpty()) {
                    return null;
                }
                
                // 대소문자 무시하고 Enum 값 찾기
                return Arrays.stream(enumType.getEnumConstants())
                        .filter(e -> e.name().equalsIgnoreCase(source))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No enum constant " + enumType.getCanonicalName() + "." + source));
            }
        }
    }
    
    // String -> LocalDateTime 변환기
    private static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        @Override
        public LocalDateTime convert(String source) {
            if (source.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(source, FORMATTER);
        }
    }
}
```

## 메시지 컨버터 설정

API 요청/응답 본문 직렬화/역직렬화를 위한 메시지 컨버터 설정입니다.

```java
@Configuration
public class WebMessageConverterConfig {
    @Bean
    public WebMvcConfigurer webMvcMessageConverterConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                // Jackson 메시지 컨버터 구성
                MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
                jacksonConverter.setObjectMapper(customObjectMapper());
                converters.add(jacksonConverter);
            }
        };
    }
    
    @Bean
    public ObjectMapper customObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // JSON 직렬화/역직렬화 설정
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // null 값 필드 제외
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 알 수 없는 속성 무시
        
        // 날짜/시간 포맷 설정
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, 
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, 
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        
        return objectMapper;
    }
}
```

## AsyncConfig

비동기 작업 처리를 위한 설정 클래스입니다.

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 풀 크기
        executor.setMaxPoolSize(10); // 최대 스레드 풀 크기
        executor.setQueueCapacity(25); // 작업 큐 용량
        executor.setThreadNamePrefix("Fream-Async-"); // 스레드 이름 접두어
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
```

### 비동기 처리 사용 예시

```java
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(to);
            helper.setSubject("환영합니다, " + name + "님!");
            helper.setText("<html><body><h2>Fream에 오신 것을 환영합니다!</h2></body></html>", true);
            
            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
```