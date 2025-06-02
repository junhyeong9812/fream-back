# 주소록 시스템 (Address System)

## 개요

주소록 시스템은 사용자의 배송지 정보를 관리하기 위한 모듈입니다. 이 시스템은 사용자별로 여러 배송 주소를 생성, 조회, 수정, 삭제할 수 있는 기능을 제공하며, 기본 배송지 설정 기능도 지원합니다. CQRS 패턴에 따라 명령(Command)과 조회(Query) 작업이 분리되어 있어 효율적인 리소스 관리가 가능합니다.

**핵심 특징:**
- **개인정보 보호**: 결정적 암호화와 양방향 암호화를 조합한 이중 보안 체계
- **검색 기능**: 암호화된 상태에서도 이름, 전화번호, 주소 검색 가능
- **CQRS 패턴**: 명령과 조회 작업의 완전한 분리

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

## 보안 및 암호화 체계

### 이중 암호화 전략

개인정보 보호와 검색 기능을 모두 만족시키기 위해 **필드별 차별 암호화** 방식을 적용했습니다:

#### 1. 결정적 암호화 (Deterministic Encryption)
**적용 필드**: 이름, 전화번호, 주소, 우편번호
- **특징**: 동일한 입력값에 대해 항상 동일한 암호화 결과 생성
- **장점**: 암호화된 상태에서도 검색 가능
- **알고리즘**: AES/ECB/PKCS5Padding + SHA-256 기반 키 생성

```java
// 검색 예시
String encryptedName = encryptionUtil.encryptForSearch("홍길동");
List<Address> addresses = addressRepository.findByUserAndRecipientName(user, encryptedName);
```

#### 2. 양방향 암호화 (Bidirectional Encryption)
**적용 필드**: 상세주소
- **특징**: IV(Initialization Vector) 사용으로 높은 보안성
- **장점**: 동일한 입력값도 매번 다른 암호화 결과
- **알고리즘**: AES/CBC/PKCS5Padding

```java
// 상세주소는 보안성 우선
String encryptedDetail = encryptionUtil.encrypt("123동 456호");
```

### 암호화 유틸리티 구조

```java
@Component
public class PersonalDataEncryptionUtil {
    
    // 결정적 암호화 (검색 가능)
    public String deterministicEncrypt(String plainText);
    public String deterministicDecrypt(String encryptedText);
    
    // 양방향 암호화 (보안성 우선)
    public String encrypt(String plainText);
    public String decrypt(String encryptedText);
    
    // 검색용 편의 메서드
    public String encryptForSearch(String plainText);
}
```

## 주요 구성 요소

### Controller

1. **AddressCommandController**: 주소 생성, 수정, 삭제 등의 명령 작업을 처리합니다.
2. **AddressQueryController**: 주소 목록 조회, 개별 주소 조회, 검색 등의 조회 작업을 처리합니다.

### Service

1. **AddressCommandService**: 주소 생성, 수정, 삭제 등의 비즈니스 로직을 처리합니다.
2. **AddressQueryService**: 주소 조회와 검색 관련 비즈니스 로직을 처리합니다.

### Repository

**AddressRepository**: JPA를 이용한 데이터베이스 접근 인터페이스로, 암호화된 데이터 검색 기능을 제공합니다.

#### 주요 검색 메서드:
- `findByUserAndRecipientName()`: 이름으로 검색
- `findByUserAndPhoneNumber()`: 전화번호로 검색
- `findByUserAndAddress()`: 주소로 검색
- `findByUserAndZipCode()`: 우편번호로 검색
- `findByUserAndMultipleFields()`: 복합 조건 검색

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

### 특정 주소 조회

```
GET /addresses/{addressId}
```

지정된 ID의 주소 정보를 조회합니다.

### 🆕 주소 검색 기능

결정적 암호화를 활용한 새로운 검색 기능들:

#### 이름으로 검색
```
GET /addresses/search/by-name?name=홍길동
```

#### 전화번호로 검색
```
GET /addresses/search/by-phone?phone=01012345678
```

#### 주소로 검색
```
GET /addresses/search/by-address?address=서울시 강남구
```

**검색 작동 원리:**
1. 검색어를 결정적 암호화로 변환
2. 암호화된 검색어와 DB의 암호화된 값 비교
3. 일치하는 레코드의 암호화된 값들을 복호화하여 응답

## 데이터베이스 스키마

`address` 테이블에는 다음 필드가 포함됩니다:

| 필드             | 타입         | 제약조건                      | 암호화 방식      | 설명                           |
|-----------------|--------------|------------------------------|----------------|-------------------------------|
| id              | BIGINT       | PK, AUTO_INCREMENT           | -              | 기본 키                        |
| user_id         | BIGINT       | FK, NOT NULL                 | -              | 사용자 외래 키 (다대일 관계)     |
| recipient_name  | VARCHAR      | NOT NULL                     | **결정적**      | 수령인 이름 (검색 가능)         |
| phone_number    | VARCHAR      | NOT NULL                     | **결정적**      | 연락처 (검색 가능)             |
| zip_code        | VARCHAR(5)   | NOT NULL                     | **결정적**      | 우편번호 (검색 가능)           |
| address         | VARCHAR      | NOT NULL                     | **결정적**      | 주소 (검색 가능)              |
| detailed_address| VARCHAR      |                              | **양방향**      | 상세 주소 (보안성 우선)        |
| is_default      | BOOLEAN      | DEFAULT FALSE                | -              | 기본 배송지 여부               |
| created_at      | TIMESTAMP    | NOT NULL                     | -              | 생성 시간                     |
| updated_at      | TIMESTAMP    | NOT NULL                     | -              | 수정 시간                     |

## 인덱스 정보

성능 최적화를 위해 다음 인덱스가 적용되어 있습니다:

| 인덱스 이름          | 대상 컬럼         | 목적                            |
|--------------------|------------------|--------------------------------|
| idx_user_id        | user_id          | 사용자별 주소 목록 조회 최적화     |
| idx_is_default     | is_default       | 기본 주소 검색 최적화             |
| idx_recipient_name | recipient_name   | 이름 검색 최적화                 |
| idx_phone_number   | phone_number     | 전화번호 검색 최적화             |
| idx_address        | address          | 주소 검색 최적화                 |

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

### 접근 제어
모든 주소 관련 작업은 인증된 사용자만 수행할 수 있으며, `SecurityUtils.extractEmailFromSecurityContext()`를 통해 현재 로그인한 사용자의 이메일을 추출합니다. 또한, 각 사용자는 자신의 주소만 조회, 수정, 삭제할 수 있습니다.

### 암호화 키 관리
```yaml
# application.yml
personal-data:
  encryption:
    secret-key: ${ENCRYPTION_SECRET_KEY:your-32-char-secret-key}
    iv: ${ENCRYPTION_IV:your-16-char-iv}
```

**주의사항:**
- 프로덕션 환경에서는 반드시 환경변수로 키 관리
- 키 변경 시 기존 데이터 마이그레이션 필요
- 정기적인 키 로테이션 정책 수립 권장

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

## 최근 개선 사항

### 🆕 2025/06/02 - 결정적 암호화 도입

#### 1. 이중 암호화 체계 구현
- **결정적 암호화**: 이름, 전화번호, 주소, 우편번호 (검색 가능)
- **양방향 암호화**: 상세주소 (보안성 우선)
- **PersonalDataEncryptionUtil** 확장으로 두 방식 모두 지원

#### 2. 검색 기능 대폭 강화
- 암호화된 상태에서도 정확한 검색 가능
- 이름, 전화번호, 주소별 개별 검색 API 추가
- 복합 조건 검색 지원

#### 3. Repository 검색 메서드 추가
```java
// 새로 추가된 검색 메서드들
List<Address> findByUserAndRecipientName(User user, String encryptedName);
List<Address> findByUserAndPhoneNumber(User user, String encryptedPhone);
List<Address> findByUserAndAddress(User user, String encryptedAddress);
List<Address> findByUserAndMultipleFields(...); // 복합 검색
```

#### 4. 성능 최적화
- 검색 필드별 인덱스 추가
- 결정적 암호화로 인한 검색 성능 향상
- 불필요한 복호화 작업 최소화

### 2025/04/25 - 유효성 검증 강화
- Jakarta Bean Validation 추가
- 전화번호, 우편번호 정규식 검증
- 사용자 친화적 오류 메시지 제공

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
6. **🆕 모음자음 분리 검색**: 한글 자음/모음 분리를 통한 퍼지 검색
7. **🆕 주소 자동완성**: 도로명주소 API 연동을 통한 실시간 주소 자동완성
8. **🆕 검색 로그 분석**: 사용자 검색 패턴 분석을 통한 UX 개선

## 트러블슈팅

### 암호화 관련 이슈

1. **기존 데이터 마이그레이션**
   ```sql
   -- 기존 평문 데이터를 암호화된 데이터로 변환하는 마이그레이션 스크립트 필요
   -- 주의: 결정적 암호화 적용 후 기존 검색 쿼리 수정 필요
   ```

2. **성능 이슈**
   ```java
   // 대량 검색 시 인덱스 활용을 위한 쿼리 최적화
   @Query("SELECT a FROM Address a WHERE a.user = :user AND a.recipientName = :name")
   ```

3. **키 관리**
   ```yaml
   # 환경별 키 분리 관리 권장
   spring:
     profiles:
       active: prod
   ---
   spring:
     profiles: prod
   personal-data:
     encryption:
       secret-key: ${PROD_ENCRYPTION_KEY}
   ```