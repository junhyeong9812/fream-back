# 글로벌 패키지 구조

이 디렉토리는 Fream 백엔드 애플리케이션의 글로벌 패키지를 설명합니다. 글로벌 패키지는 애플리케이션 전체에서 공통으로 사용되는 기능들을 포함합니다.

## 패키지 구조

```
com.fream.back.global/
├── config/                    # 전역 설정 클래스
│   ├── security/              # 보안 관련 설정
│   ├── websocket/             # 웹소켓 관련 설정
│   └── kafka/                 # Kafka 관련 설정
├── controller/                # 전역 컨트롤러
├── dto/                       # 전역 DTO 클래스
├── entity/                    # 공통 엔티티 클래스
├── exception/                 # 전역 예외 처리
└── utils/                     # 유틸리티 클래스
```

## 주요 컴포넌트 설명서

각 주요 컴포넌트에 대한 상세 문서는 다음 링크에서 확인할 수 있습니다:

1. [설정 모듈](./config/README.md) - 애플리케이션 설정 관련 클래스
2. [보안 모듈](./security/README.md) - JWT 인증 및 보안 관련 클래스
3. [웹소켓 모듈](./websocket/README.md) - 실시간 통신 관련 클래스
4. [유틸리티 모듈](./utils/README.md) - 파일 처리, 캐시 제거 등 유틸리티 클래스
5. [엔티티 모듈](./entity/README.md) - 공통 기본 엔티티 클래스
6. [DTO 모듈](./dto/README.md) - 공통 데이터 전송 객체
7. [로깅 모듈](./logging/README.md) - 로그 관리 관련 클래스

## 일반적인 사용 패턴

### 응답 공통화

모든 API 응답은 표준화된 형식을 갖도록 `ResponseDto`를 사용합니다:

```java
@GetMapping("/products/{id}")
public ResponseDto<ProductDto> getProduct(@PathVariable Long id) {
    ProductDto product = productService.getProduct(id);
    return ResponseDto.success(product);
}
```

### 페이징 처리

페이징 처리가 필요한 API는 `PageDto`와 `PageUtils`를 사용합니다:

```java
@GetMapping("/products")
public ResponseDto<PageDto<ProductDto>> getProducts(Pageable pageable) {
    Page<Product> productPage = productRepository.findAll(pageable);
    List<ProductDto> productDtos = productPage.stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    
    PageDto<ProductDto> pageDto = PageUtils.toPageDto(productPage);
    pageDto.setContent(productDtos);
    
    return ResponseDto.success(pageDto);
}
```

### 엔티티 생성 및 수정 정보 자동화

엔티티의 생성/수정 시간과 생성자/수정자 정보를 자동으로 관리하기 위해 기본 엔티티 클래스를 상속받습니다:

```java
// 생성/수정 시간만 필요한 경우
public class Product extends BaseTimeEntity {
    // 필드 정의
}

// 생성/수정 시간 + 생성자/수정자 정보가 필요한 경우
public class Order extends BaseEntity {
    // 필드 정의
}
```

### 보안 관련 패턴

현재 인증된 사용자 정보를 가져올 때 `SecurityUtils`를 사용합니다:

```java
@PostMapping("/some-endpoint")
public ResponseDto<SomeDto> someEndpoint() {
    // 현재 로그인한 사용자의 이메일 가져오기
    String email = SecurityUtils.extractEmailFromSecurityContext();
    
    // 사용자 나이, 성별 등 추가 정보 가져오기
    JwtAuthenticationFilter.UserInfo userInfo = SecurityUtils.extractUserInfo();
    Integer age = userInfo.getAge();
    Gender gender = userInfo.getGender();
    
    // 비즈니스 로직...
    
    return ResponseDto.success(someDto);
}
```

### 파일 업로드/다운로드 패턴

파일 처리가 필요한 경우 `FileUtils`를 사용합니다:

```java
@PostMapping("/products/{id}/images")
public ResponseDto<String> uploadProductImage(
        @PathVariable Long id, 
        @RequestParam("file") MultipartFile file) {
    
    // 파일 저장 (상품 ID를 디렉토리 경로로 사용)
    String fileName = fileUtils.saveFile("products/" + id, "image_", file);
    
    // 파일 URL 생성
    String fileUrl = fileUtils.getFileUrl("products/" + id, fileName);
    
    // 상품 이미지 정보 업데이트
    productService.updateProductImage(id, fileName, fileUrl);
    
    return ResponseDto.success(fileUrl, "이미지가 업로드되었습니다.");
}
```

### 웹소켓 메시지 패턴

실시간 알림이 필요한 경우 웹소켓을 사용합니다:

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailBasedUserDestinationResolver destinationResolver;
    
    public void sendNotification(String email, NotificationDto notification) {
        // 사용자별 목적지로 메시지 전송
        String destination = "/queue/notifications";
        messagingTemplate.convertAndSendToUser(
            email,  // 수신자 이메일
            destination,  // 목적지 경로
            notification  // 전송할 데이터
        );
    }
}
```

### Kafka 이벤트 처리 패턴

비동기 이벤트 처리가 필요한 경우 Kafka를 사용합니다:

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void placeOrder(OrderDto orderDto) {
        // 주문 처리...
        
        // 주문 이벤트 발행
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .totalAmount(savedOrder.getTotalAmount())
                .status(OrderStatus.PENDING)
                .build();
                
        kafkaTemplate.send("order-events", event);
    }
    
    @KafkaListener(topics = "order-events")
    public void processOrderEvent(OrderEvent event) {
        // 주문 이벤트 처리...
    }
}
```