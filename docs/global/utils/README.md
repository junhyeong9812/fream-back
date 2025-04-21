# 유틸리티 모듈

이 디렉토리는 Fream 백엔드 애플리케이션에서 사용되는 유틸리티 클래스들을 포함합니다.

## FileUtils

파일 업로드, 다운로드, 삭제 등 파일 관리 기능을 제공하는 클래스입니다.

```java
@Slf4j
@Component
public class FileUtils {
    // 서버에서 파일을 저장할 루트 디렉토리
    private static final String BASE_DIR = "/home/ubuntu/fream";
    
    public String saveFile(String directory, String prefix, MultipartFile file);
    public boolean deleteFile(String directory, String fileName);
    public String getFileUrl(String directory, String fileName);
    public boolean deleteDirectory(String directory);
    // ...
}
```

### 주요 기능

- **파일 저장**: 이미지, 비디오 등 미디어 파일을 저장합니다.
- **파일 삭제**: 개별 파일 또는 디렉토리를 삭제합니다.
- **파일 URL 생성**: 저장된 파일에 접근할 수 있는 URL을 생성합니다.
- **유효성 검사**: 지원되는 파일 타입 확인 및 안전성 검사를 수행합니다.

### 사용 예시

```java
// 상품 썸네일 이미지 저장
String fileName = fileUtils.saveFile("products/10", "thumbnail_", thumbnailFile);

// 파일 삭제
boolean deleted = fileUtils.deleteFile("products/10", fileName);

// 파일 URL 가져오기
String url = fileUtils.getFileUrl("products/10", fileName);
```

## NginxCachePurgeUtil

Nginx 캐시를 제거하기 위한 유틸리티 클래스입니다.

```java
@Component
public class NginxCachePurgeUtil {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String nginxUrl = "http://nginx:80";
    
    public void purgeProductCache();
    public void purgeEsCache();
    public void purgeStyleCache();
}
```

### 주요 기능

- **상품 캐시 제거**: 상품 관련 API 응답 캐시를 제거합니다.
- **검색 캐시 제거**: Elasticsearch 검색 결과 캐시를 제거합니다.
- **스타일 캐시 제거**: 스타일 게시글 관련 캐시를 제거합니다.

### 사용 예시

```java
// 상품 업데이트 후 캐시 제거
productService.updateProduct(productId, productDto);
cachePurgeUtil.purgeProductCache();
```

## PageUtils

페이징 처리 관련 유틸리티 클래스입니다.

```java
public class PageUtils {
    public static <T> commonDto.PageDto<T> toPageDto(Page<T> page);
}
```

### 주요 기능

- Spring Data의 `Page<T>` 객체를 커스텀 `PageDto<T>`로 변환합니다.

### 사용 예시

```java
// 상품 목록 페이징 처리
Page<Product> productPage = productRepository.findAll(pageable);
PageDto<ProductDto> pageDto = PageUtils.toPageDto(productPage);
```

## PlaywrightBrowserManager

웹 브라우저 자동화를 위한 Playwright 관리 클래스입니다.

```java
public class PlaywrightBrowserManager {
    private Playwright playwright;
    private Browser browser;
    
    public void openBrowser();
    public Page newPage();
    public void closeBrowser();
}
```

### 주요 기능

- **브라우저 관리**: Playwright 브라우저 인스턴스를 생성하고 관리합니다.
- **페이지 생성**: 웹 페이지 인스턴스를 생성합니다.
- **리소스 정리**: 브라우저 인스턴스를 안전하게 종료합니다.

## CjTrackingPlaywright

CJ 물류 배송 추적을 위한 Playwright 자동화 클래스입니다.

```java
@Configuration
public class CjTrackingPlaywright {
    public String getCurrentTrackingStatus(String trackingNumber) throws Exception;
}
```

### 주요 기능

- **배송 상태 조회**: CJ대한통운 배송 조회 페이지에서 현재 배송 상태를 자동으로 추출합니다.

### 사용 예시

```java
// 운송장 번호로 배송 상태 조회
String status = cjTrackingPlaywright.getCurrentTrackingStatus("123456789012");
```

## SecurityUtils

보안 관련 유틸리티 클래스입니다.

```java
@Slf4j
public class SecurityUtils {
    public static String extractEmailFromSecurityContext();
    public static String extractEmailOrAnonymous();
    public static JwtAuthenticationFilter.UserInfo extractUserInfo();
}
```

### 주요 기능

- **인증 정보 추출**: SecurityContext에서 인증된 사용자 정보를 추출합니다.
- **사용자 이메일 조회**: 인증된 사용자의 이메일을 조회합니다.
- **사용자 정보 조회**: 나이, 성별 등 추가 정보를 조회합니다.

### 사용 예시

```java
// 현재 로그인한 사용자의 이메일 가져오기
String email = SecurityUtils.extractEmailFromSecurityContext();

// 익명 사용자 처리도 포함한 이메일 가져오기
String email = SecurityUtils.extractEmailOrAnonymous();
```