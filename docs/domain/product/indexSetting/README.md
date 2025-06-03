# DB 인덱스 최적화 구현 가이드

## 📋 개요

기존 상품 검색 API의 성능 최적화를 위해 **인덱스 기반 검색 시스템**을 구현했습니다.
이름 기반 검색을 통한 2단계 최적화로 쿼리 성능을 향상시켰습니다.

## 🗂️ 인덱스 설계

### 적용된 인덱스 목록

```sql
-- 엔티티별 이름 검색 인덱스
CREATE INDEX idx_brand_name ON brand(name);
CREATE INDEX idx_category_name ON category(name);
CREATE INDEX idx_collection_name ON collection(name);
CREATE INDEX idx_product_color_name ON product_color(color_name);

-- FK 조인 최적화 인덱스
CREATE INDEX idx_product_brand ON product(brand_id);
CREATE INDEX idx_product_category ON product(category_id);
CREATE INDEX idx_product_collection ON product(collection_id);
CREATE INDEX idx_product_color_product ON product_color(product_id);

-- 가격 검색 최적화 인덱스
CREATE INDEX idx_product_size_purchase_price ON product_size(purchase_price);
CREATE INDEX idx_product_size_color ON product_size(product_color_id);
```

### 엔티티별 인덱스 적용 전략

#### Product
```java
@Table(name = "product", indexes = {
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_english_name", columnList = "english_name"),
    @Index(name = "idx_product_brand", columnList = "brand_id"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_collection", columnList = "collection_id"),
    @Index(name = "idx_product_gender", columnList = "gender"),
    @Index(name = "idx_product_date", columnList = "release_date")
})
```

#### ProductColor
```java
@Table(name = "product_color", indexes = {
    @Index(name = "idx_product_color_name", columnList = "color_name"),
    @Index(name = "idx_product_color_product", columnList = "product_id")
})
```

#### ProductSize
```java
@Table(name = "product_size", indexes = {
    @Index(name = "idx_product_size_color", columnList = "product_color_id"),
    @Index(name = "idx_product_size_purchase_price", columnList = "purchase_price")
})
```

## 🏗️ 아키텍처

### 구현된 컴포넌트

```
IndexedProductRepository     - 인덱스 최적화 쿼리 담당
ProductSearchByNameDto       - 이름 기반 검색 DTO
IndexedProductQueryService   - 비즈니스 로직 처리
IndexedProductQueryController - 새로운 엔드포인트 제공
```

### 2단계 검색 프로세스

1. **1단계: 이름 → ID 변환**
    - 브랜드명, 카테고리명, 컬렉션명을 각각 인덱스 쿼리로 ID 조회
    - 각 변환 작업이 독립적으로 실행되어 병렬 처리 가능

2. **2단계: ID 기반 최적화 검색**
    - FK 인덱스를 활용한 효율적인 조인
    - 추가 필터 조건들도 각각의 인덱스 활용

## 🔧 구현 세부사항

### IndexedProductRepository 핵심 메서드

```java
// 1단계: 이름 기반 ID 조회
public List<Long> findBrandIdsByNames(List<String> brandNames) {
    // idx_brand_name 인덱스 활용
}

public List<Long> findCategoryIdsByNames(List<String> categoryNames) {
    // idx_category_name 인덱스 활용
}

// 2단계: 최적화된 상품 검색
public Page<ProductSearchResponseDto> searchProductsByNames(...) {
    // 1단계에서 얻은 ID들로 최적화된 쿼리 실행
}
```

### 쿼리 조건별 인덱스 활용

```java
// 브랜드 조건: idx_product_brand 사용
buildBrandIdsPredicate(brandIds, product)

// 카테고리 조건: idx_product_category 사용  
buildCategoryIdsPredicate(categoryIds, product)

// 색상 조건: idx_product_color_name 사용
buildColorPredicate(colors, productColor)

// 가격 조건: idx_product_size_purchase_price 사용
buildPricePredicate(minPrice, maxPrice, productSize)
```

### 정렬 최적화

```java
// 가격 정렬: idx_product_size_purchase_price 활용
case "price":
    query.orderBy(productSize.purchasePrice.min().asc/desc())

// 출시일 정렬: idx_product_date 활용  
case "releasedate":
    query.orderBy(product.releaseDate.asc/desc())
```

## 🚀 API 사용법

### 새로운 인덱스 최적화 엔드포인트

```http
GET /products/indexed/search
```

### 요청 파라미터 (ProductSearchByNameDto)

```json
{
  "keyword": "Jordan",
  "brandNames": ["Nike", "Adidas"],
  "categoryNames": ["신발", "의류"],
  "collectionNames": ["Jordan", "Boost"],
  "colors": ["Black", "White"],
  "sizes": ["250", "M"],
  "minPrice": 100000,
  "maxPrice": 500000,
  "sortOption": {
    "field": "price",
    "order": "asc"
  }
}
```

### 기존 API와의 비교

```http
# 기존 ID 기반
GET /products/query?brandIds=1,2&categoryIds=3,4

# 새로운 이름 기반 (인덱스 최적화)
GET /products/indexed/search?brandNames=Nike,Adidas&categoryNames=신발,의류
```

## 📊 성능 최적화 포인트

### 인덱스 활용 전략

1. **단일 조건 검색**: 각 테이블의 name 컬럼 인덱스 직접 활용
2. **조인 최적화**: FK 컬럼 인덱스로 조인 성능 향상
3. **범위 검색**: 가격 범위 검색 시 purchase_price 인덱스 활용
4. **정렬 최적화**: ORDER BY 절에서 인덱스 활용

### 선택적 인덱스 적용

- **포함된 인덱스**: 자주 검색되는 핵심 필드만 선별
- **제외된 인덱스**: size, sale_price 등 효과 미미한 필드 제외
- **복합 인덱스 지양**: 단일 인덱스 조합으로 충분한 성능 확보

## 🔍 모니터링 및 검증

### 쿼리 실행 계획 확인

```sql
-- 브랜드명 검색 실행 계획
EXPLAIN SELECT id FROM brand WHERE name IN ('Nike', 'Adidas');

-- 상품 검색 실행 계획  
EXPLAIN SELECT * FROM product p 
LEFT JOIN product_color pc ON p.id = pc.product_id 
WHERE p.brand_id IN (1, 2);
```

### 성능 측정 방법

1. **응답 시간**: 기존 API vs 새로운 API 응답 시간 비교
2. **쿼리 실행 시간**: 슬로우 쿼리 로그 분석
3. **인덱스 사용률**: DB 통계를 통한 인덱스 히트율 확인

## 🛠️ 추가 개선 방안

### 복합 인덱스 고려사항

자주 함께 사용되는 조건들에 대한 복합 인덱스 검토:
- `(brand_id, category_id)`
- `(product_color_id, purchase_price)`

### 파티셔닝 전략

데이터 증가 시 테이블 파티셔닝 고려:
- 브랜드별 파티셔닝
- 출시일 기준 파티셔닝

## 📁 파일 구조

```
src/main/java/com/fream/back/domain/product/
├── repository/
│   └── IndexedProductRepository.java
├── dto/
│   └── ProductSearchByNameDto.java  
├── service/product/
│   └── IndexedProductQueryService.java
└── controller/query/
    └── IndexedProductQueryController.java
```