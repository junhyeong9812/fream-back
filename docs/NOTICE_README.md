# 공지사항 시스템 (Notice System)

## 개요

공지사항 시스템은 플랫폼의 중요 소식, 이벤트, 서비스 안내 등을 사용자에게 전달하기 위한 모듈입니다. 이 시스템은 카테고리별 공지사항 관리, 리치 텍스트 내용과 이미지/비디오 첨부 기능, 검색 기능 등을 제공합니다. 관리자는 공지사항의 생성, 수정, 삭제 작업을 수행할 수 있으며, 새로운 공지사항이 등록되면 모든 사용자에게 알림을 발송하는 기능도 포함되어 있습니다.

## 아키텍처

```
com.fream.back.domain.notice/
├── controller/
│   ├── command/            # 공지사항 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/              # 공지사항 조회 및 검색을 위한 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 공지사항 생성, 수정, 삭제를 위한 서비스
    │   └── NoticeFileStorageUtil  # 파일 저장 유틸리티
    └── query/              # 공지사항 조회 및 검색을 위한 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **NoticeCommandController**: 공지사항 생성, 수정, 삭제 API를 제공합니다 (관리자 전용).
2. **NoticeQueryController**: 공지사항 조회, 검색, 파일 다운로드 API를 제공합니다.

### 서비스

1. **NoticeCommandService**: 공지사항 생성, 수정, 삭제 및 이미지 처리 로직을 담당합니다.
2. **NoticeQueryService**: 공지사항 조회 및 검색 로직을 담당합니다.
3. **NoticeFileStorageUtil**: 공지사항 이미지/비디오 파일의 저장, 업데이트, 삭제를 처리합니다.

### 엔티티

1. **Notice**: 공지사항 기본 정보(제목, 내용, 카테고리)를 저장합니다.
2. **NoticeImage**: 공지사항과 연결된 이미지/비디오 정보를 저장합니다.
3. **NoticeCategory**: 공지사항 카테고리 열거형(ANNOUNCEMENT, EVENT, SERVICE, OTHERS).

### 저장소

1. **NoticeRepository**: 공지사항 엔티티의 기본 CRUD 및 조회 기능을 제공합니다.
2. **NoticeRepositoryCustom**: 공지사항 검색 기능을 위한 인터페이스입니다.
3. **NoticeRepositoryImpl**: QueryDSL을 사용한 공지사항 검색 구현체입니다.
4. **NoticeImageRepository**: 공지사항 이미지 엔티티의 CRUD 및 조회 기능을 제공합니다.

## API 엔드포인트

### 관리자 API (Command)

```
POST /notices
```
새로운 공지사항을 생성합니다. 이미지 첨부가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "title": "시스템 점검 안내",
  "content": "<p>서비스 개선을 위한 시스템 점검이 예정되어 있습니다.</p><p><img src='temp-img'/></p>",
  "category": "ANNOUNCEMENT",
  "files": [이미지 파일]
}
```

```
PUT /notices/{noticeId}
```
기존 공지사항을 수정합니다. 이미지 추가/삭제가 가능합니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "title": "시스템 점검 일정 변경 안내",
  "content": "<p>서비스 개선을 위한 시스템 점검 일정이 변경되었습니다.</p><p><img src='https://www.pinjun.xyz/api/notices/files/1/image.png'/></p>",
  "category": "ANNOUNCEMENT",
  "existingImageUrls": ["https://www.pinjun.xyz/api/notices/files/1/image.png"],
  "newFiles": [새 이미지 파일]
}
```

```
DELETE /notices/{noticeId}
```
공지사항을 삭제합니다. 연결된 이미지/비디오 파일도 함께 삭제됩니다.

### 일반 사용자 API (Query)

```
GET /notices?category=ANNOUNCEMENT&page=0&size=10
```
공지사항 목록을 조회합니다. 카테고리 필터를 적용할 수 있으며, 페이징 처리가 가능합니다.

**응답 예시:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "시스템 점검 안내",
      "content": "<p>서비스 개선을 위한 시스템 점검이 예정되어 있습니다.</p><p><img src='https://www.pinjun.xyz/api/notices/files/1/image.png'/></p>",
      "category": "ANNOUNCEMENT",
      "createdDate": "2023-06-15T14:30:45",
      "imageUrls": ["notice_1/image.png"]
    },
    // ... 추가 공지사항 항목
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
GET /notices/{noticeId}
```
단일 공지사항을 조회합니다.

```
GET /notices/search?keyword=점검&page=0&size=10
```
공지사항을 검색합니다. 제목과 내용에서 키워드를 검색합니다.

```
GET /notices/files/{noticeId}/{fileName}
```
공지사항에 첨부된 이미지/비디오 파일을 다운로드합니다.

## 데이터베이스 스키마

### 1. NOTICE 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| category       | VARCHAR        | 공지사항 카테고리(ENUM 문자열)  |
| title          | VARCHAR        | 제목                        |
| content        | TEXT/CLOB      | 내용(HTML 형식)              |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. NOTICE_IMAGE 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| notice_id      | BIGINT         | 공지사항 외래 키              |
| image_url      | VARCHAR        | 이미지 상대 경로              |
| is_video       | BOOLEAN        | 비디오 여부                  |

## 파일 저장 구조

### 경로 구조

- 기본 경로: `/home/ubuntu/fream/notice/`
- 공지사항별 하위 폴더: `notice_{noticeId}/`
- 파일 이름: UUID 기반 생성

### 예시

- 물리적 파일 경로: `/home/ubuntu/fream/notice/notice_1/550e8400-e29b-41d4-a716-446655440000.png`
- 데이터베이스 저장 경로: `notice_1/550e8400-e29b-41d4-a716-446655440000.png`
- 웹 URL: `https://www.pinjun.xyz/api/notices/files/1/550e8400-e29b-41d4-a716-446655440000.png`

## 주요 기능 상세

### 1. 리치 텍스트 내용 및 멀티미디어 관리

공지사항 시스템은 HTML 형식의 리치 텍스트 내용을 지원합니다. 내용에는 이미지와 비디오를 포함할 수 있으며, 다음과 같은 멀티미디어 처리 로직을 제공합니다:

1. **파일 저장**:
    - 새 공지사항 생성 또는 업데이트 시 파일을 서버에 저장합니다.
    - 파일은 `notice_{id}` 하위 폴더에 UUID 기반 파일명으로 저장됩니다.

2. **이미지 경로 처리**:
    - HTML 내용에서 임시 이미지 태그를 실제 저장된 이미지 경로로 대체합니다.
    - 경로 형식: `https://www.pinjun.xyz/api/notices/files/{noticeId}/{fileName}`

3. **파일 타입 인식**:
    - 파일 확장자를 기반으로 이미지와 비디오를 구분합니다.
    - 비디오는 `.mp4`, `.avi`, `.mov` 등의 확장자를 가진 파일로 인식됩니다.

4. **파일 제거 처리**:
    - 공지사항 수정 시 콘텐츠에서 제거된 이미지는 자동으로 파일 시스템에서 삭제됩니다.
    - 공지사항 삭제 시 연결된 모든 파일을 삭제합니다.

### 2. 카테고리 기반 관리

공지사항은 카테고리별로 관리됩니다. 현재 지원하는 카테고리는 다음과 같습니다:

- **ANNOUNCEMENT**: 일반 공지
- **EVENT**: 이벤트
- **SERVICE**: 서비스 안내
- **OTHERS**: 기타

### 3. 알림 연동

새로운 공지사항이 생성되면 모든 사용자에게 알림이 발송됩니다:

1. 알림 내용에는 공지사항 제목이 포함됩니다.
2. 알림은 `NotificationCommandService`를 통해 생성됩니다.
3. 알림 카테고리는 `SHOPPING`, 타입은 `ANNOUNCEMENT`로 설정됩니다.

### 4. 검색 기능

QueryDSL을 사용하여 공지사항의 제목과 내용에서 키워드 검색을 지원합니다:
- 제목(title) 필드에서 키워드 검색
- 내용(content) 필드에서 키워드 검색

## 보안

모든 공지사항 관리 API(생성, 수정, 삭제)는 관리자 권한을 가진 사용자만 접근할 수 있습니다. 권한 검사는 `userQueryService.checkAdminRole(email)` 메소드를 통해 수행됩니다.

## 구현 참고사항

### 1. 파일 처리 로직

공지사항 파일 처리는 `NoticeFileStorageUtil` 클래스에서 담당합니다. 주요 메소드는 다음과 같습니다:

- **saveFiles()**: 여러 파일을 저장합니다.
- **updateImagePaths()**: HTML 내용 내 이미지 경로를 업데이트합니다.
- **extractImagePaths()**: HTML 내용에서 이미지 경로를 추출합니다.
- **deleteFiles()**: 파일을 삭제합니다.
- **isVideo()**: 파일이 비디오인지 확인합니다.

### 2. HTML 이미지 태그 처리

HTML 내 이미지 태그는 정규식을 사용하여 처리합니다:

```java
String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
Pattern pattern = Pattern.compile(regex);
Matcher matcher = pattern.matcher(content);
```

### 3. 트랜잭션 처리

공지사항 생성, 수정, 삭제 작업은 모두 트랜잭션으로 처리되어 일관성을 유지합니다. 파일 시스템 작업에 실패할 경우 트랜잭션이 롤백됩니다.

## 확장 가능성

1. **공지사항 중요도 설정**: 중요 공지사항을 상단에 고정하거나 강조 표시하는 기능을 추가할 수 있습니다.
2. **공지사항 예약 발행**: 미래 특정 시점에 공지사항이 자동으로 게시되는 예약 발행 기능을 구현할 수 있습니다.
3. **공지사항 조회수 추적**: 각 공지사항의 조회수를 기록하여 사용자 관심도를 측정할 수 있습니다.
4. **다국어 지원**: 공지사항을 여러 언어로 제공할 수 있도록 확장할 수 있습니다.
5. **타겟팅된 알림**: 특정 사용자 그룹이나 관심사에 따라 맞춤형 공지사항을 발송할 수 있는 기능을 추가할 수 있습니다.