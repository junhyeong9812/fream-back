# FREAM 제품 도메인

## 개요

제품 도메인은 FREAM 애플리케이션의 핵심 구성 요소로, 제품, 색상, 사이즈 및 관련 정보를 관리합니다. 이 도메인은 제품과 관련된 데이터의 생성, 수정, 조회, 삭제를 위한 포괄적인 기능을 제공합니다.

## 주요 기능

- **제품 관리**: 상세 메타데이터를 포함한 제품의 생성, 수정, 삭제
- **색상 및 사이즈 관리**: 각 제품에 대한 다양한 색상 및 사이즈 지원
- **카테고리 계층 구조**: 카테고리와 서브카테고리를 통한 제품의 계층적 구성
- **브랜드 및 컬렉션 관리**: 브랜드와 컬렉션별 제품 구성
- **관심 상품 시스템**: 사용자가 제품을 즐겨찾기로 등록할 수 있는 기능
- **조회 추적**: Kafka를 사용한 제품 조회 이벤트 처리
- **고급 검색 및 필터링**: QueryDSL 기반의 다양한 필터링 옵션을 가진 제품 검색
- **이미지 관리**: 썸네일 및 상세 이미지를 포함한 제품 이미지 처리

## 도메인 모델

### 핵심 엔티티

- **Product**: 기본 정보를 가진 제품을 나타내는 주요 엔티티
- **ProductColor**: 제품의 다양한 색상 변형을 나타냄
- **ProductSize**: 각 제품 색상에 대해 제공되는 다양한 사이즈를 나타냄
- **Category**: 계층적 구조의 제품 카테고리를 나타냄
- **Brand**: 제품 브랜드를 나타냄
- **Collection**: 제품 컬렉션을 나타냄
- **Interest**: 특정 제품 색상에 대한 사용자의 관심을 나타냄
- **ProductImage/ProductDetailImage**: 제품 이미지 관리
- **ProductColorViewLog**: 제품 조회 이벤트 추적
- **ProductPriceHistory**: 시간에 따른 가격 변화 추적

### 엔티티 관계

```
Product
  ├── ProductColor (일대다)
  │     ├── ProductSize (일대다)
  │     ├── ProductImage (일대다)
  │     ├── ProductDetailImage (일대다)
  │     └── Interest (일대다)
  ├── Brand (다대일)
  ├── Category (다대일)
  └── Collection (다대일)
  
Category
  └── SubCategories (일대다)
```

### Enum 타입

- **GenderType**: MALE, FEMALE, KIDS, UNISEX
- **ColorType**: BLACK, GREY, WHITE, IVORY 등
- **SizeType**: CLOTHING, SHOES, ACCESSORIES (해당 배열의 사이즈 포함)

## 아키텍처

제품 도메인은 계층화된 아키텍처를 따릅니다:

1. **컨트롤러 계층**: HTTP 요청 및 응답 처리
    - 쓰기 작업용 Command 컨트롤러
    - 읽기 작업용 Query 컨트롤러

2. **서비스 계층**: 비즈니스 로직 포함
    - 쓰기 작업용 Command 서비스
    - 읽기 작업용 Query 서비스
    - 엔티티별 작업을 위한 Entity 서비스

3. **리포지토리 계층**: 데이터 액세스 관리
    - 표준 JPA 리포지토리
    - 복잡한 쿼리를 위한 QueryDSL 리포지토리

4. **이벤트 처리**: 조회 추적을 위한 Kafka 기반 이벤트 처리

## 주요 구성 요소

### 컨트롤러

- **ProductCommandController**: 제품 생성, 수정, 삭제 관리
- **ProductColorCommandController**: 제품 색상 변형 처리
- **InterestCommandController**: 사용자의 제품 관심 관리
- **ProductQueryController**: 제품 쿼리 및 검색 처리
- **InterestQueryController**: 사용자의 관심 정보 조회

### 서비스

- **ProductCommandService/ProductQueryService**: 핵심 제품 관리
- **ProductColorCommandService/ProductColorQueryService**: 색상 변형 관리
- **ProductSizeCommandService/ProductSizeQueryService**: 사이즈 관리
- **InterestCommandService/InterestQueryService**: 관심 상품 관리
- **BrandCommandService/BrandQueryService**: 브랜드 관리
- **CategoryCommandService/CategoryQueryService**: 카테고리 관리
- **CollectionCommandService/CollectionQueryService**: 컬렉션 관리
- **FilterService**: 제품 검색을 위한 필터링 기능 제공

### 리포지토리

- **ProductRepository**: 기본 제품 CRUD 작업
- **ProductColorRepository**: 색상 변형 작업
- **ProductSizeRepository**: 사이즈 작업
- **ProductQueryDslRepository**: QueryDSL을 사용한 복잡한 제품 쿼리
- **InterestQueryDslRepository**: 복잡한 관심 상품 쿼리

### 이벤트 처리

- **ViewEventProducer**: 제품 조회 이벤트 생성
- **ViewEventConsumer**: 제품 조회 이벤트 소비 및 처리

## 예외 처리

도메인은 포괄적인 예외 처리 전략을 구현합니다:

- **ProductException**: 모든 제품 도메인 오류에 대한 기본 예외
- **ProductErrorCode**: 제품 도메인에 특화된 오류 코드
- 특정 오류 사례에 대한 전문화된 예외(BrandNotFoundException, CategoryNotFoundException 등)

## 배치 처리

도메인은 배치 처리 기능을 포함합니다:

- **CreateSizesJobConfig**: 제품 사이즈 일괄 생성을 위한 Spring Batch 구성

## 파일 관리

- **FileUtils**: 파일 시스템에서 제품 이미지를 관리하기 위한 유틸리티

## 보안

- Command 작업은 관리자 권한 필요
- 관심 상품 작업을 위한 사용자 인증

## 사용 예시

### 제품 생성

1. 관리자 사용자로 인증
2. ProductCommandController를 사용하여 제품 생성
3. ProductColorCommandController를 사용하여 색상 추가
4. 각 색상에 사이즈 추가

### 제품 검색

1. 필터 매개변수로 ProductQueryController 사용
2. 정렬 옵션 적용
3. 페이지네이션된 결과 조회

### 사용자 관심 상품 관리

1. 사용자로 인증
2. InterestCommandController로 관심 상품 토글
3. InterestQueryController로 관심 상품 조회

## 구현된 모범 사례

- **명령-쿼리 책임 분리(CQRS)**: 별도의 명령 및 쿼리 서비스
- **도메인 주도 설계(DDD)**: 비즈니스 개념을 반영하는 도메인 엔티티 및 집계
- **계층화된 아키텍처**: 계층을 통한 관심사 분리
- **포괄적인 로깅**: 모든 계층에서의 상세 로깅
- **트랜잭션 관리**: 적절한 트랜잭션 경계
- **예외 처리**: 오류 처리에 대한 구조적 접근
- **이벤트 기반 아키텍처**: 조회 추적을 위한 Kafka 이벤트

## 성능 고려사항

- 최적화된 복잡한 쿼리를 위한 QueryDSL
- 대규모 결과 집합을 위한 페이지네이션
- Nginx를 사용한 캐싱(업데이트 시 캐시 제거)
- 대량 작업을 위한 배치 처리