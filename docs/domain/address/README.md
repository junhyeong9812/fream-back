# 주소록 시스템 (Address System)

## 개요

주소록 시스템은 사용자의 배송지 정보를 관리하기 위한 모듈입니다. 이 시스템은 사용자별로 여러 배송 주소를 생성, 조회, 수정, 삭제할 수 있는 기능을 제공하며, 기본 배송지 설정 기능도 지원합니다. CQRS 패턴에 따라 명령(Command)과 조회(Query) 작업이 분리되어 있어 효율적인 리소스 관리가 가능합니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.address/
├── controller/
│   ├── command/          # 주소 생성, 수정, 삭제를 위한 컨트롤러
│   └── query/            # 주소 조회를 위한 컨트롤러
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티
├── exception/            # 도메인 관련 예외 클래스
├── repository/           # 데이터 접근 계층
└── service/
    ├── command/          # 주소 생성, 수정, 삭제를 위한 서비스
    └── query/            # 주소 조회를 위한 서비스
```

## 주요 구성 요소

### Controller

1. **AddressCommandController**: 주소 생성, 수정, 삭제 등의 명령 작업을 처리합니다.
2. **AddressQueryController**: 주소 목록 조회, 개별 주소 조회 등의 조회 작업을 처리합니다.

### Service

1. **AddressCommandService**: 주소 생성, 수정, 삭제 등의 비즈니스 로직을 처리합니다.
2. **AddressQueryService**: 주소 조회와 관련된 비즈니스 로직을 처리합니다.

### Repository

**AddressRepository**: JPA를 이용한 데이터베이스 접근 인터페이스로, 사용자 기반 주소 조회 등의 기능을 제공합니다.

### Entity

**Address**: 주소 정보를 저장하는 JPA 엔티티입니다. `BaseTimeEntity`를 상속받아 생성/수정 시간 정보를 자동으로 관리합니다.

### DTO

1. **AddressCreateDto**: 주소 생성 요청에 사용되는 DTO입니다.
2. **AddressUpdateDto**: 주소 수정 요청에 사용되는 DTO입니다.
3. **AddressResponseDto**: 주소 정보 응답에 사용되는 DTO입니다.
4. **AddressListResponseDto**: 주소 목록 응답에 사용되는 DTO입니다.

### Exception

1. **AddressException**: 주소 관련 모든 예외의 기본 클래스입니다.
2. **AddressNotFoundException**: 주소를 찾을 수 없을 때 발생하는 예외입니다.
3. **AddressUserNotFoundException**: 주소와 관련된 사용자를 찾을 수 없을 때 발생하는 예외입니다.
4. **AddressAccessDeniedException**: 주소에 대한 접근 권한이 없을 때 발생하는 예외입니다.

## 주요 기능

### 주소 생성

```
POST /addresses
```

사용자는 수령인 이름, 전화번호, 우편번호, 주소, 상세 주소 정보와 함께 기본 배송지 여부를 설정할 수 있습니다. 기본 배송지로 설정하면 이전에 설정된 기본 배송지는 자동으로 해제됩니다.

**요청 본문 예시:**
```json
{
  "recipientName": "홍길동",
  "phoneNumber": "01012345678",
  "zipCode": "12345",
  "address": "서울시 강남구 테헤란로",
  "detailedAddress": "123번길 45, 6동 789호",
  "isDefault": true
}
```

### 주소 수정

```
PUT /addresses
```

주소 ID와 함께 수정할 정보를 전송하여 기존 주소를 수정합니다. 기본 배송지로 설정하면 다른 모든 주소의 기본 배송지 설정이 해제됩니다.

**요청 본문 예시:**
```json
{
  "addressId": 1,
  "recipientName": "홍길동",
  "phoneNumber": "01098765432",
  "zipCode": "54321",
  "address": "서울시 서초구 서초대로",
  "detailedAddress": "456번길 78, 9동 123호",
  "isDefault": true
}
```

### 주소 삭제

```
DELETE /addresses/{addressId}
```

지정된 ID의 주소를 삭제합니다.

### 주소 목록 조회

```
GET /addresses
```

현재 로그인한 사용자의 모든 주소 목록을 조회합니다.

**응답 본문 예시:**
```json
{
  "addresses": [
    {
      "id": 1,
      "recipientName": "홍길동",
      "phoneNumber": "01012345678",
      "zipCode": "12345",
      "address": "서울시 강남구 테헤란로",
      "detailedAddress": "123번길 45, 6동 789호",
      "isDefault": true
    },
    {
      "id": 2,
      "recipientName": "김철수",
      "phoneNumber": "01098765432",
      "zipCode": "54321",
      "address": "서울시 서초구 서초대로",
      "detailedAddress": "456번길 78, 9동 123호",
      "isDefault": false
    }
  ]
}
```

### 특정 주소 조회

```
GET /addresses/{addressId}
```

지정된 ID의 주소 정보를 조회합니다.

**응답 본문 예시:**
```json
{
  "id": 1,
  "recipientName": "홍길동",
  "phoneNumber": "01012345678",
  "zipCode": "12345",
  "address": "서울시 강남구 테헤란로",
  "detailedAddress": "123번길 45, 6동 789호",
  "isDefault": true
}
```

## 데이터베이스 스키마

`address` 테이블에는 다음 필드가 포함됩니다:

| 필드             | 타입         | 제약조건                      | 설명                           |
|-----------------|--------------|------------------------------|-------------------------------|
| id              | BIGINT       | PK, AUTO_INCREMENT           | 기본 키                        |
| user_id         | BIGINT       | FK, NOT NULL                 | 사용자 외래 키 (다대일 관계)     |
| recipient_name  | VARCHAR      | NOT NULL                     | 수령인 이름                    |
| phone_number    | VARCHAR      | NOT NULL                     | 연락처                         |
| zip_code        | VARCHAR(5)   | NOT NULL                     | 우편번호                       |
| address         | VARCHAR      | NOT NULL                     | 주소                          |
| detailed_address| VARCHAR      |                              | 상세 주소                     |
| is_default      | BOOLEAN      | DEFAULT FALSE                | 기본 배송지 여부               |
| created_at      | TIMESTAMP    | NOT NULL                     | 생성 시간 (BaseTimeEntity에서 상속) |
| updated_at      | TIMESTAMP    | NOT NULL                     | 수정 시간 (BaseTimeEntity에서 상속) |

## 인덱스 정보

성능 최적화를 위해 다음 인덱스가 적용되어 있습니다:

| 인덱스 이름     | 대상 컬럼      | 목적                            |
|----------------|---------------|--------------------------------|
| idx_user_id    | user_id       | 사용자별 주소 목록 조회 최적화     |
| idx_is_default | is_default    | 기본 주소 검색 최적화             |

## 유효성 검증

주소 생성 및 수정 시 다음과 같은 유효성 검증이 적용됩니다:

### AddressCreateDto

| 필드            | 검증 어노테이션                           | 오류 메시지                                               |
|----------------|-----------------------------------------|----------------------------------------------------------|
| recipientName  | @NotBlank                               | "수령인 이름은 필수 입력사항입니다."                         |
| phoneNumber    | @NotBlank, @Pattern(regexp="\\d{10,11}") | "전화번호 형식이 올바르지 않습니다. 숫자 10-11자리로 입력해주세요." |
| zipCode        | @NotBlank, @Pattern(regexp="\\d{5}")     | "우편번호 형식이 올바르지 않습니다. 5자리 숫자로 입력해주세요."   |
| address        | @NotBlank                               | "주소는 필수 입력사항입니다."                                |

### AddressUpdateDto

| 필드            | 검증 어노테이션                           | 오류 메시지                                               |
|----------------|-----------------------------------------|----------------------------------------------------------|
| addressId      | @NotNull                                | "주소 ID는 필수 입력사항입니다."                             |
| phoneNumber    | @Pattern(regexp="^$\|\\d{10,11}")        | "전화번호 형식이 올바르지 않습니다. 숫자 10-11자리로 입력해주세요." |
| zipCode        | @Pattern(regexp="^$\|\\d{5}")            | "우편번호 형식이 올바르지 않습니다. 5자리 숫자로 입력해주세요."   |

## 보안

모든 주소 관련 작업은 인증된 사용자만 수행할 수 있으며, `SecurityUtils.extractEmailFromSecurityContext()`를 통해 현재 로그인한 사용자의 이메일을 추출합니다. 또한, 각 사용자는 자신의 주소만 조회, 수정, 삭제할 수 있습니다.

## 구현 참고사항

### 기본 배송지 관리

특정 주소를 기본 배송지로 설정하면 해당 사용자의 다른 모든 주소는 기본 배송지 설정이 해제됩니다. 이를 통해 한 번에 하나의 기본 배송지만 존재하도록 보장합니다.

### 편의 메서드

`Address` 엔티티에는 다음과 같은 편의 메서드가 구현되어 있습니다:

1. **updateAddress**: 주소 정보를 업데이트합니다. null이 아닌 필드만 업데이트됩니다.
   ```java
   public void updateAddress(String recipientName, String phoneNumber, String zipCode,
                          String address, String detailedAddress, Boolean isDefault)
   ```

2. **assignUser**: 주소에 사용자를 할당합니다.
   ```java
   public void assignUser(User user)
   ```

3. **unassignUser**: 주소에서 사용자 할당을 해제합니다.
   ```java
   public void unassignUser()
   ```

### 예외 처리

서비스에서는 다음과 같은 예외 상황을 처리합니다:

| 예외 클래스                  | 상황                                | 오류 코드                   | HTTP 상태 |
|-----------------------------|------------------------------------|-----------------------------|-----------|
| AddressUserNotFoundException | 존재하지 않는 사용자에 대한 요청      | ADDRESS_USER_NOT_FOUND     | 404       |
| AddressNotFoundException    | 존재하지 않는 주소 ID 요청           | ADDRESS_NOT_FOUND          | 404       |
| AddressAccessDeniedException | 다른 사용자의 주소에 접근 시도        | ADDRESS_ACCESS_DENIED      | 403       |
| AddressInvalidDataException | 유효하지 않은 주소 데이터 입력        | ADDRESS_INVALID_DATA       | 400       |

## 최근 개선 사항 (2025/04/25)

### 1. 유효성 검증 강화
- Jakarta Bean Validation 추가: `@NotBlank`, `@Pattern`, `@NotNull` 어노테이션을 통한 검증
- 전화번호, 우편번호에 대한 정규식 기반 형식 검증 추가
- 명확하고, 사용자 친화적인 오류 메시지 제공

### 2. 엔티티 최적화
- 성능 향상을 위한 인덱스 추가: `idx_user_id`, `idx_is_default`
- 컬럼 제약 조건 강화: `@Column(nullable = false)` 추가
- 우편번호 길이 제한(5자리) 적용

### 3. 일관된 예외 처리
- 도메인 별 예외 계층 구조 정비
- 모든 API에서 동일한 예외 처리 패턴 적용
- 예외 상황에 대한 상세 로깅 개선

## 의존성 추가 사항

Jakarta Bean Validation을 사용하기 위해 다음 의존성이 추가되었습니다:

**Maven:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Gradle:**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

## 확장 가능성

향후 다음과 같은 기능을 추가할 수 있습니다:

1. **주소 즐겨찾기**: 자주 사용하는 주소를 즐겨찾기로 등록하는 기능
2. **배송지 별명 추가**: 사용자가 배송지에 별명을 지정할 수 있는 기능
3. **최근 사용 순서 관리**: 최근에 사용한 배송지를 우선적으로 표시
4. **배송비 계산 연동**: 지역별 배송비 계산을 위한 기능 연동
5. **국제 주소 지원**: 해외 배송을 위한 국제 주소 형식 지원