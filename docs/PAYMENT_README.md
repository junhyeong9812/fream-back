# 결제 시스템 (Payment System)

## 개요

결제 시스템은 애플리케이션 내 다양한 결제 수단을 통합적으로 관리하는 모듈입니다. 이 시스템은 카드 결제, 계좌 이체, 일반 결제 등 여러 결제 방식을 지원하며, PortOne API를 통해 실제 결제 처리를 수행합니다. 또한 사용자별 결제 정보(카드 정보)를 안전하게 저장하고 관리하는 기능을 제공하며, 결제 상태 추적 및 환불 기능도 구현되어 있습니다.

## 아키텍처

```
com.fream.back.domain.payment/
├── controller/
│   └── command/            # 결제 정보 생성, 삭제, 테스트 결제 컨트롤러
├── dto/
│   ├── paymentInfo/        # 결제 정보 관련 DTO
│   └── [payment types]/    # 다양한 결제 유형별 DTO
├── entity/                 # 데이터베이스 엔티티
├── portone/                # PortOne 결제 API 연동
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 결제 처리, 결제 정보 생성 서비스
    └── query/              # 결제 조회, 결제 정보 조회 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **PaymentInfoController**: 결제 정보 관리 API 제공 (생성, 조회, 삭제, 테스트 결제)

### 서비스

1. **PaymentCommandService**: 결제 처리 로직 (카드, 계좌, 일반 결제)
2. **PaymentInfoCommandService**: 결제 정보 생성 및 삭제 로직
3. **PaymentInfoQueryService**: 결제 정보 조회 로직
4. **PaymentQueryService**: 결제 내역 조회 로직

### 엔티티

1. **Payment**: 결제 기본 정보를 담는 추상 클래스
2. **CardPayment**: 카드 결제 정보 (Payment 상속)
3. **AccountPayment**: 계좌 이체 결제 정보 (Payment 상속)
4. **GeneralPayment**: 일반 결제 정보 (Payment 상속)
5. **PaymentInfo**: 사용자의 결제 수단 정보 (카드 정보 등)
6. **PaymentStatus**: 결제 상태 열거형

### 외부 연동

**PortOneApiClient**: PortOne API와의 통신을 담당하는 클라이언트 클래스

## API 엔드포인트

### 결제 정보 관리 API

```
POST /payment-info
```
새로운 결제 정보(카드)를 등록합니다. 등록 시 테스트 결제 및 즉시 환불을 통해 유효성을 검증합니다.

**요청 본문 예시:**
```json
{
  "cardNumber": "1234-5678-9012-3456",
  "cardPassword": "12",
  "expirationDate": "2312",
  "birthDate": "990101"
}
```

```
GET /payment-info
```
사용자의 모든 결제 정보 목록을 조회합니다.

```
GET /payment-info/{id}
```
특정 결제 정보를 조회합니다.

```
DELETE /payment-info/{id}
```
특정 결제 정보를 삭제합니다.

```
POST /payment-info/test-payment
```
테스트 결제를 요청하고 즉시 환불합니다. 카드 유효성 검증 용도로 사용됩니다.

## 데이터베이스 스키마

### 1. PAYMENT 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| payment_type    | VARCHAR        | 결제 유형 (CARD, ACCOUNT, GENERAL) |
| order_id        | BIGINT         | 주문 외래 키 (주문 결제 시) |
| sale_id         | BIGINT         | 판매 외래 키 (판매 결제 시) |
| user_id         | BIGINT         | 사용자 외래 키              |
| paid_amount     | DOUBLE         | 결제 금액                   |
| is_success      | BOOLEAN        | 결제 성공 여부              |
| payment_date    | TIMESTAMP      | 결제 완료 시간              |
| status          | VARCHAR        | 결제 상태(ENUM 문자열)      |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)   |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)   |

#### 1.1 CARD_PAYMENT 필드 (PAYMENT 상속)

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| card_number     | VARCHAR        | 카드 번호                   |
| card_password   | VARCHAR        | 카드 비밀번호 앞 두 자리     |
| card_expiration | VARCHAR        | 카드 유효기간                |
| birth_date      | VARCHAR        | 생년월일                    |
| card_type       | VARCHAR        | 카드 타입                   |
| imp_uid         | VARCHAR        | PortOne 거래 고유 번호      |
| receipt_url     | VARCHAR        | 영수증 URL                  |
| pg_provider     | VARCHAR        | PG사 이름                   |
| pg_tid          | VARCHAR        | PG사 거래 ID                |

#### 1.2 ACCOUNT_PAYMENT 필드 (PAYMENT 상속)

| 필드              | 타입           | 설명                        |
|------------------|----------------|---------------------------|
| imp_uid          | VARCHAR        | PortOne 거래 고유 번호      |
| bank_name        | VARCHAR        | 은행명                      |
| account_number   | VARCHAR        | 계좌번호                    |
| account_holder   | VARCHAR        | 예금주명                    |
| receipt_requested| BOOLEAN        | 현금영수증 요청 여부         |

#### 1.3 GENERAL_PAYMENT 필드 (PAYMENT 상속)

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| imp_uid         | VARCHAR        | PortOne 거래 고유 번호      |
| pg_provider     | VARCHAR        | PG사 이름                   |
| receipt_url     | VARCHAR        | 영수증 URL                  |
| buyer_name      | VARCHAR        | 구매자 이름                  |
| buyer_email     | VARCHAR        | 구매자 이메일                |

### 2. PAYMENT_INFO 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| user_id         | BIGINT         | 사용자 외래 키              |
| card_number     | VARCHAR        | 카드 번호                   |
| card_password   | VARCHAR        | 카드 비밀번호 앞 두 자리     |
| expiration_date | VARCHAR        | 유효기간                    |
| birth_date      | VARCHAR        | 생년월일                    |

## 결제 상태 흐름

### PaymentStatus 흐름

```
PENDING → PAID → REFUND_REQUESTED → REFUNDED
       └→ REFUNDED (즉시 환불 경우)
```

- **PENDING**: 결제 대기 상태
- **PAID**: 결제 완료 상태
- **REFUND_REQUESTED**: 환불 요청 상태
- **REFUNDED**: 환불 완료 상태

## 주요 기능 상세

### 1. 결제 정보 관리

1. **결제 정보 등록**:
    - 사용자가 카드 정보를 등록할 때, 실제 테스트 결제를 통해 유효성을 검증합니다.
    - 테스트 결제가 성공하면 즉시 환불되고, 검증된 카드 정보가 저장됩니다.
    - 사용자당 최대 5개의 결제 정보를 저장할 수 있습니다.

2. **결제 정보 조회**:
    - 사용자는 자신의 모든 결제 정보를 조회할 수 있습니다.
    - 결제 정보는 마스킹 처리되어 제공됩니다.

3. **결제 정보 삭제**:
    - 사용자는 등록된 결제 정보를 삭제할 수 있습니다.

### 2. 결제 처리 흐름

1. **카드 결제 처리**:
    - 저장된 결제 정보를 기반으로 PortOne API를 통해 결제를 요청합니다.
    - 결제 성공 시 CardPayment 객체를 생성하고 결제 상태를 PAID로 설정합니다.
    - 환불이 필요한 경우 즉시 환불 API를 호출하고 상태를 REFUNDED로 업데이트합니다.

2. **계좌 이체 결제 처리**:
    - 사용자가 입력한 계좌 정보를 검증하고 AccountPayment 객체를 생성합니다.
    - 결제 상태는 초기에 PENDING으로 설정됩니다.

3. **일반 결제 처리**:
    - 외부에서 처리된 결제에 대한 정보를 받아 GeneralPayment 객체를 생성합니다.
    - 주로 외부 PG사를 통한 결제에 사용됩니다.

### 3. 환불 처리

1. **환불 요청**:
    - 결제된 내역에 대해 환불을 요청할 수 있습니다.
    - PortOne API를 통해 환불 요청을 전송합니다.

2. **환불 상태 관리**:
    - 환불 요청 성공 시 결제 상태를 REFUNDED로 업데이트합니다.
    - 환불 실패 시 REFUND_REQUESTED 상태를 유지하고 관리자 개입이 필요합니다.

### 4. 알림 연동

결제 완료 시 구매자와 판매자에게 자동으로 알림을 발송합니다:
- 구매자에게는 "결제가 완료되었습니다" 메시지와 주문 ID 정보가 전달됩니다.
- 판매자에게는 "구매자가 결제를 완료하였습니다" 메시지와 판매 ID 정보가 전달됩니다.

## PortOne API 연동

### 1. 인증 토큰 발급

```java
public String getAccessToken() {
    String url = BASE_URL + "/users/getToken";
    // imp_key, imp_secret을 통한 인증 요청
    // ...
    return accessToken;
}
```

### 2. 결제 요청

```java
public Map<String, Object> processCardPayment(PaymentInfo paymentInfo, double amount) {
    String url = BASE_URL + "/subscribe/payments/onetime";
    String accessToken = getAccessToken();
    
    // 결제 요청 생성 및 전송
    // ...
    return responseMap;
}
```

### 3. 환불 요청

```java
public boolean cancelPayment(String impUid) {
    String url = BASE_URL + "/payments/cancel";
    String accessToken = getAccessToken();
    
    // 환불 요청 생성 및 전송
    // ...
    return refundSuccess;
}
```

## 보안 고려사항

1. **카드 정보 보안**:
    - 카드 번호는 일부만 표시하고 나머지는 마스킹 처리합니다.
    - 카드 정보는 PortOne을 통한 검증 과정을 거친 후에만 저장됩니다.

2. **접근 제어**:
    - 모든 결제 정보 API는 인증된 사용자만 접근 가능합니다.
    - 사용자는 자신의 결제 정보만 조회하고 관리할 수 있습니다.

## 구현 참고사항

### 1. 상속 구조를 활용한 결제 방식 관리

Payment 엔티티를 상속받아 다양한 결제 유형을 구현했습니다:

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment extends BaseTimeEntity {
    // 공통 필드 및 메서드
    
    // 하위 클래스에서 구현해야 하는 메서드
    public abstract String getImpUid();
}

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {
    // 카드 결제 관련 필드 및 메서드
}
```

### 2. 결제 상태 전이 유효성 검증

상태 변경 시 자동으로 유효성을 검사하여 불가능한 전환은 예외를 발생시킵니다:

```java
public void updateStatus(PaymentStatus newStatus) {
    if (this.status.canTransitionTo(newStatus)) {
        this.status = newStatus;
    } else {
        throw new IllegalStateException("결제 상태 전환이 허용되지 않습니다: " + this.status + " -> " + newStatus);
    }
}
```

### 3. 다형성을 활용한 결제 DTO 처리

다양한 결제 방식에 대응하기 위해 JSON SubTypes를 활용한 다형성을 구현했습니다:

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "paymentType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CardPaymentRequestDto.class, name = "CARD"),
    @JsonSubTypes.Type(value = AccountPaymentRequestDto.class, name = "ACCOUNT"),
    @JsonSubTypes.Type(value = GeneralPaymentRequestDto.class, name = "GENERAL")
})
public class PaymentRequestDto {
    // 공통 필드
}
```

## 확장 가능성

1. **추가 결제 방식 지원**: 현재의 상속 구조를 활용하여 새로운 결제 방식(e.g., 간편 결제, 가상 화폐 등)을 쉽게 추가할 수 있습니다.

2. **결제 통계 기능**: 사용자별, 기간별 결제 통계를 제공하는 기능을 추가할 수 있습니다.

3. **결제 예약 기능**: 특정 시점에 자동으로 결제되는 예약 결제 기능을 구현할 수 있습니다.

4. **할부 결제 지원**: 카드 결제 시 할부 기능을 추가로 구현할 수 있습니다.

5. **추가 PG사 연동**: 현재 PortOne 외에도 다양한 PG사 연동을 지원할 수 있도록 확장할 수 있습니다.