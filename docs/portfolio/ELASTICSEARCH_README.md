# ElasticSearch를 활용한 상품 검색 시스템 최적화

## 1. 발생 문제

### 복잡한 QueryDSL 조인 및 필터링
- 상품 목록 검색 시 다수의 테이블(`Product`, `ProductColor`, `ProductSize`, `Interest` 등)을 복잡한 QueryDSL로 조인해야 했음
- 코드 예시: 아래와 같이 복잡한 검색 로직이 반복됨
```java
JPQLQuery<Tuple> query = queryFactory.select(
        product.id,
        product.name,
        product.englishName,
        product.releasePrice,
        productColor.id,
        productColor.colorName,
        productColor.thumbnailImage.imageUrl,
        productSize.purchasePrice.min(),
        interest.count(),
        product.brand.name
)
.from(product)
.leftJoin(product.colors, productColor)
.leftJoin(product.brand, brand)
.leftJoin(productColor.thumbnailImage, QProductImage.productImage)
.leftJoin(productColor.sizes, productSize)
.leftJoin(productColor.interests, interest)
.where(
        buildKeywordPredicate(keyword, product, productColor, productSize),
        buildCategoryPredicate(categoryIds, product),
        // 여러 복잡한 필터 조건들...
)
.groupBy(product.id, productColor.id, QProductImage.productImage.imageUrl)
.distinct();
```

### 고급 검색 기능 구현 한계
- RDB와 QueryDSL로는 다음 기능 구현이 어려움:
    - **오타 허용 검색** (예: "블렉" → "블랙")
    - **동의어 검색** (예: "검은색", "블랙" → 동일 결과)
    - **다중 필드 매칭** (상품명, 브랜드, 색상 등 여러 필드 동시 검색)
    - **자동완성**, **키워드 추천** 등 사용자 편의 기능

### DB 트래픽 집중으로 인한 성능 이슈
- 검색/필터링 요청 증가 시 RDB에 부하 집중
- 결제, 회원정보 관리 등 핵심 비즈니스 로직용 DB 리소스 부족
- 쿼리 복잡도 증가로 응답 시간 지연 및 전체 서비스 품질 저하

## 2. 해결 방안

### ElasticSearch 도입 및 검색 로직 분리

1. **ES 전용 인덱스 모델 설계**
```java
@Document(indexName = "product-colors")
public class ProductColorIndex {
    @Id
    private Long colorId;               // 색상 ID (문서 식별자)
    private Long productId;             // 상품 ID
    
    @Field(type = FieldType.Text)
    private String productName;         // 상품명 (한글)
    
    @Field(type = FieldType.Text)
    private String productEnglishName;  // 상품명 (영문)
    
    @Field(type = FieldType.Text)
    private String brandName;
    
    // 필터링/검색용 필드들...
    private List<String> sizes;         // 사이즈 배열
}
```

2. **효율적인 검색 및 필터링 구현**
```java
// 키워드 검색에 멀티매치 + 오타 허용(Fuzzy) 적용
MultiMatchQuery.Builder multiMatchBuilder = new MultiMatchQuery.Builder()
    .fields("productName", "productEnglishName", "brandName", 
            "categoryName", "collectionName", "colorName")
    .query(keyword)
    .fuzziness("AUTO")       // 오타 자동 보정
    .maxExpansions(50)       // 오타 교정 시 대체 단어 최대치
    .prefixLength(1);        // 오타 허용하기 전 최소 일치 글자 수
```

### DB와 ES 동기화 전략

1. **초기 데이터 일괄 인덱싱**
```java
@Component
@RequiredArgsConstructor
@Order(2)
public class ProductColorIndexInitializer implements CommandLineRunner {
    private final ProductColorIndexingService indexingService;
    private final ElasticsearchOperations esOperations;
    
    @Override
    public void run(String... args) {
        // 1) 인덱스 초기화
        IndexOperations idxOps = esOperations.indexOps(ProductColorIndex.class);
        if (idxOps.exists()) {
            idxOps.delete();
        }
        
        // 2) 인덱스 설정 (분석기, 토크나이저, 필터)
        Document settings = Document.parse("""
            {
                "analysis": {
                  "tokenizer": {
                    "nori_tokenizer_custom": {
                      "type": "nori_tokenizer",
                      "decompound_mode": "discard",
                      "user_dictionary": "analysis/userdict_ko.txt"
                    }
                  },
                  // 동의어, N-Gram 설정...
                }
            }
            """);
            
        // 3) 매핑 설정
        Document mapping = Document.parse("""
            {
                "properties": {
                  "productName": {
                    "type": "text",
                    "analyzer": "my_nori_base_analyzer",
                    "search_analyzer": "my_nori_synonym_analyzer"
                  },
                  // 기타 필드 매핑...
                }
            }
            """);
            
        // 4-5) 인덱스 생성 및 매핑 적용
        idxOps.create(settings);
        idxOps.putMapping(mapping);
        
        // 6) 데이터 인덱싱
        indexingService.indexAllColors();
    }
}
```

2. **단건 업데이트로 동기화 유지**
```java
@Transactional
public void indexColorById(Long colorId) {
    // 1) DB에서 데이터 조회
    ProductColorIndexDto dto = queryRepository.findOneForIndexingDto(colorId);
    List<ProductColorSizeRow> sizeRows = queryRepository.findSizesByColorId(colorId);
    
    // 2) ProductColorIndex 변환
    ProductColorIndex indexObj = mapToIndex(dto, sizeRows);
    
    // 3) ES에 저장 (upsert)
    productColorEsRepository.save(indexObj);
}

// 색상 삭제 시 인덱스에서도 삭제
@Transactional
public void deleteColorFromIndex(Long colorId) {
    productColorEsRepository.deleteById(colorId);
}
```

### 아키텍처 역할 분담

1. **ES**: 검색, 필터링, 자동완성 담당
2. **RDB**: 핵심 트랜잭션 (결제, 회원정보, 재고관리)과 상세 정보 조회 담당

3. **API 계층 분리**
```java
@RestController
@RequestMapping("/es/products")
public class ProductColorSearchController {
    @GetMapping
    public ResponseEntity<commonDto.PageDto<ProductSearchResponseDto>> esSearchProducts(
            @ModelAttribute ProductSearchDto searchDto,
            @ModelAttribute SortOption sortOption,
            Pageable pageable
    ) {
        // ES 기반 검색 처리
        Page<ProductSearchResponseDto> resultPage = productColorSearchService.searchToDto(...);
        return ResponseEntity.ok(toPageDto(resultPage));
    }
    
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocompleteProducts(
            @RequestParam(name = "q", required = false) String query
    ) {
        // 자동완성 검색
        List<String> suggestions = productColorSearchService.autocomplete(query, 10);
        return ResponseEntity.ok(suggestions);
    }
}
```

4. **Nginx Reverse Proxy 캐싱**
    - `/api/es/products` 응답을 캐싱해 트래픽 집중 시에도 ES 부하 감소
    - 데이터 변경 시 Purge 요청으로 캐시 무효화

## 3. 한글 특화 검색 최적화 도전과 해결

### Nori 토크나이저와 동의어 처리 충돌

#### 문제
- `decompound_mode: "mixed"` 설정 시 복합어(예: "검은색")가 여러 경로로 분해되면서 동의어 필터와 충돌
- 다중 단어 동의어가 있는 경우 "failed to build synonyms" 에러 발생

#### 해결
```java
// decompound_mode를 "discard"로 변경해 단일 경로 분석
Document settings = Document.parse("""
    {
        "analysis": {
          "tokenizer": {
            "nori_tokenizer_custom": {
              "type": "nori_tokenizer",
              "decompound_mode": "discard",
              "user_dictionary": "analysis/userdict_ko.txt"
            }
          },
          // ...
        }
    }
    """);
```

### 파일 인코딩 문제 (BOM/CRLF)

#### 문제
- Windows에서 작성한 `synonyms.txt`에 BOM 마크와 CRLF가 포함되어 ES에서 동의어 파일 해석 실패

#### 해결
- 도커 빌드 시 `dos2unix`, `iconv` 등으로 변환 처리
- 로케일 설정 추가: `ENV LANG=ko_KR.UTF-8`

### 한글 오타 교정 최적화

#### 문제
- 알파벳 기반 Fuzziness가 한글 음절 구조("블랙"→"블렉")에 효과적으로 작동하지 않음
- 한글은 초성·중성·종성 결합으로 구성되어 단순 문자 치환으로 처리하기 어려움

#### 해결
- **Edge N-Gram 필터 적용**으로 부분 매칭 강화
```java
// Edge N-Gram 필터 설정
"my_nori_edge_ngram": {
  "type": "edge_ngram",
  "min_gram": 2,
  "max_gram": 4
}

// 분석기에 적용
"my_nori_base_analyzer": {
  "type": "custom",
  "tokenizer": "nori_tokenizer_custom",
  "filter": [
    "lowercase",
    "my_nori_edge_ngram"
  ]
}
```

- Fuzziness 설정 조정 (영문 필드만 적용하거나 `fuzziness=1`로 제한)

## 4. 검색 서비스 구현

### 멀티매치 검색과 필터링 로직

```java
public Page<ProductColorIndex> search(
        String keyword,
        List<Long> categoryIds,
        List<String> genders,
        // 기타 필터 매개변수들...
) {
    // 1) BoolQuery 구성
    BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
    
    // 2) 키워드 검색 (멀티매치 + 오타 허용)
    if (keyword != null && !keyword.isBlank()) {
        boolBuilder.must(createMultiMatchQuery(keyword));
    }
    
    // 3-9) 각종 필터 조건 추가
    if (categoryIds != null && !categoryIds.isEmpty()) {
        boolBuilder.must(createTermsFilter("categoryId", categoryIds));
    }
    
    // 기타 필터 조건들...
    
    // 10) 최종 쿼리 실행
    NativeQuery nativeQuery = buildNativeQuery(boolBuilder, sortOption, pageable);
    SearchHits<ProductColorIndex> searchHits = esOperations.search(nativeQuery, ProductColorIndex.class);
    
    // 11) 결과 변환
    return convertToPage(searchHits, pageable);
}
```

### 자동완성(Autocomplete) 기능 구현

```java
public List<String> autocomplete(String query, int limit) {
    // 1) 멀티매치 쿼리 구성
    MultiMatchQuery.Builder multiMatchBuilder = new MultiMatchQuery.Builder()
            .fields("productName", "productEnglishName", "brandName", 
                    "collectionName", "colorName")
            .query(query)
            .fuzziness("AUTO")
            .maxExpansions(50)
            .prefixLength(1);
    
    // 2-4) 쿼리 실행
    NativeQuery nativeQuery = buildAutoCompleteQuery(multiMatchBuilder, limit);
    SearchHits<ProductColorIndex> searchHits = esOperations.search(nativeQuery, ProductColorIndex.class);
    
    // 5-7) 결과 변환 및 반환
    return formatAutocompleteResults(searchHits);
}
```

## 5. 결과

### 검색 품질 및 사용자 만족도 향상
- 오타 허용, 동의어 검색, 다중 필드 매칭으로 검색 정확도 개선
- 검색 속도 최적화로 페이지 응답성 향상

### DB 부하 분산 및 서비스 안정성 확보
- 검색/필터링 부하를 ES로 분산해 RDB 리소스 확보
- QueryDSL 복잡도 감소로 유지보수성 향상

```java
// 이전: 복잡한 QueryDSL 조인 및 필터
JPQLQuery<Tuple> query = queryFactory.select(/* 복잡한 쿼리 */)
    .from(product)
    .leftJoin(/* 다수의 테이블 조인 */)
    .where(/* 다수의 필터 조건 */)
    .groupBy(/* 그룹화 조건 */);

// 이후: ES 검색 활용
Page<ProductSearchResponseDto> resultPage = productColorSearchService.searchToDto(
    searchDto.getKeyword(),
    searchDto.getCategoryIds(),
    /* 간결한 파라미터 */
);
```

### 유지보수 간소화
- 복잡한 QueryDSL 대신 ES 매핑과 분석기 설정으로 검색 스키마 관리
- 동의어, 사용자 사전 업데이트 등 유연한 검색 로직 변경 가능

### 확장성 확보
- Edge N-Gram 기반 자동완성 기능
- 새로운 검색 요구사항에 유연하게 대응 가능
- 기존 RDB는 핵심 트랜잭션에 집중해 전체 시스템 안정성 향상

## 6. 아키텍처 다이어그램

```
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│               │     │               │     │               │
│    Client     │────▶│  Nginx Cache  │────▶│  Spring Boot  │
│               │     │               │     │  Application  │
└───────────────┘     └───────────────┘     └───────┬───────┘
                                                    │
                      ┌───────────────┐             │
                      │               │             │
                      │  MySQL (RDB)  │◀────────────┤
                      │  (트랜잭션, 상세)│             │
                      └───────────────┘             │
                                                    │
                      ┌───────────────┐             │
                      │               │             │
                      │ ElasticSearch │◀────────────┘
                      │  (검색, 필터링) │
                      └───────────────┘
```

## 7. 주요 극복 사항 요약

1. **데이터 분산 저장에 따른 동기화 전략 수립**
    - CommandLineRunner로 애플리케이션 구동 시 초기 인덱싱
    - 상품/색상 변경 시 실시간 ES 업데이트

2. **한글 검색 최적화 문제**
    - Nori 토크나이저의 decompound_mode와 동의어 충돌 해결
    - 파일 인코딩(BOM/CRLF) 문제 도커 빌드 시 처리
    - 한글 오타 교정을 위한 Edge N-Gram 최적화

3. **검색 성능과 정확도 균형**
    - 멀티매치(다중 필드 검색)와 오타 허용 기능 최적 조합
    - 동의어 사전 및 분석기 튜닝으로 검색 품질 향상

4. **복잡한 필터링을 ES 쿼리로 변환**
    - 카테고리, 브랜드, 컬렉션, 성별, 가격 범위 등 필터를 BoolQuery로 구현
    - 가격, 관심도, 출시일 등 다양한 정렬 옵션 지원