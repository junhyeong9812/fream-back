# OpenAI GPT 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 OpenAI GPT API 연동을 위한 설정을 포함합니다.

## GPTConfig

OpenAI GPT API 연동을 위한 설정 클래스입니다.

```java
@Configuration
public class GPTConfig {
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.max-tokens:1000}")
    private int maxTokens;
    
    @Value("${openai.temperature:0.7}")
    private double temperature;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getApiUrl() {
        return apiUrl;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    // OpenAI API 호출 메서드 (선택적)
    @Bean
    public OpenAIService openAIService(RestTemplate restTemplate) {
        return new OpenAIService(restTemplate, this);
    }
}
```

### 주요 기능

- **API 키 관리**: OpenAI API 키를 설정 파일에서 가져옵니다.
- **모델 선택**: 사용할 GPT 모델을 설정합니다 (기본값: gpt-3.5-turbo).
- **API 엔드포인트**: OpenAI API 엔드포인트 URL을 설정합니다.
- **요청 파라미터 설정**: max_tokens, temperature 등의 생성 파라미터를 설정합니다.

## OpenAIService

OpenAI API를 호출하는 서비스 클래스입니다.

```java
@Service
@RequiredArgsConstructor
public class OpenAIService {
    private final RestTemplate restTemplate;
    private final GPTConfig gptConfig;
    
    /**
     * 텍스트 생성 요청
     *
     * @param prompt 프롬프트
     * @return 생성된 텍스트
     */
    public String generateText(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gptConfig.getApiKey());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", gptConfig.getModel());
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "You are a helpful assistant."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", gptConfig.getMaxTokens());
        requestBody.put("temperature", gptConfig.getTemperature());
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                gptConfig.getApiUrl(),
                request,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 응답 파싱
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
            
            throw new RuntimeException("GPT API 호출 실패: " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("GPT API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    /**
     * 텍스트 생성 요청 (고급 옵션)
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt 사용자 프롬프트
     * @param options 추가 옵션
     * @return 생성된 텍스트
     */
    public String generateTextWithOptions(String systemPrompt, String userPrompt, Map<String, Object> options) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gptConfig.getApiKey());
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", options.getOrDefault("model", gptConfig.getModel()));
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", options.getOrDefault("max_tokens", gptConfig.getMaxTokens()));
        requestBody.put("temperature", options.getOrDefault("temperature", gptConfig.getTemperature()));
        
        // 추가 옵션 설정
        if (options.containsKey("top_p")) {
            requestBody.put("top_p", options.get("top_p"));
        }
        
        if (options.containsKey("presence_penalty")) {
            requestBody.put("presence_penalty", options.get("presence_penalty"));
        }
        
        if (options.containsKey("frequency_penalty")) {
            requestBody.put("frequency_penalty", options.get("frequency_penalty"));
        }
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                gptConfig.getApiUrl(),
                request,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 응답 파싱
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
            
            throw new RuntimeException("GPT API 호출 실패: " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("GPT API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
```

## 사용 예시

### 상품 설명 생성

GPT를 활용하여 상품 설명을 자동 생성하는 예시입니다.

```java
@Service
@RequiredArgsConstructor
public class ProductDescriptionService {
    private final OpenAIService openAIService;
    
    /**
     * 상품 설명 생성
     *
     * @param productName 상품명
     * @param category 카테고리
     * @param features 특징
     * @return 생성된 상품 설명
     */
    public String generateProductDescription(String productName, String category, String features) {
        String prompt = String.format(
            "상품명: %s\n카테고리: %s\n특징: %s\n\n위 정보를 바탕으로 100자 내외의 매력적인 상품 설명을 작성해주세요.",
            productName, category, features
        );
        
        return openAIService.generateText(prompt);
    }
    
    /**
     * 상품 태그 생성
     *
     * @param productName 상품명
     * @param description 상품 설명
     * @return 생성된 태그 목록
     */
    public List<String> generateProductTags(String productName, String description) {
        String prompt = String.format(
            "상품명: %s\n상품 설명: %s\n\n위 상품에 적합한 검색 태그를 5개 추천해주세요. 쉼표로 구분해서 작성해주세요.",
            productName, description
        );
        
        String response = openAIService.generateText(prompt);
        return Arrays.asList(response.split(","));
    }
}
```

### 고객 리뷰 분석

GPT를 활용하여 고객 리뷰를 분석하는 예시입니다.

```java
@Service
@RequiredArgsConstructor
public class ReviewAnalysisService {
    private final OpenAIService openAIService;
    
    /**
     * 리뷰 감성 분석
     *
     * @param review 리뷰 내용
     * @return 감성 분석 결과 (긍정/부정/중립)
     */
    public String analyzeSentiment(String review) {
        String prompt = String.format(
            "다음 상품 리뷰의 감성을 분석해주세요. '긍정', '부정', '중립' 중 하나로만 답변해주세요.\n\n%s",
            review
        );
        
        return openAIService.generateText(prompt);
    }
    
    /**
     * 리뷰에서 주요 키워드 추출
     *
     * @param review 리뷰 내용
     * @return 주요 키워드 목록
     */
    public List<String> extractKeywords(String review) {
        String prompt = String.format(
            "다음 상품 리뷰에서 주요 키워드를 5개 추출해주세요. 쉼표로 구분해서 작성해주세요.\n\n%s",
            review
        );
        
        String response = openAIService.generateText(prompt);
        return Arrays.asList(response.split(","));
    }
    
    /**
     * 리뷰 요약
     *
     * @param reviews 리뷰 목록
     * @return 요약된 내용
     */
    public String summarizeReviews(List<String> reviews) {
        String combinedReviews = String.join("\n\n", reviews);
        
        String prompt = String.format(
            "다음은 특정 상품에 대한 여러 리뷰입니다. 이 리뷰들을 분석하여 주요 장점과 단점을 요약해주세요.\n\n%s",
            combinedReviews
        );
        
        return openAIService.generateText(prompt);
    }
}
```

### 스타일 추천

GPT를 활용하여 스타일을 추천하는 예시입니다.

```java
@Service
@RequiredArgsConstructor
public class StyleRecommendationService {
    private final OpenAIService openAIService;
    
    /**
     * 상품 코디 추천
     *
     * @param productName 상품명
     * @param category 카테고리
     * @param gender 성별
     * @return 코디 추천 내용
     */
    public String recommendCoordination(String productName, String category, String gender) {
        String prompt = String.format(
            "%s의 %s(을)를 구매한 %s 고객을 위한 코디 추천을 해주세요. 함께 매치하면 좋은 상품 3가지를 추천해주세요.",
            category, productName, gender
        );
        
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.8); // 더 창의적인 결과를 위해 temperature 증가
        
        return openAIService.generateTextWithOptions(
            "당신은 패션 스타일리스트입니다. 트렌디하고 실용적인 코디 추천을 제공합니다.",
            prompt,
            options
        );
    }
    
    /**
     * 계절별 스타일 추천
     *
     * @param season 계절 (봄, 여름, 가을, 겨울)
     * @param style 선호 스타일
     * @return 계절별 스타일 추천
     */
    public String recommendSeasonalStyle(String season, String style) {
        String prompt = String.format(
            "%s 시즌에 어울리는 %s 스타일의 코디 아이템을 추천해주세요. 상의, 하의, 신발, 액세서리 각각 추천해주세요.",
            season, style
        );
        
        return openAIService.generateText(prompt);
    }
}
```

## 환경 설정 (application.yml)

GPT API 연동을 위한 설정 예시입니다:

```yaml
openai:
  api:
    key: ${OPENAPI_KEY}  # 환경 변수에서 API 키 가져오기
    url: https://api.openai.com/v1/chat/completions
  model: gpt-3.5-turbo  # 기본 모델 설정
  max-tokens: 1000      # 최대 토큰 수
  temperature: 0.7      # 창의성 조절 (0: 결정적, 1: 창의적)
```

## 보안 고려사항

1. **API 키 보안**: API 키는 환경 변수나 안전한 외부 저장소에서 관리
2. **요청 제한**: 과도한 API 요청을 방지하기 위한 속도 제한 설정
3. **콘텐츠 필터링**: 생성된 콘텐츠에 대한 필터링 로직 구현
4. **에러 처리**: API 호출 실패 시 적절한 폴백 메커니즘 제공
5. **비용 모니터링**: API 사용량 및 비용 모니터링

## 모델 선택 가이드

Fream 애플리케이션에서 사용하기 적합한 OpenAI 모델 선택 가이드입니다:

1. **gpt-3.5-turbo** (기본 추천)
    - 비용 효율적이며 대부분의 사용 사례에 충분한 성능
    - 상품 설명, 기본 태그 생성, 간단한 리뷰 분석에 적합
    - 응답 속도가 빠르고 비용이 저렴함

2. **gpt-4** (고급 케이스)
    - 복잡한 분석이나 고품질 콘텐츠 생성이 필요한 경우
    - 상세한 스타일 추천, 심층적인 리뷰 분석, 마케팅 카피 작성에 적합
    - 비용이 높지만 품질이 중요한 경우 사용

## 프롬프트 엔지니어링 팁

효과적인 결과를 얻기 위한 프롬프트 작성 팁입니다:

1. **명확한 지시**: 원하는 결과물의 형식과 내용을 명확하게 지정
2. **맥락 제공**: 충분한 배경 정보와 예시 제공
3. **단계별 접근**: 복잡한 작업은 여러 단계로 나누어 요청
4. **제약 조건 명시**: 글자 수, 형식, 스타일 등의 제약 조건 명시
5. **시스템 프롬프트 활용**: 응답의 일관성을 위해 시스템 프롬프트 활용

## 오류 처리 예시

API 호출 중 발생할 수 있는 오류 처리 예시입니다:

```java
@Service
@RequiredArgsConstructor
public class ResilientOpenAIService {
    private final OpenAIService openAIService;
    private final ProductDescriptionRepository descriptionRepository;
    
    /**
     * 오류 처리가 포함된 상품 설명 생성
     *
     * @param product 상품 정보
     * @return 생성된 상품 설명
     */
    public String generateDescriptionWithFallback(Product product) {
        try {
            // GPT API를 사용하여 설명 생성 시도
            return openAIService.generateText(createProductPrompt(product));
        } catch (Exception e) {
            log.error("GPT API 호출 실패: {}", e.getMessage());
            
            // 1) 캐싱된 유사 상품 설명 검색
            Optional<String> cachedDescription = descriptionRepository
                .findSimilarDescription(product.getCategory(), product.getBrand());
            
            if (cachedDescription.isPresent()) {
                return cachedDescription.get();
            }
            
            // 2) 미리 준비된 템플릿 사용
            return String.format(
                "%s 브랜드의 %s 상품입니다. %s 시즌에 어울리는 제품으로, %s 스타일에 적합합니다.",
                product.getBrand(),
                product.getName(),
                product.getSeason(),
                product.getStyle()
            );
        }
    }
    
    /**
     * 재시도 로직이 포함된 API 호출
     *
     * @param prompt 프롬프트
     * @return 생성된 텍스트
     */
    public String generateTextWithRetry(String prompt) {
        int maxRetries = 3;
        int retryCount = 0;
        int backoffMillis = 1000; // 초기 대기 시간 (1초)
        
        while (retryCount < maxRetries) {
            try {
                return openAIService.generateText(prompt);
            } catch (Exception e) {
                retryCount++;
                log.warn("GPT API 호출 실패 ({}/{}): {}", retryCount, maxRetries, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    throw new RuntimeException("최대 재시도 횟수 초과", e);
                }
                
                // 지수 백오프 적용
                try {
                    Thread.sleep(backoffMillis * (long) Math.pow(2, retryCount - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }
        
        throw new RuntimeException("API 호출 실패");
    }
    
    private String createProductPrompt(Product product) {
        return String.format(
            "상품명: %s\n카테고리: %s\n브랜드: %s\n특징: %s\n\n위 정보를 바탕으로 100자 내외의 매력적인 상품 설명을 작성해주세요.",
            product.getName(),
            product.getCategory(),
            product.getBrand(),
            product.getFeatures()
        );
    }
}
```# OpenAI GPT 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 OpenAI GPT API 연동을 위한 설정을 포함합니다.

## GPTConfig

OpenAI GPT API 연동을 위한 설정 클래스입니다.

```java
@Configuration
public class GPTConfig {
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.max-tokens:1000}")
    private int maxTokens;
    
    @Value("${openai.temperature:0.7}")
    private double temperature;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getApiUrl() {
        return apiUrl;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    // OpenAI API 호출 메서드 (선택적)
    @Bean
    public OpenAIService openAIService(RestTemplate restTemplate) {
        return new OpenAIService(restTemplate, this);
    }
}
```

### 주요 기능

- **API 키 관리**: OpenAI API 키를 설정 파일에서 가져옵니다.
- **모델 선택**: 사용할 GPT 모델을 설정합니다 (기본값: gpt-3.5-turbo).
- **API 엔드포인트**: OpenAI API 엔드포인트 URL을 설정합니다.
- **요청 파라미터 설정**: max_tokens, temperature 등의 생성 파라미터를 설정합니다.

## OpenAIService