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
├── exception/              # 도메인 예외 처리
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
4. **OrderStatus**: 주문 상태 열거형 (상태 전이 규칙 포함)
5. **BidStatus**: 입찰 상태 열거형

### 저장소

1. **OrderRepository**: 주문 엔티티 기본 CRUD 및 조회
2. **OrderBidRepository**: 구매 입찰 엔티티 기본 CRUD 및 조회
3. **OrderItemRepository**: 주문 항목 엔티티 기본 CRUD 및 조회
4. **OrderBidRepositoryCustom**: 구매 입찰 관련 커스텀 쿼리 인터페이스
5. **OrderBidRepositoryImpl**: QueryDSL을 사용한 커스텀 쿼리 구현

### DTO

1. **OrderBidRequestDto**: 구매 입찰 생성 요청 DTO
2. **InstantOrderRequestDto**: 즉시 구매 요청 DTO
3. **PayAndShipmentRequestDto**: 결제 및 배송 정보 처리 요청 DTO
4. **OrderBidResponseDto**: 구매 입찰 응답 DTO
5. **OrderBidStatusCountDto**: 입찰 상태별 개수 응답 DTO

### 예외 처리

1. **OrderException**: 주문 도메인 기본 예외 클래스
2. **OrderErrorCode**: 주문 관련 에러 코드 정의 (접두사 'ORD'로 시작)
3. **구체적 예외 클래스**: 다양한 예외 상황에 대한 구체적인 예외 클래스 (예: OrderNotFoundException, InvalidBidPriceException 등)

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

**응답 예시:**
```json
{
  "success": true,
  "message": "주문 입찰이 성공적으로 생성되었습니다.",
  "data": 789  // 생성된 주문 입찰 ID
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

**응답 예시:**
```json
{
  "success": true,
  "message": "즉시 구매가 성공적으로 처리되었습니다.",
  "data": 123  // 생성된 주문 ID
}
```

```
DELETE /order-bids/{orderBidId}
```
구매 입찰을 삭제합니다. 아직 매칭되지 않은 입찰만 삭제 가능합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "주문 입찰이 성공적으로 삭제되었습니다.",
  "data": null
}
```

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

**응답 예시:**
```json
{
  "success": true,
  "message": "결제 및 배송 처리가 성공적으로 완료되었습니다.",
  "data": null
}
```

### 구매 입찰 조회 API

```
GET /order-bids?bidStatus=PENDING&orderStatus=PAYMENT_COMPLETED&page=0&size=10
```
구매 입찰 목록을 조회합니다. 입찰 상태와 주문 상태로 필터링 가능합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "주문 입찰 목록 조회 성공",
  "data": {
    "content": [
      {
        "orderBidId": 1,
        "productId": 101,
        "productName": "나이키 에어포스 1",
        "productEnglishName": "Nike Air Force 1",
        "size": "260",
        "colorName": "White",
        "imageUrl": "https://example.com/images/airforce1_white.jpg",
        "bidPrice": 120000,
        "bidStatus": "PENDING",
        "orderStatus": "PAYMENT_COMPLETED",
        "shipmentStatus": null,
        "createdDate": "2023-05-15T10:30:00",
        "modifiedDate": "2023-05-15T10:30:00"
      },
      // ...
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 45,
    "totalPages": 5,
    "last": false,
    "size": 10,
    "number": 0,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "numberOfElements": 10,
    "first": true,
    "empty": false
  }
}
```

```
GET /order-bids/count
```
상태별 구매 입찰 개수를 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "주문 입찰 상태별 개수 조회 성공",
  "data": {
    "pendingCount": 5,
    "matchedCount": 3,
    "cancelledOrCompletedCount": 10
  }
}
```

```
GET /order-bids/{orderBidId}
```
특정 구매 입찰의 상세 정보를 조회합니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "주문 입찰 상세 정보 조회 성공",
  "data": {
    "orderBidId": 1,
    "productId": 101,
    "productName": "나이키 에어포스 1",
    "productEnglishName": "Nike Air Force 1",
    "size": "260",
    "colorName": "White",
    "imageUrl": "https://example.com/images/airforce1_white.jpg",
    "bidPrice": 120000,
    "bidStatus": "PENDING",
    "orderStatus": "PAYMENT_COMPLETED",
    "shipmentStatus": null,
    "createdDate": "2023-05-15T10:30:00",
    "modifiedDate": "2023-05-15T10:30:00"
  }
}
```

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

## 개선된 구현 내용

### 1. 상태 전이 로직 개선

OrderStatus 열거형에서 상태 전이 규칙을 Map과 Set을 사용하여 더 명확하고 확장 가능하게 개선했습니다:

```java
public enum OrderStatus {
    PENDING_PAYMENT, PAYMENT_COMPLETED, PREPARING, IN_WAREHOUSE, 
    SHIPMENT_STARTED, IN_TRANSIT, COMPLETED, REFUND_REQUESTED, REFUNDED;

    // 각 상태에서 전이 가능한 다음 상태들을 정의
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        // 상태 전이 규칙 초기화
        ALLOWED_TRANSITIONS.put(PENDING_PAYMENT, Set.of(PAYMENT_COMPLETED, COMPLETED, IN_WAREHOUSE));
        ALLOWED_TRANSITIONS.put(PAYMENT_COMPLETED, Set.of(PREPARING, REFUND_REQUESTED, IN_WAREHOUSE));
        // ... 나머지 상태 전이 규칙 ...
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        Set<OrderStatus> allowedNextStates = ALLOWED_TRANSITIONS.get(this);
        return allowedNextStates != null && allowedNextStates.contains(newStatus);
    }
    
    public Set<OrderStatus> getAllowedNextStates() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(this, Collections.emptySet()));
    }
}
```

### 2. 엔티티 관계 설정 개선

OrderBid와 Order 간의 양방향 관계 설정을 더 안전하게 처리하도록 개선했습니다:

```java
public class OrderBid extends BaseTimeEntity {
    // ... 기존 필드 ...
    
    public void assignOrder(Order order) {
        // 기존 관계가 있으면 해제
        if (this.order != null && this.order != order) {
            Order oldOrder = this.order;
            this.order = null;
            if (oldOrder.getOrderBid() == this) {
                oldOrder.assignOrderBid(null);
            }
        }
        
        // 새 관계 설정
        this.order = order;
        
        // 매칭 상태로 변경
        if (order != null) {
            this.status = BidStatus.MATCHED;
            
            // Order 쪽에도 관계 설정
            if (order.getOrderBid() != this) {
                order.assignOrderBid(this);
            }
        }
    }
}
```

### 3. 컨트롤러 응답 표준화

모든 API 응답을 `ResponseDto` 클래스로 표준화하여 일관된 응답 형식을 제공합니다:

```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ResponseDto<T> success(T data) {
        return ResponseDto.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> success(T data, String message) {
        return ResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> fail(String message) {
        return ResponseDto.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
```

### 4. DTO 검증 강화

Bean Validation을 적용하여 입력 DTO의 유효성 검증을 강화했습니다:

```java
@Data
public class OrderBidRequestDto {
    private String userEmail;
    
    @NotNull(message = "상품 사이즈 정보가 필요합니다.")
    private Long productSizeId;
    
    @Min(value = 1, message = "입찰 가격은 0보다 커야 합니다.")
    private int bidPrice;
}
```

### 5. 리포지토리 쿼리 개선

OrderBidRepositoryImpl에서 QueryDSL 조건 처리를 BooleanBuilder를 사용하여 더 가독성 높고 안전하게 개선했습니다:

```java
@Override
public Page<OrderBidResponseDto> findOrderBidsByFilters(String email, String bidStatus, String orderStatus, Pageable pageable) {
    // ... 기존 코드 ...
    
    // 조건 빌더 생성
    BooleanBuilder whereBuilder = new BooleanBuilder();
    
    // 이메일 조건 추가
    if (StringUtils.hasText(email)) {
        whereBuilder.and(user.email.eq(email));
    }
    
    // 입찰 상태 조건 추가
    if (StringUtils.hasText(bidStatus)) {
        whereBuilder.and(orderBid.status.stringValue().eq(bidStatus));
    }
    
    // 주문 상태 조건 추가
    if (StringUtils.hasText(orderStatus)) {
        whereBuilder.and(order.status.stringValue().eq(orderStatus));
    }
    
    // Main Query
    List<OrderBidResponseDto> content = queryFactory
            .select(/* ... */)
            .from(orderBid)
            .where(whereBuilder)
            .fetch();
    
    // ... 나머지 코드 ...
}
```

### 6. 서비스 계층 메서드 분리

복잡한 비즈니스 로직을 여러 작은 메서드로 분리하여 가독성과 유지보수성을 향상시켰습니다:

```java
@Transactional
public OrderBid createOrderBid(String email, Long productSizeId, int bidPrice) {
    try {
        // 1. 입력값 검증
        validateCreateOrderBidInput(email, productSizeId, bidPrice);

        // 2. User 조회
        User user = userQueryService.findByEmail(email);

        // 3. ProductSize 조회
        ProductSize productSize = productSizeQueryService.findById(productSizeId)
                .orElseThrow(() -> new ProductSizeNotFoundException("해당 사이즈를 찾을 수 없습니다(ID: " + productSizeId + ")"));

        // 4. Order 생성
        Order order = orderCommandService.createOrderFromBid(user, productSize, bidPrice);

        // 5. OrderBid 생성 및 저장
        OrderBid orderBid = createAndSaveOrderBid(user, productSize, bidPrice, order);

        return orderBid;
    } catch (Exception e) {
        handleOrderBidCreationException(e, "주문 입찰 생성");
        return null; // 실행되지 않음 (예외가 던져짐)
    }
}

private void validateCreateOrderBidInput(String email, Long productSizeId, int bidPrice) {
    // 입력값 검증 로직
}

private OrderBid createAndSaveOrderBid(User user, ProductSize productSize, int bidPrice, Order order) {
    // OrderBid 생성 및 저장 로직
}

private void handleOrderBidCreationException(Exception e, String operation) {
    // 예외 처리 로직
}
```

### 7. 이메일 검증 로직 통합

SecurityUtils 클래스에 도메인별 이메일 검증 메서드를 추가하여 중복 코드를 제거했습니다:

```java
// 컨트롤러에서 사용
@PostMapping
public ResponseEntity<ResponseDto<Long>> createOrderBid(@RequestBody @Valid OrderBidRequestDto requestDto) {
    // 사용자 이메일 추출 및 검증
    String email = SecurityUtils.extractAndValidateEmailForOrderBid("주문 입찰 생성");
    
    // ... 나머지 코드 ...
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
6. **결제 방식 다양화**: 더 많은 결제 방식 지원 (가상계좌, 간편결제 등)
7. **다국어 지원**: 국제 배송 및 다국어 메시지 지원 기능