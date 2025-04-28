# 검수 기준 시스템 (Inspection Standard System)

## 개요

검수 기준 시스템은 상품 유형별 검수 기준과 정책을 관리하는 모듈입니다. 이 시스템은 카테고리별로 검수 기준을 관리하고, HTML 형식의 리치 텍스트 콘텐츠와 이미지를 포함한 상세한 검수 기준을 제공합니다. 관리자는 기준의 생성, 수정, 삭제를 수행할 수 있으며, 사용자는 카테고리별 검수 기준을 조회할 수 있습니다.

## 주요 개선사항

- **캐싱 시스템**: 성능 향상을 위한 캐싱 적용
- **유효성 검증**: Jakarta Validation을 사용한 입력 데이터 유효성 검증 강화
- **보안 강화**: 파일 접근 및 관리자 권한 검증 로직 개선
- **예외 처리**: 도메인별 예외 처리 체계 구축 및 일관된 에러 코드 적용
- **N+1 문제 해결**: JPQL과 QueryDSL을 활용한 성능 최적화
- **코드 리팩토링**: 중복 코드 제거 및 메소드 분리로 유지보수성 향상

## 아키텍처

```
com.fream.back.domain.inspection/
├── controller/
│   ├── command/            # 검수 기준 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/              # 검수 기준 조회를 위한 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── exception/              # 도메인 특화 예외 클래스
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 검수 기준 생성, 수정, 삭제를 위한 서비스
    └── query/              # 검수 기준 조회를 위한 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **InspectionCommandController**: 검수 기준의 생성, 수정, 삭제 API를 제공합니다 (관리자 전용).
    - 캐시 무효화 처리 추가
    - Jakarta Validation 기반 유효성 검증 적용
    - 관리자 권한 검증 로직 강화

2. **InspectionQueryController**: 검수 기준 조회 및 이미지 파일 다운로드 API를 제공합니다.
    - 캐싱 적용으로 성능 향상
    - 경로 탐색 방지 등 파일 보안 강화
    - 통합 검색 기능 지원

### 서비스

1. **InspectionStandardCommandService**: 검수 기준 생성, 수정, 삭제 및 이미지 처리 로직을 담당합니다.
    - 트랜잭션 처리 강화
    - FileUtils를 활용한 파일 처리
    - 예외 처리 일관성 유지

2. **InspectionStandardQueryService**: 검수 기준 조회 로직을 담당합니다.
    - Spring Cache를 활용한 성능 최적화
    - BaseTimeEntity 필드명 일치 (createdDate, modifiedDate)

### 엔티티

1. **InspectionStandard**: 검수 기준 기본 정보(카테고리, 내용)를 저장합니다.
    - BaseTimeEntity 상속으로 감사 정보 관리

2. **InspectionStandardImage**: 검수 기준과 연결된 이미지 정보를 저장합니다.
    - 지연 로딩(LAZY) 적용으로 성능 최적화

3. **InspectionCategory**: 검수 기준 카테고리 열거형
    - SHOES, OUTER, BAG, TECH, BEAUTY, PREMIUM_WATCH, PREMIUM_BAG

### 예외 처리

1. **InspectionErrorCode**: 검수 도메인에서 사용하는 모든 에러 코드를 정의합니다.
    - 일관된 접두사(INS)와 코드 번호 체계

2. **InspectionException**: 검수 도메인의 기본 예외 클래스입니다.
    - GlobalException 상속으로 글로벌 예외 처리와 통합

3. **InspectionNotFoundException**: 검수 기준을 찾을 수 없을 때 발생하는 예외입니다.

4. **InspectionFileException**: 파일 처리 중 발생하는 예외입니다.

5. **InspectionPermissionException**: 권한 관련 예외입니다.

### 저장소

1. **InspectionStandardRepository**: 검수 기준 엔티티의 기본 CRUD 및 조회 기능을 제공합니다.
    - JPQL을 활용한 최적화된 조인 쿼리 구현
    - N+1 문제 해결을 위한 fetch join 적용

2. **InspectionStandardRepositoryCustom**: 검색 기능을 위한 인터페이스입니다.

3. **InspectionStandardRepositoryImpl**: QueryDSL을 사용한 검색 구현체입니다.
    - 다양한 조건(카테고리, 키워드)에 따른 동적 쿼리 생성
    - 페이징 및 정렬 기능 포함

4. **InspectionStandardImageRepository**: 검수 기준 이미지 엔티티의 CRUD 및 조회 기능을 제공합니다.

## 캐싱 전략

검수 기준 시스템은 성능 향상을 위해 Spring Cache를 활용합니다:

1. **캐시 이름**:
    - `inspectionStandards`: 모든 검수 기준 목록
    - `inspectionStandardsByCategory`: 카테고리별 검수 기준 목록
    - `inspectionStandardDetail`: 개별 검수 기준 상세
    - `inspectionStandardSearchResults`: 검색 결과

2. **캐시 무효화**:
    - Command 작업(생성, 수정, 삭제) 수행 시 모든 캐시를 무효화
    - 관련 캐시만 선택적으로 무효화하여 성능 최적화

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

**응답:**
```json
{
  "result": "SUCCESS",
  "data": {
    "id": 1,
    "category": "SHOES",
    "content": "<p>신발 검수 기준입니다.</p><p><img src='https://www.pinjun.xyz/api/inspections/files/1/img_abc123.png'/></p>",
    "imageUrls": ["inspection_1/img_abc123.png"],
    "createdDate": "2023-10-15T14:30:00",
    "modifiedDate": "2023-10-15T14:30:00"
  }
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
  "content": "<p>신발 검수 기준이 업데이트되었습니다.</p><p><img src='https://www.pinjun.xyz/api/inspections/files/1/img_abc123.png'/></p>",
  "existingImageUrls": ["https://www.pinjun.xyz/api/inspections/files/1/img_abc123.png"],
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

```
GET /inspections?keyword=신발&page=0&size=10
```
키워드로 검수 기준을 검색합니다.

```
GET /inspections/{id}
```
단일 검수 기준을 조회합니다.

```
GET /inspections/files/{inspectionId}/{fileName}
```
검수 기준 이미지 파일을 다운로드합니다. 보안 강화를 위해 경로 검증 로직이 추가되었습니다.

## 파일 시스템

### 경로 구조

- 기본 경로: `/home/ubuntu/fream/inspection/`
- 검수 기준별 하위 폴더: `inspection_{inspectionId}/`
- 파일 이름: `img_` 접두사 + UUID 기반 생성

### 파일 보안

- 디렉토리 탐색 방지(path traversal) 대응
- 파일 경로 유효성 검증
- 인라인 콘텐츠 배포(Content-Disposition: inline)로 사용자 경험 개선

## 트랜잭션 관리

### 명령 서비스(Command Service)

- 모든 데이터 변경 작업은 트랜잭션으로 처리
- 파일 시스템 작업 오류 시 데이터 일관성 유지

### 조회 서비스(Query Service)

- `@Transactional(readOnly = true)` 적용으로 읽기 성능 최적화

## HTML 내용 처리

### 이미지 태그 처리

HTML 내 이미지 태그(`<img>`) 처리를 위한 정규식 패턴:

```java
String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
Pattern pattern = Pattern.compile(regex);
Matcher matcher = pattern.matcher(content);
```

### 이미지 URL 변환

- 내부 저장 경로: `inspection_{id}/{fileName}`
- 외부 접근 URL: `https://www.pinjun.xyz/api/inspections/files/{inspectionId}/{fileName}`

## 보안

1. **인증 검증**: SecurityContext를 통한 사용자 인증 정보 검증
2. **권한 검증**: 관리자 역할 확인을 통한 권한 관리
3. **파일 경로 검증**: 악의적인 파일 경로 접근 방지
4. **유효성 검증**: Jakarta Validation을 통한 입력 데이터 검증

## 확장 가능성

1. **버전 관리**: 검수 기준의 변경 이력을 추적할 수 있는 버전 관리 기능
2. **다국어 지원**: 검수 기준을 여러 언어로 제공할 수 있도록 확장
3. **신규 카테고리 추가**: InspectionCategory 열거형 확장을 통한 새로운 상품 카테고리 지원
4. **이미지 최적화**: 이미지 업로드 시 리사이징, 압축 등의 최적화 기능
5. **승인 워크플로우**: 검수 기준 변경에 대한 승인 프로세스
6. **실시간 알림**: 검수 기준 변경 시 관련 사용자에게 알림 제공
7. **API 문서화**: Swagger 등을 활용한 API 문서 자동화

## 성능 최적화

1. **N+1 문제 해결**: JPQL의 fetch join과 QueryDSL을 통한 효율적인 쿼리 생성
2. **캐싱**: 자주 조회되는 데이터에 대한 캐싱으로 데이터베이스 부하 감소
3. **지연 로딩**: 필요한 경우에만 관련 데이터를 로드하는 지연 로딩 전략 적용
4. **읽기 전용 트랜잭션**: 조회 작업에 읽기 전용 트랜잭션 적용으로 성능 향상

## 로깅 전략

- **상세한 로그**: 상세한 로그 정보를 통해 문제 추적 및 디버깅 용이
- **레벨별 로깅**: 중요도에 따른 로그 레벨 분리(INFO, DEBUG, WARN, ERROR)
- **식별 정보 포함**: 사용자 식별 정보와 작업 대상 ID를 로그에 포함하여 추적성 향상

## 에러 처리 전략

- **계층적 예외 구조**: 도메인별 예외 클래스 계층 구조로 일관된 예외 처리
- **에러 코드 체계**: INS 접두사를 가진 도메인 특화 에러 코드
- **세부 예외 클래스**: 상황별 특화된 예외 클래스(Not Found, Permission, File 등)
- **글로벌 예외 핸들링**: 모든 예외는 글로벌 예외 처리기로 최종 처리