# 1대1 문의 시스템 (Inquiry System)

## 개요

1대1 문의 시스템은 사용자가 관리자에게 직접 문의할 수 있는 기능을 제공하는 모듈입니다. 이 시스템은 사용자가 문의를 생성, 수정, 삭제할 수 있으며, 관리자는 이에 대한 답변을 작성할 수 있습니다. 문의는 카테고리별로 분류되며, 비공개 문의 기능도 지원합니다. CQRS 패턴에 따라 명령(Command)과 조회(Query) 작업이 분리되어 있어 효율적인 리소스 관리가 가능합니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.inquiry/
├── controller/
│   ├── command/          # 문의 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/            # 문의 조회를 위한 컨트롤러
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티
├── exception/            # 도메인 관련 예외 클래스
├── repository/           # 데이터 접근 계층
└── service/
    ├── command/          # 문의 생성, 수정, 삭제를 위한 서비스
    └── query/            # 문의 조회를 위한 서비스
```

## 주요 구성 요소

### Controller

1. **InquiryCommandController**: 문의 생성, 수정, 삭제, 답변 등의 명령 작업을 처리합니다.
2. **InquiryQueryController**: 문의 목록 조회, 개별 문의 조회, 이미지 다운로드 등의 조회 작업을 처리합니다.

### Service

1. **InquiryCommandService**: 문의 생성, 수정, 삭제, 답변 등의 비즈니스 로직을 처리합니다.
2. **InquiryQueryService**: 문의 조회와 관련된 비즈니스 로직을 처리합니다.

### Repository

1. **InquiryRepository**: JPA를 이용한 데이터베이스 접근 인터페이스로, 문의 조회, 생성, 수정, 삭제 기능을 제공합니다.
2. **InquiryImageRepository**: 문의에 첨부된 이미지 정보에 접근하는 인터페이스입니다.
3. **InquiryRepositoryCustom**: QueryDSL을 활용한 복잡한 조회 쿼리를 정의하는 인터페이스입니다.
4. **InquiryRepositoryImpl**: CustomRepository 인터페이스의 구현체로, QueryDSL을 사용하여 다양한 검색 조건에 따른 문의 조회 기능을 제공합니다.

### Entity

1. **Inquiry**: 문의 정보를 저장하는 JPA 엔티티입니다. `BaseTimeEntity`를 상속받아 생성/수정 시간 정보를 자동으로 관리합니다.
2. **InquiryImage**: 문의에 첨부된 이미지 정보를 저장하는 엔티티입니다.
3. **InquiryStatus**: 문의 상태를 나타내는 Enum 클래스입니다. (REQUESTED, CONFIRMED, ANSWERED)
4. **InquiryCategory**: 문의 카테고리를 나타내는 Enum 클래스입니다. (PRODUCT, ORDER, DELIVERY, RETURN, ACCOUNT, ETC)

### DTO

1. **InquiryCreateRequestDto**: 문의 생성 요청에 사용되는 DTO입니다.
2. **InquiryUpdateRequestDto**: 문의 수정 요청에 사용되는 DTO입니다.
3. **InquiryAnswerRequestDto**: 문의 답변 작성 요청에 사용되는 DTO입니다.
4. **InquiryResponseDto**: 문의 정보 응답에 사용되는 DTO입니다.
5. **InquirySearchResultDto**: 문의 검색 결과 응답에 사용되는 DTO입니다.
6. **InquirySearchCondition**: 문의 검색 조건을 정의하는 DTO입니다.

### Exception

1. **InquiryException**: 문의 관련 모든 예외의 기본 클래스입니다.
2. **InquiryNotFoundException**: 문의를 찾을 수 없을 때 발생하는 예외입니다.
3. **InquiryErrorCode**: 문의 관련 에러 코드를 정의하는 Enum입니다.

## 주요 기능

### 문의 생성

```
POST /inquiry
```

로그인한 사용자는 제목, 내용, 카테고리와 함께 문의를 생성할 수 있습니다. 비공개 여부와 답변 시 알림 수신 여부도 설정할 수 있으며, 이미지 파일도 첨부할 수 있습니다.

**요청 본문 예시 (Form-Data):**
```
title: 상품 관련 문의드립니다
content: <p>구매한 상품에 대해 문의가 있습니다.</p><p>...</p>
category: PRODUCT
isPrivate: true
pushNotification: true
files: [파일1.jpg, 파일2.png]
```

### 문의 수정

```
PUT /inquiry/{inquiryId}
```

문의 ID와 함께 수정할 정보를 전송하여 기존 문의를 수정합니다. 답변이 완료된 문의는 수정할 수 없습니다.

**요청 본문 예시 (Form-Data):**
```
title: 상품 관련 추가 문의드립니다
content: <p>구매한 상품에 대해 추가 문의가 있습니다.</p><p>...</p>
category: PRODUCT
isPrivate: true
pushNotification: true
newFiles: [파일3.jpg]
retainedImageUrls: [/inquiry/files/1/img_abc123.jpg]
```

### 문의 삭제

```
DELETE /inquiry/{inquiryId}
```

지정된 ID의 문의를 삭제합니다. 문의 작성자 본인 또는 관리자만 삭제할 수 있습니다.

### 문의 목록 조회

```
GET /inquiry
```

관리자는 모든 문의 목록을 조회할 수 있으며, 상태/카테고리/키워드별 필터링이 가능합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "message": "문의 목록 조회가 완료되었습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "상품 관련 문의드립니다",
        "content": "<p>구매한 상품에 대해 문의가 있습니다.</p><p>...</p>",
        "answer": "<p>안녕하세요. 문의 주셔서 감사합니다.</p><p>...</p>",
        "status": "ANSWERED",
        "category": "PRODUCT",
        "isPrivate": true,
        "createdDate": "2025-05-14T10:30:00",
        "modifiedDate": "2025-05-14T11:25:00",
        "userId": 123,
        "email": "user@example.com",
        "profileName": "홍길동",
        "name": "길동",
        "imageUrls": ["/inquiry/files/1/img_abc123.jpg"]
      },
      // ... 추가 문의 목록
    ],
    "pageable": {
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "pageNumber": 0,
      "pageSize": 10,
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 5,
    "totalElements": 42,
    "last": false,
    "first": true,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "number": 0,
    "numberOfElements": 10,
    "size": 10,
    "empty": false
  }
}
```

### 내 문의 목록 조회

```
GET /inquiry/my
```

로그인한 사용자의 문의 목록을 조회합니다.

### 특정 문의 조회

```
GET /inquiry/{inquiryId}
```

지정된 ID의 문의 정보를 조회합니다. 비공개 문의는 작성자 본인 또는 관리자만 조회할 수 있습니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "message": "문의 조회가 완료되었습니다.",
  "data": {
    "id": 1,
    "title": "상품 관련 문의드립니다",
    "content": "<p>구매한 상품에 대해 문의가 있습니다.</p><p>...</p>",
    "answer": "<p>안녕하세요. 문의 주셔서 감사합니다.</p><p>...</p>",
    "status": "ANSWERED",
    "category": "PRODUCT",
    "isPrivate": true,
    "pushNotification": true,
    "answeredBy": "관리자01",
    "createdDate": "2025-05-14T10:30:00",
    "modifiedDate": "2025-05-14T11:25:00",
    "userId": 123,
    "userEmail": "user@example.com",
    "userProfileName": "홍길동",
    "userName": "길동",
    "questionImageUrls": ["/inquiry/files/1/img_abc123.jpg"],
    "answerImageUrls": ["/inquiry/files/1/img_def456.jpg"]
  }
}
```

### 문의 답변

```
POST /inquiry/{inquiryId}/answer
```

관리자는 문의에 대한 답변을 작성할 수 있습니다. 이미지 파일도 첨부할 수 있습니다.

**요청 본문 예시 (Form-Data):**
```
answer: <p>안녕하세요. 문의 주셔서 감사합니다.</p><p>...</p>
answeredBy: 관리자01
files: [파일1.jpg]
```

### 문의 상태 변경

```
PUT /inquiry/{inquiryId}/status
```

관리자는 문의의 상태를 변경할 수 있습니다.

**요청 파라미터:**
```
status: CONFIRMED
```

### 문의 이미지 다운로드

```
GET /inquiry/files/{inquiryId}/{fileName}
```

문의에 첨부된 이미지를 다운로드합니다. 비공개 문의의 이미지는 작성자 본인 또는 관리자만 다운로드할 수 있습니다.

## 데이터베이스 스키마

### inquiries 테이블

| 필드             | 타입         | 제약조건                      | 설명                           |
|-----------------|--------------|------------------------------|-------------------------------|
| id              | BIGINT       | PK, AUTO_INCREMENT           | 기본 키                        |
| user_id         | BIGINT       | FK, NOT NULL                 | 사용자 외래 키 (다대일 관계)     |
| title           | VARCHAR(100) | NOT NULL                     | 문의 제목                      |
| content         | TEXT         | NOT NULL                     | 문의 내용 (HTML 형식)           |
| answer          | TEXT         |                              | 관리자 답변 (HTML 형식)         |
| status          | VARCHAR      | NOT NULL                     | 문의 상태 (REQUESTED, CONFIRMED, ANSWERED) |
| category        | VARCHAR      | NOT NULL                     | 문의 카테고리                   |
| is_private      | BOOLEAN      | DEFAULT FALSE                | 비공개 여부                    |
| push_notification | BOOLEAN    | DEFAULT TRUE                 | 답변 시 알림 수신 여부           |
| answered_by     | VARCHAR      |                              | 답변자 정보                    |
| created_date    | TIMESTAMP    | NOT NULL                     | 생성 시간                      |
| modified_date   | TIMESTAMP    |                              | 수정 시간                      |

### inquiry_images 테이블

| 필드             | 타입         | 제약조건                      | 설명                           |
|-----------------|--------------|------------------------------|-------------------------------|
| id              | BIGINT       | PK, AUTO_INCREMENT           | 기본 키                        |
| inquiry_id      | BIGINT       | FK, NOT NULL                 | 문의 외래 키 (다대일 관계)       |
| image_url       | VARCHAR      | NOT NULL                     | 이미지 URL (저장된 파일명)       |
| original_file_name | VARCHAR   |                              | 원본 파일명                    |
| file_size       | VARCHAR      |                              | 파일 크기                      |
| is_answer       | BOOLEAN      | NOT NULL                     | 답변 이미지 여부                |
| created_date    | TIMESTAMP    | NOT NULL                     | 생성 시간                      |
| modified_date   | TIMESTAMP    |                              | 수정 시간                      |

## 유효성 검증

문의 생성 및 수정 시 다음과 같은 유효성 검증이 적용됩니다:

### InquiryCreateRequestDto

| 필드            | 검증 어노테이션                           | 오류 메시지                                   |
|----------------|-----------------------------------------|----------------------------------------------|
| title          | @NotBlank, @Size(min=2, max=100)        | "문의 제목은 필수 입력 항목입니다.", "제목은 2자 이상 100자 이하로 입력해주세요." |
| content        | @NotBlank, @Size(min=10)                | "문의 내용은 필수 입력 항목입니다.", "내용은 10자 이상 입력해주세요." |
| category       | @NotNull                                | "문의 카테고리는 필수 선택 항목입니다."             |

### InquiryUpdateRequestDto

| 필드            | 검증 어노테이션                           | 오류 메시지                                   |
|----------------|-----------------------------------------|----------------------------------------------|
| title          | @NotBlank, @Size(min=2, max=100)        | "문의 제목은 필수 입력 항목입니다.", "제목은 2자 이상 100자 이하로 입력해주세요." |
| content        | @NotBlank, @Size(min=10)                | "문의 내용은 필수 입력 항목입니다.", "내용은 10자 이상 입력해주세요." |
| category       | @NotNull                                | "문의 카테고리는 필수 선택 항목입니다."             |

### InquiryAnswerRequestDto

| 필드            | 검증 어노테이션                           | 오류 메시지                                   |
|----------------|-----------------------------------------|----------------------------------------------|
| answer         | @NotBlank, @Size(min=10)                | "답변 내용은 필수 입력 항목입니다.", "답변 내용은 10자 이상 입력해주세요." |
| answeredBy     | @NotBlank                               | "답변자 정보는 필수 입력 항목입니다."               |

## 보안

모든 문의 관련 작업은 인증된 사용자만 수행할 수 있으며, `SecurityUtils.extractEmailFromSecurityContext()`를 통해 현재 로그인한 사용자의 이메일을 추출합니다. 관리자 권한이 필요한 기능은 `UserQueryService.checkAdminRole()`을 통해 권한을 검증합니다. 비공개 문의는 작성자 본인과 관리자만 조회할 수 있습니다.

## 구현 참고사항

### 이미지 처리

문의 및 답변 작성 시 HTML 에디터를 통해 작성된 내용에 이미지가 포함될 수 있습니다. 이미지는 다음과 같은 방식으로 처리됩니다:

1. 에디터에서 이미지가 포함된 경우, 이미지는 Base64 인코딩된 데이터 URL 형식(data:image/...)으로 전송됩니다.
2. 서버에서는 HTML 내용을 파싱하여 데이터 URL을 찾고, 이를 실제 파일로 저장합니다.
3. 저장된 이미지 파일은 `/inquiry/files/{inquiryId}/{fileName}` 경로를 통해 접근할 수 있습니다.
4. HTML 내용 중 데이터 URL은 실제 이미지 URL로 대체됩니다.

### 편의 메서드

`Inquiry` 엔티티에는 다음과 같은 편의 메서드가 구현되어 있습니다:

1. **setAnswer**: 문의 답변 및 답변자 정보를 설정하고, 문의 상태를 ANSWERED로 변경합니다.
   ```java
   public void setAnswer(String answer, String answeredBy)
   ```

2. **updateStatus**: 문의 상태를 변경합니다.
   ```java
   public void updateStatus(InquiryStatus status)
   ```

3. **updateInquiry**: 문의 정보(제목, 내용, 카테고리, 비공개 여부, 알림 수신 여부)를 업데이트합니다. 답변된 문의는 수정할 수 없습니다.
   ```java
   public void updateInquiry(String title, String content, InquiryCategory category, boolean isPrivate, boolean pushNotification)
   ```

4. **getFileDirectory**: 문의 파일이 저장될 디렉토리 경로를 반환합니다.
   ```java
   public String getFileDirectory()
   ```

5. **getImageUrlPath**: 이미지 URL 경로를 반환합니다.
   ```java
   public String getImageUrlPath(String fileName)
   ```

### 예외 처리

서비스에서는 다음과 같은 예외 상황을 처리합니다:

| 예외 클래스                  | 상황                                | 오류 코드                   | HTTP 상태 |
|-----------------------------|------------------------------------|-----------------------------|-----------|
| InquiryNotFoundException    | 존재하지 않는 문의 ID 요청           | INQUIRY_NOT_FOUND          | 404       |
| InquiryException            | 문의 관련 일반 예외                 | 다양                        | 다양      |
| InquiryAccessDeniedException| 다른 사용자의 문의에 접근 시도       | INQUIRY_ACCESS_DENIED      | 403       |
| InquiryInvalidInput         | 유효하지 않은 문의 데이터 입력       | INQUIRY_INVALID_INPUT      | 400       |
| InquiryAlreadyAnswered      | 이미 답변된 문의를 수정하려는 시도   | INQUIRY_ALREADY_ANSWERED   | 400       |

## 향후 개선 사항

향후 다음과 같은 기능을 추가할 수 있습니다:

1. **문의 템플릿**: 자주 묻는 문의에 대한 템플릿 제공
2. **FAQ 연동**: 유사한 FAQ 자동 추천 기능
3. **메일 알림**: 문의 답변 시 이메일 알림 발송
4. **채팅 연동**: 실시간 채팅 상담 연동
5. **다국어 지원**: 다양한 언어로 문의 및 답변 지원