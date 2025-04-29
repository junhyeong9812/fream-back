# FREAM 공지사항 도메인

## 개요

공지사항 도메인은 FREAM 애플리케이션 내에서 사용자에게 중요한 정보를 전달하는 기능을 담당합니다. 이 도메인은 관리자가 다양한 카테고리의 공지사항을 작성, 수정, 삭제할 수 있게 하며, 사용자는 이를 조회할 수 있습니다. 또한 공지사항에 첨부파일(이미지, 비디오)을 추가하는 기능을 지원합니다.

## 주요 기능

- **공지사항 관리**: 공지사항의 생성, 수정, 삭제 (관리자 권한 필요)
- **공지사항 조회**: 전체 목록, 카테고리별 목록, 단일 공지사항 조회
- **파일 관리**: 이미지 및 비디오 파일 업로드, 삭제, 다운로드
- **검색 기능**: 제목 및 내용 기반 공지사항 검색
- **카테고리 분류**: 공지사항, 이벤트, 서비스 안내 등 카테고리 구분
- **알림 발송**: 새로운 공지사항 등록 시 사용자에게 알림 발송

## 도메인 모델

### 핵심 엔티티

- **Notice**: 공지사항의 기본 정보(제목, 내용, 카테고리 등)를 포함하는 주요 엔티티
- **NoticeImage**: 공지사항에 첨부된 이미지나 비디오 파일 정보를 관리하는 엔티티

### 엔티티 관계

```
Notice
  └── NoticeImage (일대다)
```

### Enum 타입

- **NoticeCategory**: 공지사항 카테고리 (ANNOUNCEMENT, EVENT, SERVICE, OTHERS)

## 아키텍처

공지사항 도메인은 계층화된 아키텍처를 따릅니다:

1. **컨트롤러 계층**: HTTP 요청 및 응답 처리
    - **NoticeCommandController**: 공지사항 생성, 수정, 삭제 처리
    - **NoticeQueryController**: 공지사항 조회 및 검색 처리

2. **서비스 계층**: 비즈니스 로직 포함
    - **NoticeCommandService**: 공지사항 생성, 수정, 삭제 관련 비즈니스 로직
    - **NoticeQueryService**: 공지사항 조회 및 검색 관련 비즈니스 로직

3. **리포지토리 계층**: 데이터 액세스 관리
    - **NoticeRepository**: 기본 CRUD 작업 및 JPA 쿼리 메소드
    - **NoticeImageRepository**: 공지사항 이미지 관리
    - **NoticeRepositoryCustom**: QueryDSL 기반 복잡한 검색 쿼리 인터페이스
    - **NoticeRepositoryImpl**: QueryDSL 구현체

## 주요 구성 요소

### DTO(Data Transfer Object)

- **NoticeCreateRequestDto**: 공지사항 생성 요청 데이터
- **NoticeUpdateRequestDto**: 공지사항 수정 요청 데이터
- **NoticeResponseDto**: 공지사항 응답 데이터

### 컨트롤러

- **NoticeCommandController**: 공지사항 생성, 수정, 삭제 API
- **NoticeQueryController**: 공지사항 조회, 검색, 파일 다운로드 API

### 서비스

- **NoticeCommandService**: 공지사항 생성, 수정, 삭제 비즈니스 로직
- **NoticeQueryService**: 공지사항 조회, 검색, 파일 조회 비즈니스 로직

### 예외 처리

공지사항 도메인은 계층적 예외 처리 전략을 구현합니다:

- **NoticeException**: 모든 공지사항 관련 예외의 기본 클래스
- **NoticeNotFoundException**: 공지사항을 찾을 수 없을 때 발생하는 예외
- **NoticeFileException**: 파일 처리 관련 예외
- **NoticePermissionException**: 권한 관련 예외
- **NoticeErrorCode**: 도메인 특화 에러 코드

## API 엔드포인트

### 명령(Command) API

- **POST /notices**: 공지사항 생성
- **PUT /notices/{noticeId}**: 공지사항 수정
- **DELETE /notices/{noticeId}**: 공지사항 삭제

### 조회(Query) API

- **GET /notices**: 공지사항 목록 조회 (카테고리별 필터링 가능)
- **GET /notices/{noticeId}**: 단일 공지사항 조회
- **GET /notices/search**: 공지사항 검색
- **GET /notices/files/{noticeId}/{fileName}**: 공지사항 첨부 파일 다운로드

## 파일 관리

- **허용 파일 형식**: 이미지(jpg, jpeg, png, gif), 비디오(mp4, avi, mov)
- **파일 저장 경로**: `/home/ubuntu/fream/notice/notice_{noticeId}/`
- **파일명 중복 방지**: UUID 기반 파일명 생성

## 보안

- **권한 검증**: 공지사항 생성, 수정, 삭제는 관리자 권한 필요
- **인증 정보 추출**: SecurityContextHolder를 통한 사용자 이메일 추출
- **경로 검증**: 디렉토리 탐색(Path Traversal) 취약점 방지

## 로깅

- 각 계층에서 상세한 로깅 구현
- 오류 발생 시 구체적인 로그 메시지 제공
- 성공적인 작업 완료 시 정보 로깅

## 트랜잭션 관리

- 명령(Command) 서비스: `@Transactional` 적용
- 조회(Query) 서비스: `@Transactional(readOnly = true)` 적용

## 알림 기능

- 새 공지사항 등록 시 NotificationCommandService를 통해 모든 사용자에게 알림 발송
- 알림 카테고리: SHOPPING, 알림 타입: ANNOUNCEMENT

## 사용 예시

### 공지사항 생성 절차

1. 관리자 권한으로 인증
2. 제목, 내용, 카테고리, 첨부파일로 구성된 요청 데이터 준비
3. `/notices` 엔드포인트로 POST 요청
4. 첨부파일이 있을 경우 저장 및 내용에 이미지 URL 삽입
5. 알림 발송 (모든 사용자에게)

### 공지사항 조회 절차

1. 전체 목록: `/notices` GET 요청
2. 카테고리별 목록: `/notices?category=EVENT` GET 요청
3. 키워드 검색: `/notices/search?keyword=이벤트` GET 요청
4. 단일 공지사항: `/notices/{noticeId}` GET 요청

## 구현된 모범 사례

- **명령-쿼리 책임 분리(CQRS)**: 명령과 쿼리를 별도의 컨트롤러와 서비스로 분리
- **예외 계층화**: 도메인 특화 예외 클래스와 에러 코드 체계
- **방어적 프로그래밍**: 입력값 검증, null 안전 처리, 예외 핸들링
- **인덱싱**: 효율적인 쿼리를 위한 데이터베이스 인덱스 적용
- **지연 로딩**: 필요한 경우에만 연관 데이터 로딩
- **스트림 API**: 컬렉션 처리에 자바 스트림 API 활용
- **빌더 패턴**: 객체 생성 시 빌더 패턴 적용

## 성능 고려사항

- **페이지네이션**: 대량의 공지사항 목록을 페이지 단위로 조회
- **지연 로딩**: 필요한 경우에만 이미지 데이터 로딩
- **인덱싱**: 카테고리 및 생성일 기준 인덱스 적용
- **파일 처리 최적화**: 파일 저장 및 삭제 시 비동기 작업 고려