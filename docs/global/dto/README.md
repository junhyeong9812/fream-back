# 글로벌 DTO 모듈

이 디렉토리는 Fream 백엔드 애플리케이션에서 공통으로 사용되는 Data Transfer Object(DTO) 클래스들을 포함합니다.

## ResponseDto

API 응답을 위한 표준 응답 DTO 클래스입니다.

```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ResponseDto<T> success(T data);
    public static <T> ResponseDto<T> success(T data, String message);
    public static <T> ResponseDto<T> fail(String message);
}
```

### 주요 기능

- **표준화된 응답 형식**: 모든 API 응답에 일관된 구조를 제공합니다.
- **제네릭 타입**: 어떤 타입의 데이터라도 포함할 수 있습니다.
- **정적 팩토리 메서드**: 성공/실패 응답을 쉽게 생성할 수 있는 메서드를 제공합니다.

### 응답 구조

- **success**: 요청 처리 성공 여부 (true/false)
- **message**: 응답 메시지 (선택적)
- **data**: 실제 응답 데이터 (선택적)

### 사용 예시

```java
// 성공 응답 (데이터만)
@GetMapping("/products/{id}")
public ResponseDto<ProductDto> getProduct(@PathVariable Long id) {
    ProductDto product = productService.getProduct(id);
    return ResponseDto.success(product);
}

// 성공 응답 (데이터 + 메시지)
@PostMapping("/products")
public ResponseDto<ProductDto> createProduct(@RequestBody ProductDto productDto) {
    ProductDto saved = productService.saveProduct(productDto);
    return ResponseDto.success(saved, "상품이 성공적으로 등록되었습니다.");
}

// 실패 응답
@DeleteMapping("/products/{id}")
public ResponseDto<Void> deleteProduct(@PathVariable Long id) {
    try {
        productService.deleteProduct(id);
        return ResponseDto.success(null, "상품이 삭제되었습니다.");
    } catch (Exception e) {
        return ResponseDto.fail("상품 삭제에 실패했습니다: " + e.getMessage());
    }
}
```

## PageDto (commonDto.PageDto)

페이징된 결과를 위한 DTO 클래스입니다.

```java
public class commonDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageDto<T> {
        private List<T> content;      // 실제 데이터 목록
        private long totalElements;   // 전체 개수
        private int totalPages;       // 전체 페이지
        private int page;            // 현재 페이지
        private int size;            // 페이지 당 개수
    }
}
```

### 주요 기능

- **페이징 정보 캡슐화**: 페이징된 결과의 메타데이터를 포함합니다.
- **제네릭 타입**: 어떤 타입의 목록 데이터라도 포함할 수 있습니다.

### 사용 예시

```java
// 상품 페이징 API
@GetMapping("/products")
public ResponseDto<PageDto<ProductDto>> getProducts(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    // Spring Data JPA에서 페이징 처리 (0-based)
    Pageable pageable = PageRequest.of(page - 1, size);
    Page<Product> productPage = productRepository.findAll(pageable);
    
    // Product -> ProductDto 변환
    List<ProductDto> productDtos = productPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    
    // PageDto 생성
    PageDto<ProductDto> pageDto = new PageDto<>(
            productDtos,
            productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.getNumber() + 1, // 1-based로 변환
            productPage.getSize()
    );
    
    return ResponseDto.success(pageDto);
}
```

## 로그 관련 DTO

애플리케이션 로그를 관리하기 위한 DTO들입니다.

### LogFileDTO

로그 파일 정보를 담는 DTO입니다.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogFileDTO {
    private String fileName;
    private Long fileSize;
    private String lastModified;
}
```

### LogLineDTO

로그 한 줄의 파싱된 정보를 담는 DTO입니다.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogLineDTO {
    private int lineNumber;
    private String timestamp;
    private String thread;
    private String level;
    private String logger;
    private String message;
    private String rawLine;
}
```

### LogResponseDTO

로그 조회 API 응답을 위한 DTO입니다.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDTO {
    private String fileName;
    private int page;
    private int size;
    private int totalLines;
    private int totalFilteredLines;
    private List<LogLineDTO> content;
    private String error;
}
```

### 주요 기능

- **로그 파일 관리**: 로그 파일 목록 조회, 파일 크기, 수정 일시 등의 정보를 제공합니다.
- **로그 내용 파싱**: 로그 라인을 파싱하여 타임스탬프, 로그 레벨, 스레드 이름 등으로 구조화합니다.
- **페이징 및 필터링**: 대용량 로그 파일을 페이징하여 조회하고 특정 조건으로 필터링합니다.