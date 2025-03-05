# 판매 시스템 (Sale System)

## 개요

판매 시스템은 상품 판매 프로세스의 핵심 모듈로, 판매 입찰(SaleBid), 판매(Sale), 판매자 계좌 정보(SaleBankAccount) 등을 통합적으로 관리합니다. 이 시스템은 일반 판매 입찰과 즉시 판매 두 가지 판매 경로를 지원하며, 판매 상태 관리, 배송 정보 관리, 창고 보관 옵션 등 다양한 기능을 제공합니다. 또한 구매 입찰(OrderBid)과의 매칭을 통해 거래가 이루어지는 전체 프로세스를 체계적으로 처리합니다.

## 아키텍처

```
com.fream.back.domain.sale/
├── controller/
│   ├── command/            # 판매 입찰 생성, 삭제 컨트롤러
│   └── query/              # 판매 입찰 조회 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 판매 입찰, 판매, 계좌 정보 관리 서비스
    └── query/              # 판매 입찰 조회 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **SaleBidCommandController**: 판매 입찰 생성 및 관리 API 제공
2. **SaleBidQueryController**: 판매 입찰 조회 API 제공

### 서비스

1. **SaleBidCommandService**: 판매 입찰 생성 및 상태 관리 로직
2. **SaleCommandService**: 판매 생성, 상태 관리, 배송 정보 설정 로직
3. **SaleBankAccountCommandService**: 판매자 계좌 정보 관리 로직
4. **SaleBidQueryService**: 판매 입찰 조회 로직

### 엔티티

1. **Sale**: 판매 정보 (판매자, 상품, 상태, 배송 정보 등)
2. **SaleBid**: 판매 입찰 정보 (판매자, 상품 사이즈, 입찰 가격, 상태 등)
3. **SaleBankAccount**: 판매자 계좌 정보 (은행명, 계좌번호, 예금주)
4. **SaleStatus**: 판매 상태 열거형
5. **BidStatus**: 입찰 상태 열거형

### 저장소

1. **SaleRepository**: 판매 엔티티 기본 CRUD 및 조회
2. **SaleBidRepository**: 판매 입찰 엔티티 기본 CRUD 및 조회
3. **SaleBankAccountRepository**: 판매자 계좌 정보 엔티티 기본 CRUD
4. **SaleBidRepositoryCustom**: 판매 입찰 관련 커스텀 쿼리
5. **SaleBidRepositoryImpl**: QueryDSL을 사용한 커스텀 쿼리 구현

## API 엔드포인트

### 판매 입찰 API (SaleBid)

```
POST /sale-bids
```
판매 입찰을 생성합니다. 상품 사이즈, 입찰 가격, 반송 주소 정보 등을 지정합니다.

**요청 본문 예시:**
```json
{
  "productSizeId": 123,
  "bidPrice": 150000,
  "returnAddress": "서울시 강남구 역삼동 123-45",
  "postalCode": "12345",
  "receiverPhone": "010-1234-5678",
  "warehouseStorage": true
}
```

```
POST /sale-bids/instant
```
즉시 판매를 생성합니다. 이미 존재하는 구매 입찰(OrderBid)과 매칭합니다.

**요청 본문 예시:**
```json
{
  "orderBidId": 456,
  "returnAddress": "서울시 강남구 역삼동 123-45",
  "postalCode": "12345",
  "receiverPhone": "010-1234-5678"
}
```

```
DELETE /sale-bids/{saleBidId}
```
판매 입찰을 삭제합니다. 아직 매칭되지 않은 입찰만 삭제 가능합니다.

### 판매 입찰 조회 API

```
GET /sale-bids?saleBidStatus=PENDING&saleStatus=PENDING_SHIPMENT&page=0&size=10
```
판매 입찰 목록을 조회합니다. 입찰 상태와 판매 상태로 필터링 가능합니다.

```
GET /sale-bids/count
```
상태별 판매 입찰 개수를 조회합니다.

```
GET /sale-bids/{saleBidId}
```
특정 판매 입찰의 상세 정보를 조회합니다.

## 데이터베이스 스키마

### 1. SALE 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| seller_id       | BIGINT         | 판매자 외래 키               |
| product_size_id | BIGINT         | 상품 사이즈 외래 키           |
| return_address  | VARCHAR        | 반송 주소                    |
| postal_code     | VARCHAR        | 우편번호                    |
| receiver_phone  | VARCHAR        | 수령인 전화번호               |
| is_warehouse_storage | BOOLEAN   | 창고 보관 여부               |
| status          | VARCHAR        | 판매 상태(ENUM 문자열)        |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. SALE_BID 테이블

| 필드                 | 타입           | 설명                        |
|---------------------|----------------|---------------------------|
| id                  | BIGINT         | 기본 키                     |
| seller_id           | BIGINT         | 판매자 외래 키               |
| product_size_id     | BIGINT         | 상품 사이즈 외래 키           |
| bid_price           | INT            | 입찰 가격                    |
| status              | VARCHAR        | 입찰 상태(ENUM 문자열)        |
| sale_id             | BIGINT         | 판매 외래 키                 |
| order_id            | BIGINT         | 주문 외래 키                 |
| is_instant_sale     | BOOLEAN        | 즉시 판매 여부                |
| created_date        | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date       | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 3. SALE_BANK_ACCOUNT 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| sale_id         | BIGINT         | 판매 외래 키                 |
| bank_name       | VARCHAR        | 은행명                      |
| account_number  | VARCHAR        | 계좌번호                    |
| account_holder  | VARCHAR        | 예금주명                    |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

## 상태 흐름도

### SaleStatus 흐름

```
PENDING_SHIPMENT → IN_TRANSIT → IN_INSPECTION → IN_STORAGE → ON_AUCTION → SOLD
                                            └→ FAILED_INSPECTION
                                                       └→ ON_AUCTION → AUCTION_EXPIRED
```

- **PENDING_SHIPMENT**: 판매자 발송 대기 상태
- **IN_TRANSIT**: 배송 중 상태
- **IN_INSPECTION**: 검수 중 상태
- **FAILED_INSPECTION**: 검수 불합격 상태
- **IN_STORAGE**: 창고 보관 중 상태
- **ON_AUCTION**: 판매 입찰 중 상태
- **SOLD**: 판매 완료 상태
- **AUCTION_EXPIRED**: 입찰 기한 만료 상태

### BidStatus 흐름

```
PENDING → MATCHED → COMPLETED
        └→ CANCELLED
```

- **PENDING**: 입찰 대기 중 상태
- **MATCHED**: 입찰 매칭 완료 상태
- **CANCELLED**: 입찰 취소 상태
- **COMPLETED**: 거래 완료 상태

## 주요 기능 상세

### 1. 판매 입찰 프로세스

1. **입찰 생성**:
    - 판매자가 상품 사이즈, 입찰 가격, 반송 주소 정보를 지정하여 판매 입찰을 생성합니다.
    - 입찰 상태는 `PENDING`으로 설정됩니다.
    - 동시에 `PENDING_SHIPMENT` 상태의 Sale 객체가 생성됩니다.
    - 판매자의 계좌 정보를 기반으로 SaleBankAccount 객체도 생성됩니다.

2. **입찰 매칭**:
    - 구매 입찰과 판매 입찰이 매칭되면 입찰 상태가 `MATCHED`로 변경됩니다.
    - 매칭 시 Sale 정보도 함께 업데이트됩니다.

3. **판매 처리**:
    - 판매자가 상품을 발송하면 Sale 상태가 `IN_TRANSIT`으로 업데이트됩니다.
    - 이후 검수 과정을 거쳐 Sale 상태가 계속 업데이트됩니다.
    - 판매 완료 시 입찰 상태는 `COMPLETED`로 변경됩니다.

### 2. 즉시 판매 프로세스

1. **즉시 판매**:
    - 판매자가 기존 구매 입찰(OrderBid)을 선택하여 즉시 판매를 진행합니다.
    - 반송 주소 정보를 제공합니다.
    - 판매 입찰 상태는 바로 `MATCHED`로 설정됩니다.

2. **판매 생성 및 계좌 정보 설정**:
    - Sale 객체가 생성되고 상태는 `PENDING_SHIPMENT`로 설정됩니다.
    - 판매자의 계좌 정보를 기반으로 SaleBankAccount 객체도 생성됩니다.

3. **알림 발송**:
    - 구매자에게 판매자가 등록되었다는 알림이 발송됩니다.

### 3. 판매 상태 관리

- 판매 상태는 `SaleStatus` 열거형을 통해 관리됩니다.
- 각 상태는 다음 상태로의 전환 가능 여부를 `canTransitionTo` 메서드로 검증합니다.
- 상태 변경 시 자동으로 유효성을 검사하여 불가능한 전환은 예외를 발생시킵니다.

### 4. 판매 연관 관계 관리

- Sale과 SaleBid는 일대일 관계로 관리됩니다.
- Sale과 SaleBankAccount는 일대일 관계로 관리됩니다.
- Sale, SaleBid, Payment, SellerShipment 등과의 연관 관계를 설정합니다.
- 모든 연관 관계는 양방향으로 설정하여 데이터 일관성을 유지합니다.

## 구현 참고사항

### 1. 상태 전이 로직

Sale 엔티티의 상태 전이 로직은 다음과 같이 구현되어 있습니다:

```java
public boolean canTransitionTo(SaleStatus newStatus) {
    return switch (this) {
        case PENDING_SHIPMENT -> newStatus == IN_TRANSIT;
        case IN_TRANSIT -> newStatus == IN_INSPECTION;
        case IN_INSPECTION -> newStatus == FAILED_INSPECTION || newStatus == IN_STORAGE;
        case IN_STORAGE -> newStatus == ON_AUCTION || newStatus == AUCTION_EXPIRED;
        case ON_AUCTION -> newStatus == SOLD || newStatus == AUCTION_EXPIRED;
        case SOLD, AUCTION_EXPIRED, FAILED_INSPECTION -> false;
    };
}
```

### 2. 비즈니스 로직 분리

- 명령(Command)과 조회(Query)를 분리하여 책임을 명확히 구분했습니다.
- Entity에는 도메인 로직을, Service에는 비즈니스 로직을 배치하여 관심사를 분리했습니다.
- Repository에서는 기본 CRUD 외에 복잡한 조회는 QueryDSL을 활용하여 구현했습니다.

### 3. 트랜잭션 관리

모든 판매 입찰 및 판매 생성, 상태 변경은 트랜잭션으로 처리되어 데이터 일관성을 유지합니다:

```java
@Transactional
public SaleBid createSaleBid(String sellerEmail, Long productSizeId, int bidPrice,
                             String returnAddress, String postalCode, String receiverPhone,
                             boolean isWarehouseStorage) {
    // 트랜잭션 내에서 여러 엔티티 생성 및 상태 설정
    // ...
}
```

### 4. 판매 입찰 조회를 위한 QueryDSL 활용

복잡한 조인과 필터링을 위해 QueryDSL을 활용한 커스텀 쿼리를 구현했습니다:

```java
@Override
public Page<SaleBidResponseDto> findSaleBidsByFilters(String email, String saleBidStatus, String saleStatus, Pageable pageable) {
    // QueryDSL을 사용한 복잡한 조회 쿼리 구현
    // ...
}
```

## 외부 시스템 연동

### 1. 알림 시스템 연동

- `NotificationCommandService`를 통해 판매 관련 알림을 발송합니다.
- 판매 입찰 등록, 매칭 완료 등의 이벤트에서 알림을 생성합니다.

### 2. 배송 시스템 연동

- `SellerShipmentCommandService`를 통해 판매자 배송 정보를 관리합니다.
- 배송 상태에 따라 Sale 상태를 업데이트합니다.

### 3. 결제 시스템 연동

- Sale과 Payment 간의 연관 관계를 통해 결제 정보를 관리합니다.
- 결제 완료 시 Sale 상태를 업데이트합니다.

## 확장 가능성

1. **판매 취소 기능**: 특정 조건에서 판매자가 판매를 취소할 수 있는 기능 추가
2. **판매 기한 설정**: 판매 입찰의 유효 기간을 설정하는 기능 추가
3. **가격 자동 조정**: 시장 상황에 따라 판매 가격을 자동으로 조정하는 기능 추가
4. **대량 판매 지원**: 동일한 상품을 여러 개 판매할 수 있는 기능 추가
5. **판매 분석 기능**: 판매 데이터를 분석하여 인사이트를 제공하는 기능 추가