# FAQ 시스템 (Frequently Asked Questions)

## 개요

FAQ 시스템은 자주 묻는 질문과 답변을 관리하기 위한 모듈입니다. 이 시스템은 카테고리별 FAQ 관리, 이미지가 포함된 리치 텍스트 답변 지원, 검색 기능 등을 제공합니다. 관리자는 FAQ의 생성, 수정, 삭제 작업을 수행할 수 있으며, 일반 사용자는 FAQ 조회와 검색 기능을 이용할 수 있습니다.

## 아키텍처

```
com.fream.back.domain.faq/
├── controller/
│   ├── command/            # FAQ 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/              # FAQ 조회 및 검색을 위한 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # FAQ 생성, 수정, 삭제를 위한 서비스
    │   └── FAQFileStorageUtil  # 파일 저장 유틸리티
    └── query/              # FAQ 조회 및 검색을 위한 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **FAQCommandController**: FAQ 생성, 수정, 삭제 API를 제공합니다 (관리자 전용).
2. **FAQQueryController**: FAQ 조회, 검색, 파일 다운로드 API를 제공합니다.

### 서비스

1. **FAQCommandService**: FAQ 생성, 수정, 삭제 및 이미지 처리 로직을 담당합니다.
2. **FAQQueryService**: FAQ 조회 및 검색 로직을 담당합니다.
3. **FAQFileStorageUtil**: FAQ 이미지 파일의 저장, 업데이트, 삭제를 처리합니다.

### 엔티티

1. **FAQ**: FAQ 기본 정보(카테고리, 질문, 답변)를 저장합니다.
2. **FAQImage**: FAQ와 연결된 이미지 정보를 저장합니다.
3. **FAQCategory**: FAQ 카테고리 열거형(POLICY, GENERAL, BUYING, SELLING).

### 저장소

1. **FAQRepository**: FAQ 엔티티의 기본 CRUD 및 조회 기능을 제공합니다.
2. **FAQRepositoryCustom**: FAQ 검색 기능을 위한 인터페이스입니다.
3. **FAQRepositoryImpl**: QueryDSL을 사용한 FAQ 검색 구현체입니다.
4. **FAQImageRepository**: FAQ 이미지 엔티티의 CRUD 및 조회 기능을 제공합니다.

## API 엔드포인트

### 관리자 API (Command)

```
POST /faq
```
새로운 FAQ를 생성합니다. 이미지 첨부가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "category": "BUYING",
  "question": "주문 취소는 어떻게 하나요?",
  "answer": "<p>주문 취소는 <b>마이페이지</b>에서 가능합니다.</p><p><img src='temp-img'/></p>",
  "files": [이미지 파일]
}
```

```
PUT /faq/{id}
```
기존 FAQ를 수정합니다. 이미지 추가/삭제가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "category": "BUYING",
  "question": "주문 취소는 어떻게 하나요?",
  "answer": "<p>주문 취소는 <b>마이페이지 > 주문내역</b>에서 가능합니다.</p><p><img src='https://www.pinjun.xyz/api/faq/files/1/image.png'/></p>",
  "existingImageUrls": ["https://www.pinjun.xyz/api/faq/files/1/image.png"],
  "newFiles": [새 이미지 파일]
}
```

```
DELETE /faq/{id}
```
FAQ를 삭제합니다. 연결된 이미지 파일도 함께 삭제됩니다.

### 일반 사용자 API (Query)

```
GET /faq?category=BUYING&page=0&size=10
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
      "answer": "<p>주문 취소는 <b>마이페이지 > 주문내역</b>에서 가능합니다.</p><p><img src='https://www.pinjun.xyz/api/faq/files/1/image.png'/></p>",
      "imageUrls": ["faq_1/image.png"]
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
GET /faq/{id}
```
단일 FAQ를 조회합니다.

```
GET /faq/search?keyword=취소&page=0&size=10
```
FAQ를 검색합니다. 질문과 답변 내용에서 키워드를 검색합니다.

```
GET /faq/files/{faqId}/{fileName}
```
FAQ 이미지 파일을 다운로드합니다.

## 데이터베이스 스키마

### 1. FAQ 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| category       | VARCHAR        | FAQ 카테고리(ENUM 문자열)     |
| question       | VARCHAR        | 질문                        |
| answer         | TEXT/CLOB      | 답변(HTML 형식)              |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. FAQ_IMAGE 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| faq_id         | BIGINT         | FAQ 외래 키                 |
| image_url      | VARCHAR        | 이미지 상대 경로              |

## 파일 저장 구조

### 경로 구조

- 기본 경로: `/home/ubuntu/fream/faq/`
- FAQ 별 하위 폴더: `faq_{faqId}/`
- 파일 이름: UUID 기반 생성

### 예시

- 물리적 파일 경로: `/home/ubuntu/fream/faq/faq_1/550e8400-e29b-41d4-a716-446655440000.png`
- 데이터베이스 저장 경로: `faq_1/550e8400-e29b-41d4-a716-446655440000.png`
- 웹 URL: `https://www.pinjun.xyz/api/faq/files/1/550e8400-e29b-41d4-a716-446655440000.png`

## 주요 기능 상세

### 1. 리치 텍스트 답변 및 이미지 관리

FAQ 시스템은 HTML 형식의 리치 텍스트 답변을 지원합니다. 답변에는 이미지를 포함할 수 있으며, 다음과 같은 이미지 처리 로직을 제공합니다:

1. **이미지 저장**:
    - 새 FAQ 생성 또는 업데이트 시 이미지 파일을 서버에 저장합니다.
    - 이미지 파일은 `faq_{id}` 하위 폴더에 UUID 기반 파일명으로 저장됩니다.

2. **이미지 경로 처리**:
    - 임시 이미지 태그(`<img src='temp-img'/>`)를 실제 저장된 이미지 경로로 대체합니다.
    - 경로 형식: `https://www.pinjun.xyz/api/faq/files/{faqId}/{fileName}`

3. **이미지 제거 처리**:
    - FAQ 수정 시 콘텐츠에서 제거된 이미지는 자동으로 삭제됩니다.
    - FAQ 삭제 시 연결된 모든 이미지 파일을 삭제합니다.

### 2. 카테고리 기반 관리

FAQ는 카테고리별로 관리됩니다. 현재 지원하는 카테고리는 다음과 같습니다:

- **POLICY**: 이용 정책
- **GENERAL**: 공통
- **BUYING**: 구매
- **SELLING**: 판매

### 3. 전체 키워드 검색

FAQ 검색 기능은 질문(question)과 답변(answer) 필드에서 키워드를 검색합니다. QueryDSL을 사용하여 효율적인 검색을 구현하였습니다.

## 보안

모든 FAQ 관리 API(생성, 수정, 삭제)는 관리자 권한을 가진 사용자만 접근할 수 있습니다. 권한 검사는 `userQueryService.checkAdminRole(email)` 메소드를 통해 수행됩니다.

## 구현 참고사항

### 1. 이미지 처리 로직

FAQ 이미지 처리는 `FAQFileStorageUtil` 클래스에서 담당합니다. 주요 메소드는 다음과 같습니다:

- **saveFiles()**: 여러 이미지 파일을 저장합니다.
- **updateImagePaths()**: HTML 내용 내 이미지 경로를 업데이트합니다.
- **extractImagePaths()**: HTML 내용에서 이미지 경로를 추출합니다.
- **deleteFiles()**: 이미지 파일을 삭제합니다.

### 2. HTML 이미지 태그 처리

HTML 내 이미지 태그는 정규식을 사용하여 처리합니다:

```java
String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
Matcher matcher = Pattern.compile(regex).matcher(content);
```

### 3. 트랜잭션 처리

FAQ 생성, 수정, 삭제 작업은 모두 트랜잭션으로 처리되어 일관성을 유지합니다. 파일 시스템 작업에 실패할 경우 트랜잭션이 롤백됩니다.

## 확장 가능성

1. **다국어 지원**: 질문과 답변을 여러 언어로 제공할 수 있도록 확장할 수 있습니다.
2. **태그 시스템**: FAQ에 태그를 추가하여 더 효율적인 분류와 검색이 가능하도록 할 수 있습니다.
3. **관련 FAQ 추천**: 현재 조회 중인 FAQ와 관련된 다른 FAQ를 추천하는 기능을 추가할 수 있습니다.
4. **인기 FAQ 관리**: 자주 조회되는 FAQ를 추적하고 상단에 노출하는 기능을 추가할 수 있습니다.
5. **이미지 최적화**: 이미지 리사이징, 압축 등의 최적화 기능을 추가할 수 있습니다.