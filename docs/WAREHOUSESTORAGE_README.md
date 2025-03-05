# 창고 보관 시스템 (Warehouse Storage System)

## 개요

창고 보관 시스템은 구매 또는 판매 과정에서 상품을 임시로 보관하는 기능을 제공하는 모듈입니다. 이 시스템은 사용자가 구매한 상품을 보관하거나 판매자가 판매할 상품을 창고에 보관하는 기능을 지원합니다. 또한 보관 중인 상품의 상태를 추적하고, 보관 기간을 관리하며, 다양한 보관 상태(단순 보관 중, 구매와 연결된 보관, 판매 중, 판매 완료 등)를 처리합니다.

## 아키텍처

```
com.fream.back.domain.warehouseStorage/
├── controller/
│   ├── command/            # 창고 보관 명령 컨트롤러 (생략)
│   └── query/              # 창고 보관 조회 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 창고 보관 생성 및 상태 관리 서비스
    └── query/              # 창고 보관 조회 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **WarehouseStorageQueryController**: 창고 보관 조회 관련 API 제공

### 서비스

1. **WarehouseStorageCommandService**: 창고 보관 생성 및 상태 관리 로직
2. **WarehouseStorageQueryService**: 창고 보관 조회 및 통계 로직

### 엔티티

1. **WarehouseStorage**: 창고 보관 정보 (사용자, 주문, 판매, 보관 상태, 기간 등)
2. **WarehouseStatus**: 창고 보관 상태 열거형

### 저장소

**WarehouseStorageRepository**: 창고 보관 엔티티의 CRUD 및 조회 기능

## API 엔드포인트

### 조회 API

```
GET /warehouse-storage/queries/status-count
```
현재 사용자의 보관 상태별 개수를 조회합니다.

**응답 예시:**
```json
{
  "inStorageCount": 5,
  "associatedWithOrderCount": 2,
  "removedFromStorageCount": 1,
  "onAuctionCount": 3,
  "soldCount": 7
}
```

## 데이터베이스 스키마

### WAREHOUSE_STORAGE 테이블

| 필드               | 타입           | 설명                        |
|-------------------|----------------|---------------------------|
| id                | BIGINT         | 기본 키                     |
| user_id           | BIGINT         | 사용자 외래 키               |
| order_id          | BIGINT         | 주문 외래 키 (nullable)      |
| sale_id           | BIGINT         | 판매 외래 키 (nullable)      |
| storage_location  | VARCHAR        | 창고 위치 정보               |
| start_date        | DATE           | 보관 시작 날짜               |
| end_date          | DATE           | 보관 종료 날짜               |
| status            | VARCHAR        | 보관 상태 (ENUM 문자열)      |
| is_linked_to_order| BOOLEAN        | 구매와 연결된 보관 여부       |

## 창고 보관 상태 (WarehouseStatus)

창고 보관은 다음과 같은 상태를 가질 수 있습니다:

- **IN_STORAGE**: 단순 보관 중 - 상품이 창고에 보관된 초기 상태
- **ASSOCIATED_WITH_ORDER**: 구매와 연결된 보관 - 구매 주문과 연결되어 보관 중인 상태
- **REMOVED_FROM_STORAGE**: 보관 해제 - 창고에서 제거된 상태
- **ON_AUCTION**: 입찰 중 - 상품이 판매 입찰 중인 상태
- **SOLD**: 판매 완료 - 창고에 보관된 상품이 판매 완료된 상태

## 주요 기능 상세

### 1. 구매자 보관 관리

1. **구매자 보관 생성**:
    - `WarehouseStorageCommandService.createOrderStorage` 메서드를 통해 구매자 보관 정보 생성
    - 구매 주문(Order)과 사용자(User) 정보를 연결
    - 초기 상태는 `IN_STORAGE`로 설정
    - 기본 보관 기간은 1개월로 설정 (시작일: 현재, 종료일: 현재 + 1개월)

   ```java
   public WarehouseStorage createOrderStorage(Order order, User user) {
       WarehouseStorage warehouseStorage = WarehouseStorage.builder()
               .user(user)
               .order(order)
               .storageLocation("Default Location")
               .status(WarehouseStatus.IN_STORAGE)
               .startDate(LocalDate.now())
               .endDate(LocalDate.now().plusMonths(1))
               .build();

       return warehouseStorageRepository.save(warehouseStorage);
   }
   ```

### 2. 판매자 보관 관리

1. **판매자 보관 생성**:
    - `WarehouseStorageCommandService.createSellerStorage` 메서드를 통해 판매자 보관 정보 생성
    - 판매(Sale)와 판매자(User) 정보를 연결
    - 초기 상태는 `IN_STORAGE`로 설정
    - 기본 보관 기간은 1개월로 설정

   ```java
   public WarehouseStorage createSellerStorage(Sale sale, User seller) {
       WarehouseStorage warehouseStorage = WarehouseStorage.builder()
               .user(seller)
               .sale(sale)
               .storageLocation("Seller's Warehouse")
               .status(WarehouseStatus.IN_STORAGE)
               .startDate(LocalDate.now())
               .endDate(LocalDate.now().plusMonths(1))
               .build();

       return warehouseStorageRepository.save(warehouseStorage);
   }
   ```

### 3. 보관 상태 관리

1. **상태 업데이트**:
    - `WarehouseStorageCommandService.updateWarehouseStatus` 메서드를 통해 보관 상태 업데이트
    - 판매 완료 시 `updateWarehouseStatusToSold` 메서드를 통해 상태를 `SOLD`로 변경

   ```java
   public void updateWarehouseStatus(Sale sale, WarehouseStatus newStatus) {
       WarehouseStorage storage = warehouseStorageQueryService.findBySale(sale);
       storage.updateStatus(newStatus);
       warehouseStorageRepository.save(storage);
   }
   
   public void updateWarehouseStatusToSold(Sale sale) {
       if (sale.isWarehouseStorage()) {
           WarehouseStorage storage = warehouseStorageQueryService.findBySale(sale);
           storage.updateStatus(WarehouseStatus.SOLD);
           warehouseStorageRepository.save(storage);
       }
   }
   ```

### 4. 보관 상태 통계

1. **상태별 개수 조회**:
    - `WarehouseStorageQueryService.getWarehouseStatusCount` 메서드를 통해 상태별 보관 개수 조회
    - 사용자 이메일 기준으로 해당 사용자의 보관 상태 통계 제공

   ```java
   public WarehouseStatusCountDto getWarehouseStatusCount(String userEmail) {
       List<WarehouseStorage> storages = warehouseStorageRepository.findByUser_Email(userEmail);

       return WarehouseStatusCountDto.builder()
               .inStorageCount(countByStatus(storages, WarehouseStatus.IN_STORAGE))
               .associatedWithOrderCount(countByStatus(storages, WarehouseStatus.ASSOCIATED_WITH_ORDER))
               .removedFromStorageCount(countByStatus(storages, WarehouseStatus.REMOVED_FROM_STORAGE))
               .onAuctionCount(countByStatus(storages, WarehouseStatus.ON_AUCTION))
               .soldCount(countByStatus(storages, WarehouseStatus.SOLD))
               .build();
   }
   ```

## 연관 관계 관리

WarehouseStorage 엔티티는 다른 엔티티와 다음과 같은 연관 관계를 가집니다:

1. **User와의 관계**: 다대일(N:1) - 사용자는 여러 창고 보관을 가질 수 있음
2. **Order와의 관계**: 일대일(1:1) - 하나의 주문에 하나의 창고 보관만 연결 가능
3. **Sale과의 관계**: 일대일(1:1) - 하나의 판매에 하나의 창고 보관만 연결 가능

주요 연관 관계 관리 메서드:

```java
public void assignOrder(Order order) {
    this.order = order;
    this.sale = null; // 배타적 관계 설정
    this.isLinkedToOrder = true;
    this.status = WarehouseStatus.ASSOCIATED_WITH_ORDER;
}

public void assignSale(Sale sale) {
    this.sale = sale;
    this.order = null; // 배타적 관계 설정
    this.isLinkedToOrder = false;
    this.status = WarehouseStatus.IN_STORAGE;
}
```

## 보관 기간 관리

WarehouseStorage 엔티티는 보관 기간을 관리하기 위한 다음과 같은 메서드를 제공합니다:

```java
public void setStorageDates(LocalDate startDate, int initialPeriodDays) {
    this.startDate = startDate;
    this.endDate = startDate.plusDays(initialPeriodDays);
}

public void extendStorage(int additionalDays) {
    this.endDate = this.endDate.plusDays(additionalDays);
}
```

## 확장 가능성

1. **보관 연장 기능**: 사용자가 보관 기간을 연장할 수 있는 API 및 로직 추가
2. **보관 비용 계산**: 보관 기간에 따른 비용 계산 기능 추가
3. **보관 위치 추적**: 더 상세한 창고 위치 정보 및 상품 위치 추적 기능 추가
4. **보관 알림 기능**: 보관 기간 종료 전 알림 발송 기능 추가
5. **재고 관리 연동**: 창고 내 재고 관리 시스템과 연동하여 통합 관리 기능 추가