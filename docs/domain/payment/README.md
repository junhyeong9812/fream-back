# 결제 시스템 (Payment System)

## 개요

결제 시스템은 애플리케이션 내 다양한 결제 수단을 통합적으로 관리하는 모듈입니다. 이 시스템은 카드 결제, 계좌 이체, 일반 결제 등 여러 결제 방식을 지원하며, PortOne API를 통해 실제 결제 처리를 수행합니다. 또한 사용자별 결제 정보(카드 정보)를 안전하게 저장하고 관리하는 기능을 제공하며, 결제 상태 추적 기능도 구현되어 있습니다. 이 플랫폼은 중고거래 특성에 맞게 사용자 간 거래를 중개하는 역할에 중점을 두고 있습니다.

## 아키텍처

```
com.fream.back.domain.payment/
├── controller/
│   └── command/            # 결제 정보 생성, 삭제, 테스트 결제 컨트롤러
├── dto/
│   ├── paymentInfo/        # 결제 정보 관련 DTO
│   └── [payment types]/    # 다양한 결제 유형별 DTO
├── entity/                 # 데이터베이스 엔티티
├── exception/              # 결제 관련 예외 클래스
├── portone/                # PortOne 결제 API 연동
├── repository/             # 데이터 접근 계층
├── service/
│   ├── command/            # 결제 처리, 결제 정보 생성 서비스
│   └── query/              # 결제 조회, 결제 정보 조회 서비스
└── util/                   # 결제 관련 유틸리티 클래스
```

## 주요 구성 요소

### 컨트롤러

1. **PaymentInfoController**: 결제 정보 관리 API 제공 (생성, 조회, 삭제, 테스트 결제)

### 서비스

1. **PaymentCommandService**: 결제 처리 로직 (카드, 계좌, 일반 결제)
2. **PaymentInfoCommandService**: 결제 정보 생성 및 삭제 로직
3. **PaymentInfoQueryService**: 결제 정보 조회 로직
4. **PaymentQueryService**: 결제 내역 조회 로직
5. **PaymentEncryptionService**: 민감한 결제 정보 암호화/복호화 서비스

### 유틸리티

1. **PaymentCardUtils**: 카드 정보 검증 및 마스킹 유틸리티 클래스

### 엔티티

1. **Payment**: 결제 기본 정보를 담는 추상 클래스
2. **CardPayment**: 카드 결제 정보 (Payment 상속)
3. **AccountPayment**: 계좌 이체 결제 정보 (Payment 상속)
4. **GeneralPayment**: 일반 결제 정보 (Payment 상속)
5. **PaymentInfo**: 사용자의 결제 수단 정보 (카드 정보 등)
6. **PaymentStatus**: 결제 상태 열거형
7. **Bank**: 지원하는 은행 목록 열거형

### 예외 처리

1. **PaymentException**: 결제 관련 공통 예외 클래스
2. **PaymentInfoNotFoundException**: 결제 정보를 찾을 수 없을 때 발생하는 예외
3. **PaymentApiException**: 외부 API 연동 오류 시 발생하는 예외
4. **PaymentProcessingException**: 결제 처리 중 오류 발생 시 발생하는 예외

### 외부 연동

**PortOneApiClient**: PortOne API와의 통신을 담당하는 클라이언트 클래스 (재시도 메커니즘 포함)

## API 엔드포인트

### 결제 정보 관리 API

```
POST /api/v1/payment-info
```
새로운 결제 정보(카드)를 등록합니다. 등록 시 테스트 결제 및 즉시 환불을 통해 유효성을 검증합니다.

**요청 본문 예시:**
```json
{
  "cardNumber": "1234567890123456",
  "cardPassword": "12",
  "expirationDate": "12/25",
  "birthDate": "990101"
}
```

```
GET /api/v1/payment-info
```
사용자의 모든 결제 정보 목록을 조회합니다. 민감 정보는 마스킹 처리됩니다.

```
GET /api/v1/payment-info/{id}
```
특정 결제 정보를 조회합니다. 민감 정보는 마스킹 처리됩니다.

```
DELETE /api/v1/payment-info/{id}
```
특정 결제 정보를 삭제합니다.

```
POST /api/v1/payment-info/test-payment
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
| card_number     | VARCHAR        | 암호화된 카드 번호           |
| card_password   | VARCHAR        | 암호화된 카드 비밀번호 앞 두 자리  |
| card_expiration | VARCHAR        | 카드 유효기간                |
| birth_date      | VARCHAR        | 암호화된 생년월일            |
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
| card_number     | VARCHAR        | 암호화된 카드 번호           |
| card_password   | VARCHAR        | 암호화된 카드 비밀번호 앞 두 자리 |
| expiration_date | VARCHAR        | 유효기간                    |
| birth_date      | VARCHAR        | 암호화된 생년월일            |

## 결제 상태 흐름

### PaymentStatus 흐름

```
PENDING → PAID
       └→ REFUND_REQUESTED (사용 안함)
       └→ REFUNDED (테스트 결제 시 즉시 환불 경우)
```

- **PENDING**: 결제 대기 상태
- **PAID**: 결제 완료 상태
- **REFUND_REQUESTED**: 환불 요청 상태 (중고거래 특성상 일반적으로 사용 안함)
- **REFUNDED**: 환불 완료 상태 (테스트 결제 및 특수 케이스만 해당)

## 주요 기능 상세

### 1. 결제 정보 관리

1. **결제 정보 등록**:
    - 사용자가 카드 정보를 등록할 때, 실제 테스트 결제를 통해 유효성을 검증합니다.
    - 테스트 결제가 성공하면 즉시 환불되고, 검증된 카드 정보가 암호화되어 저장됩니다.
    - 사용자당 최대 5개의 결제 정보를 저장할 수 있습니다.
    - 모든 민감한 정보(카드번호, 비밀번호, 생년월일)는 AES 암호화되어 저장됩니다.

2. **결제 정보 조회**:
    - 사용자는 자신의 모든 결제 정보를 조회할 수 있습니다.
    - 결제 정보는 마스킹 처리되어 제공됩니다. (카드번호: 앞 4자리와 뒤 4자리만 표시)
    - 사용자는 자신이 등록한 결제 정보만 조회할 수 있습니다.

3. **결제 정보 삭제**:
    - 사용자는 등록된 결제 정보를 삭제할 수 있습니다.
    - 삭제된 정보는 복구할 수 없습니다.

### 2. 결제 처리 흐름

1. **카드 결제 처리**:
    - 저장된 결제 정보를 기반으로 PortOne API를 통해 결제를 요청합니다.
    - 결제 처리 전 암호화된 카드 정보를 복호화하여 API 요청에 사용합니다.
    - 결제 성공 시 CardPayment 객체를 생성하고 결제 상태를 PAID로 설정합니다.
    - 모든 API 요청은 실패 시 최대 3회까지 재시도되며, 재시도 간격은 설정 가능합니다.

2. **계좌 이체 결제 처리**:
    - 사용자가 입력한 계좌 정보를 검증하고 AccountPayment 객체를 생성합니다.
    - 결제 상태는 초기에 PENDING으로 설정됩니다.
    - 은행명은 Bank 열거형을 통해 유효성이 검증됩니다.

3. **일반 결제 처리**:
    - 외부에서 처리된 결제에 대한 정보를 받아 GeneralPayment 객체를 생성합니다.
    - 주로 외부 PG사를 통한 결제에 사용됩니다.

### 3. 알림 시스템

결제 완료 시 구매자와 판매자에게 자동으로 알림을 발송합니다:
- 구매자에게는 "결제가 완료되었습니다" 메시지와 주문 ID 정보가 전달됩니다.
- 판매자에게는 "구매자가 결제를 완료하였습니다" 메시지와 판매 ID 정보가 전달됩니다.
- 모든 알림은 NotificationCategory와 NotificationType에 따라 분류되어 관리됩니다.

### 4. 성능 모니터링 및 로깅

- 모든 API 요청 및 서비스 메서드는 처리 시간을 측정하고 로깅합니다.
- 민감한 정보(카드번호 등)는 로깅 시 마스킹 처리됩니다.
- 로그 레벨에 따라 상세한 요청/응답 내용을 확인할 수 있습니다.
- 모든 예외는 구체적인 오류 코드와 함께 로깅됩니다.

## PortOne API 연동

### 1. 인증 토큰 발급

```java
@Retryable(
    value = {PaymentApiException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public String getAccessToken() {
    Instant start = Instant.now();
    String url = BASE_URL + TOKEN_URL;
    
    log.info("PortOne API 토큰 발급 요청 시작");
    
    // imp_key, imp_secret을 통한 인증 요청
    // ...
    
    log.info("PortOne API 토큰 발급 성공: {}", tokenSubstring);
    
    // 처리 시간 로깅
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    log.debug("PortOne API 토큰 발급 처리 시간: {}ms", duration.toMillis());
    
    return accessToken;
}
```

### 2. 결제 요청

```java
@Retryable(
    value = {PaymentApiException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public Map<String, Object> processCardPayment(PaymentInfo paymentInfo, double amount) {
    Instant start = Instant.now();
    String url = BASE_URL + ONE_TIME_PAYMENT_URL;
    
    log.info("PortOne API 카드 결제 요청 시작: 금액={}", amount);

    // 결제 정보 검증
    validatePaymentInfo(paymentInfo, amount);
    
    // 인증 토큰 획득
    String accessToken = getAccessToken();
    
    // 결제 요청 생성 및 전송
    // ...
    
    log.info("PortOne API 카드 결제 성공: impUid={}", responseMap.get("imp_uid"));
    
    // 처리 시간 로깅
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    log.debug("PortOne API 카드 결제 처리 시간: {}ms", duration.toMillis());
    
    return responseMap;
}
```

### 3. 테스트 결제 및 환불 (카드 검증용)

```java
public String requestTestPayment(PaymentInfoCreateDto dto) {
    Instant start = Instant.now();
    log.info("PortOne API 테스트 결제 요청 시작: 카드번호={}", 
            PaymentCardUtils.maskCardNumberForLogging(dto.getCardNumber()));
    
    // 카드 정보 검증
    PaymentCardUtils.validateCardInfo(dto);
    
    // 테스트 결제 요청
    // ...
    
    log.info("PortOne API 테스트 결제 성공: impUid={}", impUid);
    
    // 처리 시간 로깅
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    log.debug("PortOne API 테스트 결제 처리 시간: {}ms", duration.toMillis());
    
    return impUid;
}

public boolean cancelTestPayment(String impUid) {
    log.info("테스트 결제 환불 요청 시작: impUid={}", impUid);
    return refundPayment(impUid); // 환불 메서드 재사용
}
```

## 보안 강화 사항

1. **민감 정보 암호화**:
    - 모든 카드 번호와 생년월일은 AES 알고리즘으로 암호화하여 저장합니다.
    - 암호화 키는 환경 변수와 설정 파일을 통해 관리됩니다.

2. **민감 정보 마스킹**:
    - 카드번호는 앞 4자리와 뒤 4자리만 표시하고 나머지는 마스킹 처리합니다.
    - 생년월일은 가운데 2자리만 표시하고 나머지는 마스킹 처리합니다.
    - 로그 파일에도 민감 정보는 마스킹 처리 후 기록됩니다.

3. **접근 제어**:
    - 모든 결제 정보 API는 인증된 사용자만 접근 가능합니다.
    - 사용자는 자신의 결제 정보만 조회하고 관리할 수 있습니다.
    - API 요청마다 사용자 인증 정보를 확인합니다.

4. **카드 정보 검증**:
    - 카드 정보는 등록 시 테스트 결제를 통한 유효성 검증을 거칩니다.
    - 유효하지 않은 카드 정보는 저장되지 않습니다.

## 구현 참고사항

### 1. 상속 구조를 활용한 결제 방식 관리

Payment 엔티티를 상속받아 다양한 결제 유형을 구현했습니다:

```java
@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment extends BaseTimeEntity {
    // 공통 필드 및 메서드
    
    // 상태 관리
    public void updateStatus(PaymentStatus newStatus) {
        if (this.status.canTransitionTo(newStatus)) {
            this.status = newStatus;
        } else {
            throw new IllegalStateException("결제 상태 전환이 허용되지 않습니다: " + this.status + " -> " + newStatus);
        }
    }
    
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
public enum PaymentStatus {
    PENDING, PAID, REFUND_REQUESTED, REFUNDED;

    public boolean canTransitionTo(PaymentStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PAID;
            case PAID -> newStatus == REFUND_REQUESTED || newStatus == REFUNDED;
            case REFUND_REQUESTED -> newStatus == REFUNDED;
            default -> false;
        };
    }
}
```

### 3. 다형성을 활용한 결제 DTO 처리

다양한 결제 방식에 대응하기 위해 JSON SubTypes를 활용한 다형성을 구현했습니다:

```java
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "paymentType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CardPaymentRequestDto.class, name = "CARD"),
    @JsonSubTypes.Type(value = AccountPaymentRequestDto.class, name = "ACCOUNT"),
    @JsonSubTypes.Type(value = GeneralPaymentRequestDto.class, name = "GENERAL")
})
@AllArgsConstructor
@NoArgsConstructor
public abstract class PaymentRequestDto {
    // 공통 필드
    @NotBlank(message = "결제 유형은 필수 입력 값입니다.")
    private String paymentType;
    
    @JsonIgnore
    private String resolvedType;
    
    @NotNull(message = "결제 금액은 필수 입력 값입니다.")
    @Min(value = 100, message = "결제 금액은 100원 이상이어야 합니다.")
    private double paidAmount;
    
    @NotNull(message = "주문 ID는 필수 입력 값입니다.")
    private Long orderId;
    
    @NotBlank(message = "사용자 이메일은 필수 입력 값입니다.")
    private String userEmail;
}
```

### 4. 예외 처리 체계

다양한 결제 관련 예외를 명확히 구분하고 중복 로직을 방지했습니다:

```java
// 결제 기본 예외 클래스
public class PaymentException extends GlobalException {
    // 구현 내용
}

// 특정 상황별 예외 클래스
public class PaymentInfoNotFoundException extends PaymentException {
    public PaymentInfoNotFoundException(String message) {
        super(PaymentErrorCode.PAYMENT_INFO_NOT_FOUND, message);
    }
}

public class PaymentApiException extends PaymentException {
    public PaymentApiException(String message) {
        super(PaymentErrorCode.PAYMENT_API_ERROR, message);
    }
}
```

### 5. 성능 최적화 및 모니터링

모든 중요 메서드에 성능 측정 코드를 추가했습니다:

```java
@Transactional
public String refundPayment(Long paymentId) {
    Instant start = Instant.now();
    try {
        // 메서드 구현
        // ...
    } finally {
        // 처리 시간 로깅
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        log.debug("결제 환불 처리 시간: {}ms", duration.toMillis());
    }
}
```

## 특이사항 및 제한사항

1. **중고거래 플랫폼 특성**:
    - 이 시스템은 중고거래 플랫폼의 특성에 맞게 구현되었습니다.
    - 플랫폼은 사용자 간 거래를 중개하는 역할을 하며, 결제 처리 후 환불은 일반적으로 지원하지 않습니다.
    - 판매가 완료된 상품(SOLD 상태)은 일반적으로 다시 판매 중 상태로 돌아가지 않습니다.

2. **테스트 결제 처리**:
    - 결제 정보 등록 시의 테스트 결제는 실제 결제 후 즉시 환불되는 방식으로 작동합니다.
    - 테스트 환불은 실제 거래 환불과는 별개의 프로세스입니다.

3. **서버 환경 의존성**:
    - 암호화 키와 API 인증 정보는 환경 변수 또는 설정 파일을 통해 제공되어야 합니다.
    - 필요한 환경 변수: PAYMENT_ENCRYPTION_KEY, PAYMENT_ENCRYPTION_SALT, PAYMENT_ENCRYPTION_IV, IMP_KEY, IMP_SECRET, IMP_PG, IMP_STOREID

## 확장 가능성

1. **추가 결제 방식 지원**: 현재의 상속 구조를 활용하여 새로운 결제 방식(e.g., 간편 결제, 가상 화폐 등)을 쉽게 추가할 수 있습니다.

2. **결제 통계 기능**: 사용자별, 기간별 결제 통계를 제공하는 기능을 추가할 수 있습니다.

3. **에스크로 지원**: 구매자 보호를 위한 에스크로 결제 기능을 추가할 수 있습니다.

4. **다국어 결제 지원**: 국제 결제를 위한 다양한 통화 및 언어 지원 기능을 추가할 수 있습니다.

5. **추가 PG사 연동**: 현재 PortOne 외에도 다양한 PG사 연동을 지원할 수 있도록 확장할 수 있습니다.