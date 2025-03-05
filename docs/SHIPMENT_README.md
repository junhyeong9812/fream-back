# 배송 시스템 (Shipment System)

## 개요

배송 시스템은 주문 및 판매 프로세스에서 상품의 배송 정보와 상태를 관리하는 모듈입니다. 이 시스템은 구매자 배송(OrderShipment)과 판매자 배송(SellerShipment)을 관리하며, CJ대한통운과 같은 외부 배송 추적 API를 연동하여 실시간 배송 상태를 모니터링합니다. 배송 상태 변경 시 알림 발송, 주문 상태 업데이트, 자동화된 배치 처리 등 다양한 기능을 제공합니다.

## 아키텍처

```
com.fream.back.domain.shipment/
├── config/                # 배치 처리 관련 설정
│   ├── BrowserManageStepListener    # Playwright 브라우저 관리 리스너
│   ├── ShipmentItemProcessor        # 배송 상태 처리 프로세서
│   └── UpdateShipmentStatusesJobConfig  # 배송 상태 갱신 배치 작업 설정
├── controller/
│   ├── command/          # 배송 정보 등록 및 수정 컨트롤러
│   └── query/            # 배송 정보 조회 컨트롤러 (생략됨)
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티
├── repository/           # 데이터 접근 계층
└── service/
    ├── command/          # 배송 정보 관리 및 상태 업데이트 서비스
    └── query/            # 배송 정보 조회 서비스 (생략됨)
```

## 주요 구성 요소

### 컨트롤러

1. **OrderShipmentCommandController**: 구매자 배송 정보 관리 API 제공
2. **SellerShipmentCommandController**: 판매자 배송 정보 관리 API 제공

### 서비스

1. **OrderShipmentCommandService**: 구매자 배송 정보 관리 및 상태 업데이트 로직
2. **SellerShipmentCommandService**: 판매자 배송 정보 관리 및 상태 업데이트 로직

### 엔티티

1. **OrderShipment**: 구매자 배송 정보 (수령인, 주소, 송장번호 등)
2. **SellerShipment**: 판매자 배송 정보 (송장번호, 배송사 등)
3. **ShipmentStatus**: 배송 상태 열거형

### 저장소

1. **OrderShipmentRepository**: 구매자 배송 정보 엔티티의 CRUD 및 조회
2. **SellerShipmentRepository**: 판매자 배송 정보 엔티티의 CRUD 및 조회

### 배치 처리

1. **UpdateShipmentStatusesJobConfig**: 배송 상태 주기적 갱신 배치 작업 설정
2. **ShipmentItemProcessor**: 개별 배송 상태 처리 로직
3. **BrowserManageStepListener**: Playwright 브라우저 생명주기 관리

## API 엔드포인트

### 구매자 배송 API (OrderShipment)

```
PATCH /shipments/order/{shipmentId}/status
```
구매자 배송 정보의 택배사와 송장번호를 업데이트합니다.

**요청 본문 예시:**
```json
{
  "courier": "CJ대한통운",
  "trackingNumber": "123456789012"
}
```

```
POST /shipments/order/{shipmentId}/check-status
```
구매자 배송 정보를 업데이트하고 현재 배송 상태를 즉시 확인합니다.

**요청 본문 예시:**
```json
{
  "courier": "CJ대한통운",
  "trackingNumber": "123456789012"
}
```

**응답 예시:**
```json
{
  "status": "DELIVERED"
}
```

```
POST /shipments/order/{shipmentId}/check-status-string
```
구매자 배송 정보를 업데이트하고 현재 배송 상태를 문자열로 반환합니다.

### 판매자 배송 API (SellerShipment)

```
POST /shipments/seller
```
판매자 배송 정보를 생성합니다.

**요청 본문 예시:**
```json
{
  "saleId": 123,
  "courier": "CJ대한통운",
  "trackingNumber": "123456789012"
}
```

```
PATCH /shipments/seller/{shipmentId}
```
판매자 배송 정보를 업데이트합니다.

**요청 본문 예시:**
```json
{
  "courier": "CJ대한통운",
  "trackingNumber": "123456789012"
}
```

## 데이터베이스 스키마

### 1. ORDER_SHIPMENT 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| order_id        | BIGINT         | 주문 외래 키                 |
| receiver_name   | VARCHAR        | 수령인 이름                  |
| receiver_phone  | VARCHAR        | 수령인 전화번호               |
| postal_code     | VARCHAR        | 우편번호                    |
| address         | VARCHAR        | 주소                        |
| courier         | VARCHAR        | 택배사 이름                  |
| tracking_number | VARCHAR        | 송장 번호                    |
| status          | VARCHAR        | 배송 상태(ENUM 문자열)        |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. SELLER_SHIPMENT 테이블

| 필드             | 타입           | 설명                        |
|-----------------|----------------|---------------------------|
| id              | BIGINT         | 기본 키                     |
| sale_id         | BIGINT         | 판매 외래 키                 |
| courier         | VARCHAR        | 택배사 이름                  |
| tracking_number | VARCHAR        | 송장 번호                    |
| status          | VARCHAR        | 배송 상태(ENUM 문자열)        |
| created_date    | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date   | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

## 배송 상태 흐름

ShipmentStatus 열거형은 다음과 같은 상태 흐름을 가집니다:

```
PENDING → SHIPPED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
        └→ CANCELED
                   └→ RETURNED → CANCELED
                   └→ DELAYED → IN_TRANSIT
                             └→ CANCELED
                   └→ FAILED_DELIVERY → RETURNED
                                      └→ OUT_FOR_DELIVERY
                                      └→ CANCELED
```

- **PENDING**: 배송 대기 상태
- **SHIPPED**: 배송 시작 상태
- **IN_TRANSIT**: 배송 중 상태
- **OUT_FOR_DELIVERY**: 배송 출발 상태
- **DELIVERED**: 배송 완료 상태
- **RETURNED**: 반송 상태
- **CANCELED**: 취소 상태
- **DELAYED**: 배송 지연 상태
- **FAILED_DELIVERY**: 배송 실패 상태

각 상태는 `canTransitionTo` 메서드를 통해 다음 상태로의 전환 가능 여부를 검증합니다:

```java
public boolean canTransitionTo(ShipmentStatus nextStatus) {
    return switch (this) {
        case IN_TRANSIT -> nextStatus == OUT_FOR_DELIVERY || 
                           nextStatus == DELAYED || 
                           nextStatus == CANCELED ||
                           nextStatus == DELIVERED;
        case OUT_FOR_DELIVERY -> nextStatus == DELIVERED || 
                                 nextStatus == FAILED_DELIVERY || 
                                 nextStatus == CANCELED;
        // ... 다른 상태들
    };
}
```

## 주요 기능 상세

### 1. 배송 정보 관리

#### 구매자 배송 정보 관리

1. **배송 정보 생성**:
    - `OrderShipmentCommandService.createOrderShipment` 메서드로 구매자 배송 정보 생성
    - 수령인 이름, 전화번호, 주소 정보 포함
    - 초기 상태는 `PENDING`으로 설정

2. **배송 정보 업데이트**:
    - `OrderShipmentCommandService.updateTrackingInfo` 메서드로 택배사, 송장번호 업데이트
    - 배송 상태가 `IN_TRANSIT`으로 변경되고 주문 상태도 업데이트
    - 구매자에게 알림 발송

#### 판매자 배송 정보 관리

1. **배송 정보 생성**:
    - `SellerShipmentCommandService.createSellerShipment` 메서드로 판매자 배송 정보 생성
    - 택배사, 송장번호 포함
    - 초기 상태는 `IN_TRANSIT`으로 설정
    - 창고 보관 옵션에 따라 추가 처리 수행

2. **배송 정보 업데이트**:
    - `SellerShipmentCommandService.updateShipment` 메서드로 택배사, 송장번호 업데이트

### 2. 배송 상태 모니터링

1. **실시간 배송 상태 확인**:
    - `OrderShipmentCommandService.updateAndCheckShipmentStatus` 메서드로 현재 배송 상태 즉시 확인
    - CJ대한통운과 같은 외부 API를 통해 실제 배송 상태 조회
    - 배송 상태에 따라 주문 및 알림 처리

2. **배치 처리를 통한 자동 상태 갱신**:
    - `UpdateShipmentStatusesJobConfig`에 정의된 배치 작업으로 주기적 상태 갱신
    - `ShipmentItemProcessor`에서 각 배송 정보별 상태 처리
    - Playwright를 활용한 웹 스크래핑으로 배송사 사이트에서 상태 추출

### 3. 배송 완료 처리

배송 상태가 `DELIVERED`로 변경될 때 다음과 같은 후속 처리가 수행됩니다:

1. **주문 상태 업데이트**:
    - `OrderShipmentCommandService.completeOrder` 메서드로 주문 상태를 `COMPLETED`로 변경
    - 관련 판매 상태도 `SOLD`로 업데이트

2. **알림 발송**:
    - 구매자에게 배송 완료 알림 발송
    - 판매자에게 판매 완료 알림 발송

3. **입찰 상태 업데이트**:
    - OrderBid 및 SaleBid의 상태를 `COMPLETED`로 변경

### 4. Playwright를 활용한 배송 추적

CJ대한통운과 같은 배송사 사이트에서 배송 상태를 추적하기 위해 Playwright를 활용합니다:

1. **브라우저 관리**:
    - `PlaywrightBrowserManager`를 통해 브라우저 인스턴스 관리
    - 배치 작업의 시작과 종료 시점에 브라우저 열기/닫기 수행

2. **배송 상태 추출**:
    - `CjTrackingPlaywright.getCurrentTrackingStatus` 메서드로 배송 상태 추출
    - 송장번호로 배송사 사이트 접속 및 상태 페이지 파싱
    - 추출된 상태를 시스템 내부 상태로 매핑

## 배치 작업 설정

배송 상태를 자동으로 갱신하기 위한 배치 작업이 설정되어 있습니다:

```java
@Bean
public Job updateShipmentStatusesJob() {
    return new JobBuilder("updateShipmentStatusesJob", jobRepository)
            .start(updateShipmentStatusesStep())
            .build();
}

@Bean
public Step updateShipmentStatusesStep() {
    return new StepBuilder("updateShipmentStatusesStep", jobRepository)
            .<OrderShipment, OrderShipment>chunk(50, transactionManager)
            .reader(shipmentItemReader())
            .processor(new ShipmentItemProcessor(...))
            .writer(shipmentJpaItemWriter())
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(50)
            .listener(shipmentSkipListener())
            .listener(new BrowserManageStepListener(playwrightBrowserManager()))
            .build();
}
```

이 배치 작업은 다음과 같은 단계로 실행됩니다:

1. **Reader**: `IN_TRANSIT` 또는 `OUT_FOR_DELIVERY` 상태의 OrderShipment 목록을 조회
2. **Processor**: 각 OrderShipment에 대해 CJ대한통운 페이지를 조회하여 상태 갱신
3. **Writer**: 변경된 상태를 데이터베이스에 저장
4. **Listener**: 예외 처리 및 브라우저 리소스 관리

## 구현 참고사항

### 1. 상태 전이 로직

배송 상태 변경 시 자동으로 유효성을 검사하는 로직이 엔티티에 구현되어 있습니다:

```java
public void updateStatus(ShipmentStatus newStatus) {
    if (this.status == null || this.status.canTransitionTo(newStatus)) {
        this.status = newStatus;
    } else {
        throw new IllegalStateException(
                "Cannot transition from " + this.status + " to " + newStatus
        );
    }
}
```

### 2. Playwright 활용한 웹 스크래핑

배송 상태 추적을 위해 Playwright를 활용한 웹 스크래핑 로직이 구현되어 있습니다:

```java
public String getCurrentTrackingStatus(String trackingNumber) throws Exception {
    Page page = browserManager.newPage();
    page.navigate("https://trace.cjlogistics.com/next/tracking.html?wblNo=" + trackingNumber);
    page.waitForSelector("tbody#statusDetail tr");
    String renderedHtml = page.content();
    page.close();
    
    // HTML 파싱 및 상태 추출
    // ...
    
    return statusText;
}
```

### 3. 트랜잭션 관리

모든 배송 정보 생성 및 상태 변경은 트랜잭션으로 처리되어 데이터 일관성을 유지합니다:

```java
@Transactional
public ShipmentStatus updateAndCheckShipmentStatus(Long shipmentId, String courier, String trackingNumber) throws Exception {
    // 트랜잭션 내에서 여러 엔티티 상태 변경
    // ...
}
```

## 외부 시스템 연동

### 1. CJ대한통운 배송 추적

CJ대한통운 배송 조회 페이지를 스크래핑하여 배송 상태를 추적합니다:

1. **URL**: `https://trace.cjlogistics.com/next/tracking.html?wblNo={trackingNumber}`
2. **상태 매핑**:
    - "배송완료" → `DELIVERED`
    - "배송출발" → `OUT_FOR_DELIVERY`
    - 그 외 → `IN_TRANSIT`

### 2. 알림 시스템 연동

배송 상태 변경 시 `NotificationCommandService`를 통해 알림을 발송합니다:

```java
notificationCommandService.createNotification(
    buyer.getId(),
    NotificationCategory.SHOPPING,
    NotificationType.BID,
    "상품이 배송 완료되었습니다. 주문 ID: " + order.getId()
);
```

### 3. 주문 및 판매 시스템 연동

배송 상태에 따라 주문 및 판매 상태를 업데이트합니다:

```java
// 주문 완료 처리
order.updateStatus(OrderStatus.COMPLETED);

// 판매 완료 처리
sale.updateStatus(SaleStatus.SOLD);
```

## 확장 가능성

1. **다양한 배송사 지원**: 현재 CJ대한통운 외에도 다양한 배송사의 배송 추적 기능 추가
2. **배송 예정일 계산**: 배송사 정보를 기반으로 예상 배송 완료일 계산 기능
3. **배송 알림 세분화**: 각 배송 상태 변경에 따른 세분화된 알림 발송
4. **배송 통계 분석**: 배송 시간, 지역별 배송 통계 등 분석 기능
5. **사용자 배송 추적 인터페이스**: 사용자가 직접 배송 상태를 확인할 수 있는 인터페이스 제공