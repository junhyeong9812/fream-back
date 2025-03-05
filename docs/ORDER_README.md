# 주문 시스템 (Order System)

## 개요

주문 시스템은 상품 구매 프로세스의 핵심 모듈로, 구매 입찰(OrderBid), 주문(Order), 주문 항목(OrderItem)을 통합적으로 관리합니다. 이 시스템은 일반 구매와 즉시 구매 두 가지 주문 경로를 지원하며, 결제 처리, 배송 정보 관리, 창고 보관 옵션 등 다양한 기능을 제공합니다. 또한 구매 입찰의 상태 관리를 통해 거래 매칭, 결제, 배송까지의 전체 프로세스를 체계적으로 처리합니다.

## 아키텍처

```
com.fream.back.domain.order/
├── controller/
│   ├── command/            # 주문 생성, 입찰, 결제 처리 컨트롤러
│   └── query/              # 주문 조회 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 주문 생성, 입찰, 결제 처리 서비스
    └── query/              # 주문 조회 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **OrderBidCommandController**: 구매 입찰 생성 및 관리 API 제공
2. **OrderCommandController**: 주문 처리 및 결제 관련 API 제공
3. **OrderBidQueryController**: 구매 입찰 조회 API 제공

### 서비스

1. **OrderBidCommandService**: 구매 입찰 생성 및 상태 관리 로직
2. **OrderCommandService**: 주문 생성, 결제 처리, 배송 정보 설정 로직
3. **OrderItemCommandService**: 주문 항목 생성 로직
4. **OrderBidQueryService**: 구매 입찰 조회 로직

### 엔티티

1. **Order**: 주문 정보 (사용자, 결제, 배송, 상태 등)
2. **OrderBid**: 구매 입찰 정보 (사용자, 상품 사이즈, 입찰 가격, 상태 등)
3. **OrderItem**: 주문 항목 정보 (상품, 수량, 가격 등)
4. **OrderStatus**: 주문 상태 열거형
5. **BidStatus**: 입찰 상태 열거형

### 저장소

1. **OrderRepository**: 주문 엔티티 기본 CRUD 및 조회
2. **OrderBidRepository**: 구매 입찰 엔티티 기본 CRUD 및 조회
3. **OrderItemRepository**: 주문 항목 엔티티 기본 CRUD 및 조회
4. **OrderBidRepositoryCustom**: 구매 입찰 관련 커스텀 쿼리
5. **OrderBidRepositoryImpl**: QueryDSL을 사용한 커스텀 쿼리 구현

## API 엔드포인트

### 구매 입찰 API (OrderBid)

```
POST /order-bids
```
구매 입찰을 생성합니다. 상품 사이즈와 입찰 가격을 지정합니다.

**요청 본문 예시:**
```json
{
  "productSizeId": 123,
  "bidPrice": 150000
}
```

```
POST /order-bids/instant
```
즉시 구매 입찰을 생성합니다. 판매 입찰 ID, 주소 정보, 창고 보관 여부, 결제 정보가 필요합니다.

**요청 본문 예시:**
```json
{
  "saleBidId": 456,
  "addressId": 789,
  "warehouseStorage": false,
  "paymentRequest": {
    "paymentMethod": "CARD",
    "amount": 150000
  }
}
```

```
DELETE /order-bids/{orderBidId}
```
구매 입찰을 삭제합니다. 아직 매칭되지 않은 입찰만 삭제 가능합니다.

### 주문 API (Order)

```
POST /orders/{orderId}/process-payment-shipment
```
주문에 대한 결제 및 배송 정보를 처리합니다.

**요청 본문 예시:**
```json
{
  "paymentRequest": {
    "paymentMethod": "CARD",
    "amount": 150000
  },
  "receiverName": "홍길동",
  "receiverPhone": "010-1234-5678",
  "postalCode": "12345",
  "address": "서울시 강남구 삼성동 123-45",
  "warehouseStorage": false
}
```

### 구매 입찰 조회 API

```
GET /order-bids?bidStatus=PENDING&orderStatus=PAYMENT_COMPLETED&page=0&size=10
```
구매 입찰 목록을 조회합니다. 입찰 상태와 주문 상태로 필터링 가능합니다.

```
GET /order-bids/count
```
상태별 구매 입찰 개수를 조회합니다.

```
GET /order-bids/{orderBidId}
```
특정 구매 입찰의 상세 정보를 조회합니다.

## 데이터베이스 스키마

### 1. ORDERS 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| user_id         | BIGINT         | 사용자 외래 키               |
| total_amount    | INT            | 총 주문 금액                 |
| discount_amount | INT            | 할인 금액                    |
| used_points     | INT            | 사용된 포인트                 |
| status          | VARCHAR        | 주문 상태(ENUM 문자열)        |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. ORDER_BID 테이블

| 필드                 | 타입           | 설명                        |
|---------------------|----------------|---------------------------|
| id                  | BIGINT         | 기본 키                     |
| user_id             | BIGINT         | 사용자 외래 키               |
| product_size_id     | BIGINT         | 상품 사이즈 외래 키           |
| bid_price           | INT            | 입찰 가격                    |
| status              | VARCHAR        | 입찰 상태(ENUM 문자열)        |
| order_id            | BIGINT         | 주문 외래 키                 |
| sale_id             | BIGINT         | 판매 외래 키                 |
| is_instant_purchase | BOOLEAN        | 즉시 구매 여부                |
| created_date        | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date       | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 3. ORDER_ITEM 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| order_id        | BIGINT         | 주문 외래 키                 |
| product_size_id | BIGINT         | 상품 사이즈 외래 키           |
| quantity        | INT            | 주문 수량                    |
| price           | INT            | 단가                        |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

## 주문 상태 흐름

### OrderStatus 흐름

```
PENDING_PAYMENT → PAYMENT_COMPLETED → PREPARING → SHIPMENT_STARTED → IN_TRANSIT → COMPLETED
               └→ REFUND_REQUESTED → REFUNDED
                 └→ IN_WAREHOUSE → SHIPMENT_STARTED
                                  └→ COMPLETED
```

- **PENDING_PAYMENT**: 결제 대기 상태
- **PAYMENT_COMPLETED**: 결제 완료 상태
- **PREPARING**: 상품 준비 중 상태
- **IN_WAREHOUSE**: 창고 보관 중 상태
- **SHIPMENT_STARTED**: 배송 시작 상태
- **IN_TRANSIT**: 배송 중 상태
- **COMPLETED**: 배송 완료/주문 완료 상태
- **REFUND_REQUESTED**: 환불 요청 상태
- **REFUNDED**: 환불 완료 상태

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

### 1. 구매 입찰 프로세스

1. **입찰 생성**:
    - 사용자가 상품 사이즈와 입찰 가격을 지정하여 구매 입찰을 생성합니다.
    - 입찰 상태는 `PENDING`으로 설정됩니다.
    - 동시에 `PENDING_PAYMENT` 상태의 Order 객체가 생성됩니다.

2. **입찰 매칭**:
    - 판매 입찰과 구매 입찰이 매칭되면 입찰 상태가 `MATCHED`로 변경됩니다.
    - 매칭 시 Order 정보도 함께 업데이트됩니다.

3. **주문 처리**:
    - 결제 및 배송 정보가 처리되면 주문 상태가 업데이트됩니다.
    - 입찰 상태는 주문 완료 시 `COMPLETED`로 변경됩니다.

### 2. 즉시 구매 프로세스

1. **즉시 구매**:
    - 사용자가 특정 판매 입찰을 선택하여 즉시 구매를 진행합니다.
    - 주소, 결제 정보를 함께 제출합니다.
    - 입찰 상태는 바로 `MATCHED`로 설정됩니다.

2. **주문 생성 및 결제**:
    - Order, OrderItem, OrderShipment 객체가 생성됩니다.
    - 결제가 즉시 처리되어 주문 상태가 `PAYMENT_COMPLETED`로 업데이트됩니다.

3. **창고 보관 옵션**:
    - 창고 보관을 선택한 경우 주문 상태가 `IN_WAREHOUSE`로 업데이트됩니다.
    - 창고 보관 관련 정보가 WarehouseStorage에 기록됩니다.

### 3. 주문 상태 관리

- 주문 상태는 `OrderStatus` 열거형을 통해 관리됩니다.
- 각 상태는 다음 상태로의 전환 가능 여부를 `canTransitionTo` 메서드로 검증합니다.
- 상태 변경 시 자동으로 유효성을 검사하여 불가능한 전환은 예외를 발생시킵니다.

### 4. 주문 연관 관계 관리

- Order와 OrderItem은 일대다 관계로 관리됩니다.
- Order와 OrderBid는 일대일 관계로 관리됩니다.
- Order와 Payment, OrderShipment, WarehouseStorage 등과도 연관 관계를 맺습니다.
- 모든 연관 관계는 양방향으로 설정하여 데이터 일관성을 유지합니다.

## 구현 참고사항

### 1. 상태 전이 로직

Order 엔티티의 상태 전이 로직은 다음과 같이 구현되어 있습니다:

```java
public boolean canTransitionTo(OrderStatus newStatus) {
    switch (this) {
        case PENDING_PAYMENT:
            return newStatus == PAYMENT_COMPLETED || newStatus == COMPLETED || newStatus == IN_WAREHOUSE;
        case PAYMENT_COMPLETED:
            return newStatus == PREPARING || newStatus == REFUND_REQUESTED || newStatus == IN_WAREHOUSE;
        case PREPARING:
            return newStatus == IN_WAREHOUSE || newStatus == SHIPMENT_STARTED;
        case IN_WAREHOUSE:
            return newStatus == SHIPMENT_STARTED || newStatus == COMPLETED;
        case SHIPMENT_STARTED:
            return newStatus == IN_TRANSIT;
        case IN_TRANSIT:
            return newStatus == COMPLETED;
        case REFUND_REQUESTED:
            return newStatus == REFUNDED;
        default:
            return false;
    }
}
```

### 2. 비즈니스 로직 분리

- 명령(Command)과 조회(Query)를 분리하여 책임을 명확히 구분했습니다.
- Entity에는 도메인 로직을, Service에는 비즈니스 로직을 배치하여 관심사를 분리했습니다.
- Repository에서는 기본 CRUD 외에 복잡한 조회는 QueryDSL을 활용하여 구현했습니다.

### 3. 트랜잭션 관리

모든 주문 생성 및 상태 변경은 트랜잭션으로 처리되어 데이터 일관성을 유지합니다:

```java
@Transactional
public Order createInstantOrder(User buyer, SaleBid saleBid, Long addressId,
                                boolean isWarehouseStorage, PaymentRequestDto paymentRequest) {
    // 트랜잭션 내에서 여러 엔티티 생성 및 상태 변경
    // ...
}
```

## 외부 시스템 연동

### 1. 결제 시스템 연동

- `PaymentCommandService`를 통해 결제 처리를 수행합니다.
- 결제 완료 시 Order 상태를 업데이트합니다.

### 2. 배송 시스템 연동

- `OrderShipmentCommandService`를 통해 배송 정보를 관리합니다.
- 배송 상태에 따라 Order 상태를 업데이트합니다.

### 3. 창고 보관 시스템 연동

- `WarehouseStorageCommandService`를 통해 창고 보관 정보를 관리합니다.
- 창고 보관 시 관련 상태를 업데이트합니다.

## 확장 가능성

1. **주문 취소 기능**: 주문 취소 및 환불 처리 로직 추가
2. **부분 배송 지원**: 여러 상품을 포함한 주문의 부분 배송 지원
3. **배송 추적 기능**: 외부 배송 API와 연동하여 실시간 배송 추적 제공
4. **재고 관리 연동**: 주문 처리 시 자동으로 재고를 업데이트하는 기능
5. **주문 분석 기능**: 주문 데이터를 분석하여 인사이트를 제공하는 기능