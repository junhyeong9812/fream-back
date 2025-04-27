# FAQ 시스템 (Frequently Asked Questions)

## 개요

FAQ 시스템은 자주 묻는 질문과 답변을 관리하기 위한 모듈입니다. 이 시스템은 카테고리별 FAQ 관리, 이미지가 포함된 리치 텍스트 답변 지원, 검색 기능 등을 제공합니다. 관리자는 FAQ의 생성, 수정, 삭제 작업을 수행할 수 있으며, 일반 사용자는 FAQ 조회와 검색 기능을 이용할 수 있습니다.

## 최근 개선사항

1. **글로벌 FileUtils 활용**: 기존 `FAQFileStorageUtil` 대신 글로벌 `FileUtils`를 사용하여 파일 처리 일관성 개선
2. **캐싱 적용**: 조회 및 검색 성능 향상을 위한 스프링 캐시 적용
3. **HTML 처리 개선**: Jsoup 라이브러리 활용으로 HTML 이미지 태그 처리 안정성 향상
4. **예외 처리 중앙화**: 도메인 예외를 `GlobalExceptionHandler`로 통합하여 일관된 예외 처리
5. **API 경로 표준화**: API 엔드포인트를 `/api/faq`로 표준화
6. **응답 구조 개선**: 클라이언트에 반환되는 이미지 URL 처리 및 날짜 필드 일관성 개선
7. **유효성 검사 강화**: 입력값에 대한 Bean Validation 적용

## 아키텍처

```
com.fream.back.domain.faq/
├── controller/
│   ├── command/            # FAQ 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/              # FAQ 조회 및 검색을 위한 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── exception/              # 도메인 관련 예외 클래스
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # FAQ 생성, 수정, 삭제를 위한 서비스
    └── query/              # FAQ 조회 및 검색을 위한 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **FAQCommandController**: FAQ 생성, 수정, 삭제 API를 제공합니다 (관리자 전용).
2. **FAQQueryController**: FAQ 조회, 검색, 파일 다운로드 API를 제공합니다.

### 서비스

1. **FAQCommandService**: FAQ 생성, 수정, 삭제 및 이미지 처리 로직을 담당합니다.
2. **FAQQueryService**: FAQ 조회 및 검색 로직을 담당하며, 캐싱 기능을 적용합니다.

### 엔티티

1. **FAQ**: FAQ 기본 정보(카테고리, 질문, 답변)를 저장하며, `BaseTimeEntity`를 상속받아 생성일/수정일을 관리합니다.
2. **FAQImage**: FAQ와 연결된 이미지 정보를 저장합니다.
3. **FAQCategory**: FAQ 카테고리 열거형(POLICY, GENERAL, BUYING, SELLING).

### 저장소

1. **FAQRepository**: FAQ 엔티티의 기본 CRUD 및 조회 기능을 제공합니다.
2. **FAQRepositoryCustom**: FAQ 검색 기능을 위한 인터페이스입니다.
3. **FAQRepositoryImpl**: QueryDSL을 사용한 FAQ 검색 구현체입니다.
4. **FAQImageRepository**: FAQ 이미지 엔티티의 CRUD 및 조회 기능을 제공합니다.

### 예외 처리

1. **FAQException**: FAQ 도메인의 기본 예외 클래스입니다.
2. **FAQNotFoundException**: FAQ를 찾을 수 없을 때 발생하는 예외입니다.
3. **FAQFileException**: 파일 처리 중 발생하는 예외입니다.
4. **FAQPermissionException**: 권한 부족으로 발생하는 예외입니다.

## API 엔드포인트

### 관리자 API (Command)

```
POST /api/faq
```
새로운 FAQ를 생성합니다. 이미지 첨부가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "category": "BUYING",
  "question": "주문 취소는 어떻게 하나요?",
  "answer": "<p>주문 취소는 <b>마이페이지</b>에서 가능합니다.</p><p><img src='data:image/jpeg;base64,...'/></p>",
  "files": [이미지 파일]
}
```

```
PUT /api/faq/{id}
```
기존 FAQ를 수정합니다. 이미지 추가/삭제가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "category": "BUYING",
  "question": "주문 취소는 어떻게 하나요?",
  "answer": "<p>주문 취소는 <b>마이페이지 > 주문내역</b>에서 가능합니다.</p><p><img src='/api/faq/files/1/img_abc123.jpg'/></p>",
  "retainedImageUrls": ["img_abc123.jpg"],
  "newFiles": [새 이미지 파일]
}
```

```
DELETE /api/faq/{id}
```
FAQ를 삭제합니다. 연결된 이미지 파일도 함께 삭제됩니다.

### 일반 사용자 API (Query)

```
GET /api/faq?category=BUYING&page=0&size=10
```
FAQ 목록을 조회합니다. 카테고리 필터를 적용할 수 있으며, 페이징 처리가 가능합니다.

**응답 예시:**
```json
{
  "content": [
    {
      "id": 1,
      "category": "BUYING",
      "question": "주문 취소는 어떻게 하나요?",
      "answer": "<p>주문 취소는 <b>마이페이지 > 주문내역</b>에서 가능합니다.</p><p><img src='/api/faq/files/1/img_abc123.jpg'/></p>",
      "imageUrls": ["img_abc123.jpg"],
      "createdDate": "2024-04-15T09:30:00",
      "modifiedDate": "2024-04-15T10:15:00"
    },
    // ... 추가 FAQ 항목
  ],
  "pageable": { ... },
  "totalPages": 5,
  "totalElements": 48,
  "last": false,
  "size": 10,
  "number": 0,
  "sort": { ... },
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

```
GET /api/faq/{id}
```
단일 FAQ를 조회합니다.

```
GET /api/faq/search?keyword=취소&page=0&size=10
```
FAQ를 검색합니다. 질문과 답변 내용에서 키워드를 검색합니다.

```
GET /api/faq/files/{faqId}/{fileName}
```
FAQ 이미지 파일을 다운로드합니다.

## 데이터베이스 스키마

### 1. FAQ 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| category       | VARCHAR        | FAQ 카테고리(ENUM 문자열)     |
| question       | VARCHAR(100)   | 질문 (최대 100자)            |
| answer         | TEXT/CLOB      | 답변(HTML 형식)              |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. FAQ_IMAGE 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| faq_id         | BIGINT         | FAQ 외래 키                 |
| image_url      | VARCHAR        | 이미지 파일명                |

## 파일 저장 구조

### 경로 구조

- 기본 경로: `/home/ubuntu/fream/faq/`
- FAQ 별 하위 폴더: `faq/{faqId}/`
- 파일 이름 접두사: `img_`
- 파일 이름: 글로벌 FileUtils를 통해 생성된 고유 파일명

### 예시

- 물리적 파일 경로: `/home/ubuntu/fream/faq/1/img_550e8400e29b41d4.png`
- 데이터베이스 저장 경로: `img_550e8400e29b41d4.png`
- 웹 URL: `/api/faq/files/1/img_550e8400e29b41d4.png`

## 주요 기능 상세

### 1. 리치 텍스트 답변 및 이미지 관리

FAQ 시스템은 HTML 형식의 리치 텍스트 답변을 지원합니다. 답변에는 이미지를 포함할 수 있으며, 다음과 같은 이미지 처리 로직을 제공합니다:

1. **이미지 저장**:
    - 새 FAQ 생성 또는 업데이트 시 이미지 파일을 서버에 저장합니다.
    - 이미지 파일은 `faq/{id}` 하위 폴더에 고유 파일명으로 저장됩니다.
    - 글로벌 `FileUtils`를 사용하여 파일 시스템 작업을 일관되게 처리합니다.

2. **이미지 경로 처리**:
    - Base64 인코딩된 이미지 데이터(`<img src='data:image/jpeg;base64,...'/>`)를 실제 저장된 이미지 경로로 대체합니다.
    - Jsoup 라이브러리를 사용하여 HTML 이미지 태그를 안전하게 처리합니다.
    - 경로 형식: `/api/faq/files/{faqId}/{fileName}`

3. **이미지 제거 처리**:
    - FAQ 수정 시 콘텐츠에서 제거된 이미지는 자동으로 삭제됩니다.
    - FAQ 삭제 시 연결된 모든 이미지 파일 및 디렉토리를 삭제합니다.

### 2. 카테고리 기반 관리

FAQ는 카테고리별로 관리됩니다. 현재 지원하는 카테고리는 다음과 같습니다:

- **POLICY**: 이용 정책
- **GENERAL**: 공통
- **BUYING**: 구매
- **SELLING**: 판매

### 3. 전체 키워드 검색

FAQ 검색 기능은 질문(question)과 답변(answer) 필드에서 키워드를 검색합니다. QueryDSL을 사용하여 효율적인 검색을 구현하였습니다.

### 4. 캐싱 적용

FAQ 조회 성능 향상을 위해 Spring의 캐싱 기능을 적용하였습니다:

```java
@Cacheable(value = "faqList", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
public Page<FAQResponseDto> getFAQs(Pageable pageable) { ... }

@Cacheable(value = "faqDetail", key = "#id")
public FAQResponseDto getFAQ(Long id) { ... }

@Cacheable(value = "faqCategoryList", key = "#category.name() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
public Page<FAQResponseDto> getFAQsByCategory(FAQCategory category, Pageable pageable) { ... }

@Cacheable(value = "faqSearchResults", key = "'search:' + (T(java.util.Objects).toString(#keyword)) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
public Page<FAQResponseDto> searchFAQs(String keyword, Pageable pageable) { ... }
```

생성, 수정, 삭제 작업 시 관련 캐시를 자동으로 무효화합니다:

```java
@CacheEvict(value = {"faqList", "faqDetail", "faqCategoryList", "faqSearchResults"}, allEntries = true)
public FAQResponseDto createFAQ(FAQCreateRequestDto requestDto) { ... }
```

## 보안

모든 FAQ 관리 API(생성, 수정, 삭제)는 관리자 권한을 가진 사용자만 접근할 수 있습니다. 권한 검사는 `userQueryService.checkAdminRole(email)` 메소드를 통해 수행됩니다. 인증 정보는 Spring Security의 SecurityContext에서 추출합니다.

## 구현 참고사항

### 1. 이미지 처리 로직

FAQ 이미지 처리는 이제 `FileUtils` 글로벌 유틸리티를 활용하며, `FAQCommandService`에서 HTML 처리 로직을 담당합니다:

- **processImagesAndUpdateHtml()**: 이미지 저장 및 HTML 내 이미지 경로 업데이트를 처리합니다.
- **updateHtmlImageSources()**: Jsoup을 사용하여 HTML 내용 내 이미지 소스를 안전하게 업데이트합니다.
- **deleteImages()**: 이미지 파일을 삭제합니다.

### 2. HTML 이미지 태그 처리 (Jsoup 활용)

```java
Document doc = Jsoup.parse(htmlContent);
Elements imgTags = doc.select("img[src^='data:image']");

for (Element img : imgTags) {
    if (imageIndex >= savedImages.size()) break;
    
    FAQImage image = savedImages.get(imageIndex++);
    String imageUrl = faq.getImageUrlPath(image.getImageUrl());
    img.attr("src", imageUrl);
}
```

### 3. 트랜잭션 및 예외 처리

FAQ 생성, 수정, 삭제 작업은 모두 트랜잭션으로 처리되어 일관성을 유지합니다. 예외 처리는 계층적으로 구성되어 있습니다:

1. **서비스 계층**: 발생한 예외를 적절한 도메인 예외로 변환합니다.
2. **컨트롤러 계층**: 예외를 그대로 전파합니다.
3. **글로벌 예외 처리기**: `GlobalExceptionHandler`에서 모든 예외를 일관되게 처리합니다.

### 4. 입력 검증 (Bean Validation)

DTO에 Bean Validation 어노테이션을 적용하여 입력 검증을 강화했습니다:

```java
@Data
public class FAQCreateRequestDto {
    @NotNull(message = "카테고리는 필수입니다.")
    private FAQCategory category;
    
    @NotBlank(message = "질문은 필수입니다.")
    @Size(max = 100, message = "질문은 100자 이내로 작성해주세요.")
    private String question;
    
    @NotBlank(message = "답변은 필수입니다.")
    private String answer;
    
    private List<MultipartFile> files = new ArrayList<>();
}
```

## 확장 가능성

1. **다국어 지원**: 질문과 답변을 여러 언어로 제공할 수 있도록 확장할 수 있습니다.
2. **태그 시스템**: FAQ에 태그를 추가하여 더 효율적인 분류와 검색이 가능하도록 할 수 있습니다.
3. **관련 FAQ 추천**: 현재 조회 중인 FAQ와 관련된 다른 FAQ를 추천하는 기능을 추가할 수 있습니다.
4. **인기 FAQ 관리**: 자주 조회되는 FAQ를 추적하고 상단에 노출하는 기능을 추가할 수 있습니다.
5. **이미지 최적화**: 이미지 리사이징, 압축 등의 최적화 기능을 추가할 수 있습니다.
6. **분산 캐싱**: 현재 로컬 캐시에서 Redis 같은 분산 캐시로 확장할 수 있습니다.
7. **캐시 만료 정책 개선**: 더 세밀한 캐시 만료 정책을 적용하여 성능과 데이터 일관성 사이의 균형을 개선할 수 있습니다.
8. **실시간 업데이트**: WebSocket을 활용하여 FAQ 업데이트를 실시간으로 클라이언트에 전파할 수 있습니다.