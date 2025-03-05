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
  "phoneNumber": "010-1234-5678",
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
  "phoneNumber": "010-9876-5432",
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
      "phoneNumber": "010-1234-5678",
      "zipCode": "12345",
      "address": "서울시 강남구 테헤란로",
      "detailedAddress": "123번길 45, 6동 789호",
      "isDefault": true
    },
    {
      "id": 2,
      "recipientName": "김철수",
      "phoneNumber": "010-9876-5432",
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
  "phoneNumber": "010-1234-5678",
  "zipCode": "12345",
  "address": "서울시 강남구 테헤란로",
  "detailedAddress": "123번길 45, 6동 789호",
  "isDefault": true
}
```

## 데이터베이스 스키마

`address` 테이블에는 다음 필드가 포함됩니다:

| 필드             | 타입         | 설명                           |
|-----------------|--------------|-------------------------------|
| id              | BIGINT       | 기본 키                        |
| user_id         | BIGINT       | 사용자 외래 키 (다대일 관계)     |
| recipient_name  | VARCHAR      | 수령인 이름                    |
| phone_number    | VARCHAR      | 연락처                         |
| zip_code        | VARCHAR      | 우편번호                       |
| address         | VARCHAR      | 주소                          |
| detailed_address| VARCHAR      | 상세 주소                     |
| is_default      | BOOLEAN      | 기본 배송지 여부               |
| created_at      | TIMESTAMP    | 생성 시간 (BaseTimeEntity에서 상속) |
| updated_at      | TIMESTAMP    | 수정 시간 (BaseTimeEntity에서 상속) |

## 보안

모든 주소 관련 작업은 인증된 사용자만 수행할 수 있으며, `SecurityUtils.extractEmailFromSecurityContext()`를 통해 현재 로그인한 사용자의 이메일을 추출합니다. 또한, 각 사용자는 자신의 주소만 조회, 수정, 삭제할 수 있습니다.

## 구현 참고사항

### 기본 배송지 관리

특정 주소를 기본 배송지로 설정하면 해당 사용자의 다른 모든 주소는 기본 배송지 설정이 해제됩니다. 이를 통해 한 번에 하나의 기본 배송지만 존재하도록 보장합니다.

### 편의 메서드

`Address` 엔티티에는 다음과 같은 편의 메서드가 구현되어 있습니다:

1. **updateAddress**: 주소 정보를 업데이트합니다. null이 아닌 필드만 업데이트됩니다.
2. **assignUser**: 주소에 사용자를 할당합니다.
3. **unassignUser**: 주소에서 사용자 할당을 해제합니다.

### 예외 처리

서비스에서는 다음과 같은 예외 상황을 처리합니다:

1. 존재하지 않는 사용자에 대한 주소 조작 시도: "해당 사용자를 찾을 수 없습니다."
2. 존재하지 않는 주소 ID에 대한 접근 시도: "해당 주소록을 찾을 수 없습니다."

## 확장 가능성

향후 다음과 같은 기능을 추가할 수 있습니다:

1. **주소 검증**: 우편번호 및 주소 형식 검증
2. **배송지 별명 추가**: 사용자가 배송지에 별명을 지정할 수 있는 기능
3. **최근 사용 순서 관리**: 최근에 사용한 배송지를 우선적으로 표시
4. **지역별 배송 정책 연동**: 특정 지역에 대한 배송 정책 정보 제공