# 검수 기준 시스템 (Inspection Standard System)

## 개요

검수 기준 시스템은 상품 유형별 검수 기준과 정책을 관리하는 모듈입니다. 이 시스템은 카테고리별로 검수 기준을 관리하고, HTML 형식의 리치 텍스트 콘텐츠와 이미지를 포함한 상세한 검수 기준을 제공합니다. 관리자는 기준의 생성, 수정, 삭제를 수행할 수 있으며, 사용자는 카테고리별 검수 기준을 조회할 수 있습니다.

## 아키텍처

```
com.fream.back.domain.inspection/
├── controller/
│   ├── command/            # 검수 기준 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/              # 검수 기준 조회를 위한 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 검수 기준 생성, 수정, 삭제를 위한 서비스
    │   └── InspectionFileStorageUtil  # 파일 저장 유틸리티
    └── query/              # 검수 기준 조회를 위한 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **InspectionCommandController**: 검수 기준의 생성, 수정, 삭제 API를 제공합니다 (관리자 전용).
2. **InspectionQueryController**: 검수 기준 조회 및 이미지 파일 다운로드 API를 제공합니다.

### 서비스

1. **InspectionStandardCommandService**: 검수 기준 생성, 수정, 삭제 및 이미지 처리 로직을 담당합니다.
2. **InspectionStandardQueryService**: 검수 기준 조회 로직을 담당합니다.
3. **InspectionFileStorageUtil**: 검수 기준 이미지 파일의 저장, 업데이트, 삭제를 처리합니다.

### 엔티티

1. **InspectionStandard**: 검수 기준 기본 정보(카테고리, 내용)를 저장합니다.
2. **InspectionStandardImage**: 검수 기준과 연결된 이미지 정보를 저장합니다.
3. **InspectionCategory**: 검수 기준 카테고리 열거형(SHOES, OUTER, BAG, TECH, BEAUTY, PREMIUM_WATCH, PREMIUM_BAG).

### 저장소

1. **InspectionStandardRepository**: 검수 기준 엔티티의 기본 CRUD 및 조회 기능을 제공합니다.
2. **InspectionStandardRepositoryCustom**: 검수 기준 검색 기능을 위한 인터페이스입니다.
3. **InspectionStandardRepositoryImpl**: QueryDSL을 사용한 검색 구현체입니다.
4. **InspectionStandardImageRepository**: 검수 기준 이미지 엔티티의 CRUD 및 조회 기능을 제공합니다.

## API 엔드포인트

### 관리자 API (Command)

```
POST /inspections
```
새로운 검수 기준을 생성합니다. 이미지 첨부가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "category": "SHOES",
  "content": "<p>신발 검수 기준입니다.</p><p><img src='temp-img'/></p>",
  "files": [이미지 파일]
}
```

```
PUT /inspections/{id}
```
기존 검수 기준을 수정합니다. 이미지 추가/삭제가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "category": "SHOES",
  "content": "<p>신발 검수 기준이 업데이트되었습니다.</p><p><img src='https://www.pinjun.xyz/api/inspections/files/1/image.png'/></p>",
  "existingImageUrls": ["https://www.pinjun.xyz/api/inspections/files/1/image.png"],
  "newFiles": [새 이미지 파일]
}
```

```
DELETE /inspections/{id}
```
검수 기준을 삭제합니다. 연결된 이미지 파일도 함께 삭제됩니다.

### 일반 사용자 API (Query)

```
GET /inspections?category=SHOES&page=0&size=10
```
검수 기준 목록을 조회합니다. 카테고리 필터를 적용할 수 있으며, 페이징 처리가 가능합니다.

**응답 예시:**
```json
{
  "content": [
    {
      "id": 1,
      "category": "SHOES",
      "content": "<p>신발 검수 기준입니다.</p><p><img src='https://www.pinjun.xyz/api/inspections/files/1/image.png'/></p>",
      "imageUrls": ["inspection_1/image.png"]
    },
    // ... 추가 검수 기준 항목
  ],
  "pageable": { ... },
  "totalPages": 3,
  "totalElements": 25,
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
GET /inspections/{id}
```
단일 검수 기준을 조회합니다.

```
GET /inspections/files/{inspectionId}/{fileName}
```
검수 기준 이미지 파일을 다운로드합니다.

## 데이터베이스 스키마

### 1. INSPECTION_STANDARD 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| category       | VARCHAR        | 검수 카테고리(ENUM 문자열)     |
| content        | TEXT/CLOB      | 검수 기준 내용(HTML 형식)      |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. INSPECTION_STANDARD_IMAGE 테이블

| 필드                     | 타입           | 설명                        |
|-------------------------|----------------|---------------------------|
| id                      | BIGINT         | 기본 키                     |
| inspection_standard_id  | BIGINT         | 검수 기준 외래 키             |
| image_url               | VARCHAR        | 이미지 상대 경로              |

## 파일 저장 구조

### 경로 구조

- 기본 경로: `/home/ubuntu/fream/inspection/`
- 검수 기준별 하위 폴더: `inspection_{inspectionId}/`
- 파일 이름: UUID 기반 생성

### 예시

- 물리적 파일 경로: `/home/ubuntu/fream/inspection/inspection_1/550e8400-e29b-41d4-a716-446655440000.png`
- 데이터베이스 저장 경로: `inspection_1/550e8400-e29b-41d4-a716-446655440000.png`
- 웹 URL: `https://www.pinjun.xyz/api/inspections/files/1/550e8400-e29b-41d4-a716-446655440000.png`

## 주요 기능 상세

### 1. 리치 텍스트 내용 및 이미지 관리

검수 기준 시스템은 HTML 형식의 리치 텍스트 내용을 지원합니다. 내용에는 이미지를 포함할 수 있으며, 다음과 같은 이미지 처리 로직을 제공합니다:

1. **이미지 저장**:
    - 새 검수 기준 생성 또는 업데이트 시 이미지 파일을 서버에 저장합니다.
    - 이미지 파일은 `inspection_{id}` 하위 폴더에 UUID 기반 파일명으로 저장됩니다.

2. **이미지 경로 처리**:
    - HTML 내용에서 임시 이미지 태그를 실제 저장된 이미지 경로로 대체합니다.
    - 경로 형식: `https://www.pinjun.xyz/api/inspections/files/{inspectionId}/{fileName}`

3. **이미지 제거 처리**:
    - 검수 기준 수정 시 콘텐츠에서 제거된 이미지는 자동으로 파일 시스템에서 삭제됩니다.
    - 검수 기준 삭제 시 연결된 모든 이미지 파일을 삭제합니다.

### 2. 카테고리 기반 관리

검수 기준은 카테고리별로 관리됩니다. 현재 지원하는 카테고리는 다음과 같습니다:

- **SHOES**: 신발
- **OUTER**: 아우터
- **BAG**: 가방
- **TECH**: 테크
- **BEAUTY**: 뷰티
- **PREMIUM_WATCH**: 프리미엄 시계
- **PREMIUM_BAG**: 프리미엄 가방

### 3. 콘텐츠 검색

QueryDSL을 사용하여 콘텐츠 내용과 카테고리를 기준으로 검색을 지원합니다:
- 내용(content) 필드에서 키워드 검색
- 카테고리(category) 이름에서 키워드 검색

## 보안

모든 검수 기준 관리 API(생성, 수정, 삭제)는 관리자 권한을 가진 사용자만 접근할 수 있습니다. 권한 검사는 `userQueryService.checkAdminRole(email)` 메소드를 통해 수행됩니다.

## 구현 참고사항

### 1. 이미지 처리 로직

검수 기준 이미지 처리는 `InspectionFileStorageUtil` 클래스에서 담당합니다. 주요 메소드는 다음과 같습니다:

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

검수 기준 생성, 수정, 삭제 작업은 모두 트랜잭션으로 처리되어 일관성을 유지합니다. 파일 시스템 작업에 실패할 경우 트랜잭션이 롤백됩니다.

## 확장 가능성

1. **버전 관리**: 검수 기준의 변경 이력을 추적할 수 있는 버전 관리 기능을 추가할 수 있습니다.
2. **다국어 지원**: 검수 기준을 여러 언어로 제공할 수 있도록 확장할 수 있습니다.
3. **신규 카테고리 추가**: 새로운 상품 카테고리가 추가될 때 InspectionCategory 열거형을 확장할 수 있습니다.
4. **이미지 최적화**: 이미지 업로드 시 리사이징, 압축 등의 최적화 기능을 추가할 수 있습니다.
5. **승인 워크플로우**: 검수 기준 변경에 대한 승인 프로세스를 추가하여 여러 관리자의 검토 후 변경 사항이 적용되도록 할 수 있습니다.