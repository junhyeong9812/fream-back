package com.fream.back.domain.product.elasticsearch.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.order.repository.OrderBidRepository;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.elasticsearch.index.ProductColorIndex;
import com.fream.back.domain.product.elasticsearch.repository.ProductColorEsRepository;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.style.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductColorSearchService {

    private final ElasticsearchOperations esOperations;
    private final ProductColorEsRepository productColorEsRepository;
    private final StyleRepository styleRepository;       // <- custom
    private final OrderBidRepository orderBidRepository; // <- custom
    private final ObjectMapper objectMapper;
    /**
     * 고급 검색 (멀티매치 + 오타 허용 + 동의어 등)
     */
    /**
     * 고급 검색 (멀티매치 + 오타 허용 + 동의어 등)
     */
    public Page<ProductSearchResponseDto> searchToDto(
            String keyword,
            List<Long> categoryIds,
            List<String> genders,
            List<Long> brandIds,
            List<Long> collectionIds,
            List<String> colorNames,
            List<String> sizes,
            Integer minPrice,
            Integer maxPrice,
            SortOption sortOption,
            Pageable pageable
    ) {
        log.info("=== searchToDto 호출 시작 ===");
        log.info("키워드: {}", keyword);
        log.info("카테고리IDs: {}", categoryIds);
        log.info("성별: {}", genders);
        log.info("브랜드IDs: {}", brandIds);
        log.info("컬렉션IDs: {}", collectionIds);
        log.info("색상: {}", colorNames);
        log.info("사이즈: {}", sizes);
        log.info("가격범위: {} ~ {}", minPrice, maxPrice);
        log.info("정렬옵션: {}", sortOption != null ? sortOption.getField() + " " + sortOption.getOrder() : "null");
        log.info("페이지: {}, 사이즈: {}",
                pageable != null ? pageable.getPageNumber() : "null",
                pageable != null ? pageable.getPageSize() : "null");

        // ES 검색
        Page<ProductColorIndex> pageResult = search(
                keyword, categoryIds, genders, brandIds,
                collectionIds, colorNames, sizes,
                minPrice, maxPrice, sortOption, pageable
        );

        log.info("ES 검색 결과 - 총 항목 수: {}, 페이지 크기: {}",
                pageResult.getTotalElements(), pageResult.getSize());

        // ProductColorIndex → ProductSearchResponseDto 변환
        List<ProductSearchResponseDto> dtoList = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        log.info("DTO 변환 완료 - DTO 개수: {}", dtoList.size());

        // colorIds 추출
        List<Long> colorIds = dtoList.stream()
                .map(ProductSearchResponseDto::getColorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        log.info("추출된 컬러ID 개수: {}", colorIds.size());

        if (!colorIds.isEmpty()) {
            // styleCount, tradeCount Map
            Map<Long, Long> styleCountMap = styleRepository.styleCountByColorIds(colorIds);
            Map<Long, Long> tradeCountMap = orderBidRepository.tradeCountByColorIds(colorIds);

            log.info("스타일 카운트 맵 크기: {}", styleCountMap.size());
            log.info("거래 카운트 맵 크기: {}", tradeCountMap.size());

            // 주입
            dtoList.forEach(dto -> {
                Long cId = dto.getColorId();
                dto.setStyleCount(styleCountMap.getOrDefault(cId, 0L));
                dto.setTradeCount(tradeCountMap.getOrDefault(cId, 0L));
            });
        }

        log.info("=== searchToDto 호출 완료 ===");

        // 결과 반환
        return new PageImpl<>(
                dtoList,
                pageResult.getPageable(),
                pageResult.getTotalElements()
        );
    }

    public Page<ProductColorIndex> search(
            String keyword,
            List<Long> categoryIds,
            List<String> genders,
            List<Long> brandIds,
            List<Long> collectionIds,
            List<String> colorNames,
            List<String> sizes,
            Integer minPrice,
            Integer maxPrice,
            SortOption sortOption,
            Pageable pageable
    ) {
        log.info("=== search 메서드 호출 시작 ===");

        // 페이지 정보 로깅
        log.info("페이지 정보: {}, 사이즈: {}",
                pageable != null ? pageable.getPageNumber() : "null",
                pageable != null ? pageable.getPageSize() : "null");

        // 필터 정보 로깅
        log.info("필터 - 키워드: {}, 카테고리: {}, 성별: {}, 브랜드: {}, 컬렉션: {}",
                keyword, categoryIds, genders, brandIds, collectionIds);
        log.info("필터 - 색상: {}, 사이즈: {}, 가격: {} ~ {}",
                colorNames, sizes, minPrice, maxPrice);

        // 1) 최상위 BoolQuery
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 2) 키워드가 있으면 향상된 검색 로직 적용
        if (keyword != null && !keyword.isBlank()) {
            log.info("키워드 검색 시작: '{}'", keyword);

            // 2-1) 정확한 구문 일치 쿼리 (높은 부스트)
            MultiMatchQuery.Builder exactMatchBuilder = new MultiMatchQuery.Builder()
                    .fields("productName^3", "productEnglishName^3", "brandName^2",
                            "categoryName", "collectionName", "colorName")
                    .query(keyword)
                    .type(TextQueryType.Phrase)  // 정확한 구문 매칭
                    .boost(2.0f);               // 높은 부스트

            // 2-2) 퍼지 매칭 쿼리 (낮은 부스트)
            MultiMatchQuery.Builder fuzzyMatchBuilder = new MultiMatchQuery.Builder()
                    .fields("productName", "productEnglishName", "brandName",
                            "categoryName", "collectionName", "colorName")
                    .query(keyword)
                    .fuzziness("AUTO")       // 오타 자동 보정
                    .maxExpansions(50)       // 오타 교정 시 대체 단어 최대치
                    .prefixLength(1)         // 오타 허용하기 전 최소 몇 글자 일치해야 하는지
                    .boost(1.0f);            // 낮은 부스트

            // 2-3) 키워드를 공백으로 분리하여 term 쿼리도 추가 (각 토큰에 대한 정확한 일치)
            BoolQuery.Builder keywordTermsBuilder = new BoolQuery.Builder();

            // 키워드를 공백으로 분리
            String[] tokens = keyword.split("\\s+");
            if (tokens.length > 1) {
                log.info("키워드를 {} 개의 토큰으로 분리", tokens.length);

                for (String token : tokens) {
                    if (StringUtils.hasText(token)) {
                        // 각 토큰에 대해 여러 필드 검색 (should로 연결)
                        BoolQuery.Builder tokenBuilder = new BoolQuery.Builder();

                        tokenBuilder.should(new Query.Builder()
                                .match(m -> m.field("productName").query(token).boost(1.5f))
                                .build());

                        tokenBuilder.should(new Query.Builder()
                                .match(m -> m.field("productEnglishName").query(token).boost(1.5f))
                                .build());

                        tokenBuilder.should(new Query.Builder()
                                .match(m -> m.field("brandName").query(token).boost(1.2f))
                                .build());

                        tokenBuilder.should(new Query.Builder()
                                .match(m -> m.field("colorName").query(token))
                                .build());

                        // 각 토큰에 대한 쿼리를 전체 키워드 쿼리에 must로 추가
                        keywordTermsBuilder.must(new Query.Builder()
                                .bool(tokenBuilder.build())
                                .build());
                    }
                }
            }

            // 2-4) 세 가지 쿼리 접근법을 should로 결합 (어느 하나라도 매칭되면 결과에 포함)
            BoolQuery.Builder keywordCombinedBuilder = new BoolQuery.Builder();

            // 정확한 구문 매칭
            keywordCombinedBuilder.should(new Query.Builder()
                    .multiMatch(exactMatchBuilder.build())
                    .build());

            // 퍼지 매칭
            keywordCombinedBuilder.should(new Query.Builder()
                    .multiMatch(fuzzyMatchBuilder.build())
                    .build());

            // 토큰 매칭 (tokens.length > 1인 경우에만)
            if (tokens.length > 1) {
                keywordCombinedBuilder.should(new Query.Builder()
                        .bool(keywordTermsBuilder.build())
                        .build());
            }

            // 2-5) 최종 키워드 쿼리를 메인 쿼리의 must로 추가
            boolBuilder.must(new Query.Builder()
                    .bool(keywordCombinedBuilder.build())
                    .build());

            log.info("키워드 검색 쿼리 구성 완료");
        }

        // 3) categoryIds (OR 조건)
        if (categoryIds != null && !categoryIds.isEmpty()) {
            log.info("카테고리 필터 적용: {}", categoryIds);
            BoolQuery.Builder catBuilder = new BoolQuery.Builder();

            for (Long catId : categoryIds) {
                if (catId != null) {
                    catBuilder.should(new Query.Builder()
                            .term(t -> t.field("categoryId").value(catId))
                            .build());
                }
            }

            boolBuilder.must(new Query.Builder().bool(catBuilder.build()).build());
        }

        // 4) gender (OR 조건) - 수정된 부분: match과 term 쿼리 모두 시도
        if (genders != null && !genders.isEmpty()) {
            log.info("성별 필터 적용 시도: {}", genders);
            BoolQuery.Builder genderBuilder = new BoolQuery.Builder();

            for (String g : genders) {
                if (g != null && !g.isBlank()) {
                    // 대소문자와 공백 처리를 확실히
                    String normalizedGender = g.trim().toUpperCase();
                    log.info("정규화된 성별 값: {}", normalizedGender);

                    // 1. match 쿼리 사용
                    genderBuilder.should(new Query.Builder()
                            .match(m -> m.field("gender").query(normalizedGender))
                            .build());

                    // 2. term 쿼리도 사용
                    genderBuilder.should(new Query.Builder()
                            .term(t -> t.field("gender").value(normalizedGender))
                            .build());
                }
            }

            // 유효한 성별 필터가 있는 경우에만 must로 추가
            BoolQuery genderBool = genderBuilder.build();

            // 성별 쿼리 로깅
            try {
                String genderQueryJson = objectMapper.writeValueAsString(genderBool);
                log.info("성별 BoolQuery JSON: {}", genderQueryJson);
            } catch (Exception e) {
                log.error("성별 쿼리 JSON 변환 오류: {}", e.getMessage());
            }

            if (genderBool.should() != null && !genderBool.should().isEmpty()) {
                boolBuilder.must(new Query.Builder().bool(genderBool).build());
                log.info("성별 필터 적용 완료");
            } else {
                log.info("유효한 성별 필터가 없어 적용하지 않음");
            }
        }

        // 5) brandIds (OR 조건)
        if (brandIds != null && !brandIds.isEmpty()) {
            log.info("브랜드 필터 적용: {}", brandIds);
            BoolQuery.Builder brandBuilder = new BoolQuery.Builder();

            for (Long bId : brandIds) {
                if (bId != null) {
                    brandBuilder.should(new Query.Builder()
                            .term(t -> t.field("brandId").value(bId))
                            .build());
                }
            }

            boolBuilder.must(new Query.Builder().bool(brandBuilder.build()).build());
        }

        // 6) collectionIds (OR 조건)
        if (collectionIds != null && !collectionIds.isEmpty()) {
            log.info("컬렉션 필터 적용: {}", collectionIds);
            BoolQuery.Builder colBuilder = new BoolQuery.Builder();

            for (Long cId : collectionIds) {
                if (cId != null) {
                    colBuilder.should(new Query.Builder()
                            .term(t -> t.field("collectionId").value(cId))
                            .build());
                }
            }

            boolBuilder.must(new Query.Builder().bool(colBuilder.build()).build());
        }

        // 7) colorNames (OR 조건, 색상명 매칭 개선)
        if (colorNames != null && !colorNames.isEmpty()) {
            log.info("색상 필터 적용: {}", colorNames);
            BoolQuery.Builder colorBuilder = new BoolQuery.Builder();

            for (String cName : colorNames) {
                if (cName != null && !cName.isBlank()) {
                    // 공백 및 대소문자 처리
                    String normalizedColor = cName.trim();
                    log.info("정규화된 색상명: {}", normalizedColor);

                    // 정확한 일치와 퍼지 매칭 모두 지원
                    colorBuilder.should(new Query.Builder()
                            .match(m -> m.field("colorName").query(normalizedColor).boost(2.0f))
                            .build());

                    colorBuilder.should(new Query.Builder()
                            .match(m -> m.field("colorName")
                                    .query(normalizedColor)
                                    .fuzziness("AUTO")
                                    .boost(1.0f))
                            .build());
                }
            }

            boolBuilder.must(new Query.Builder().bool(colorBuilder.build()).build());
        }

        // 8) sizes (OR 조건, Keyword Array)
        if (sizes != null && !sizes.isEmpty()) {
            log.info("사이즈 필터 적용: {}", sizes);
            BoolQuery.Builder sizeBuilder = new BoolQuery.Builder();

            for (String sz : sizes) {
                if (sz != null && !sz.isBlank()) {
                    // 공백 제거 후 적용
                    String normalizedSize = sz.trim();
                    log.info("정규화된 사이즈: {}", normalizedSize);

                    sizeBuilder.should(new Query.Builder()
                            .term(t -> t.field("sizes").value(normalizedSize))
                            .build());
                }
            }

            boolBuilder.must(new Query.Builder().bool(sizeBuilder.build()).build());
        }

        // 9) minPrice / maxPrice (Range)
        if (minPrice != null && minPrice > 0) {
            log.info("최소 가격 필터 적용: {}", minPrice);
            boolBuilder.must(rangeQuery("minPrice", null, minPrice));
        }

        if (maxPrice != null && maxPrice > 0) {
            log.info("최대 가격 필터 적용: {}", maxPrice);
            boolBuilder.must(rangeQuery("maxPrice", maxPrice, null));
        }

        // 최종 BoolQuery 빌드 및 조건 확인
        BoolQuery builtBool = boolBuilder.build();
        boolean noConditions = (builtBool.must() == null || builtBool.must().isEmpty()) &&
                (builtBool.should() == null || builtBool.should().isEmpty()) &&
                (builtBool.filter() == null || builtBool.filter().isEmpty()) &&
                (builtBool.mustNot() == null || builtBool.mustNot().isEmpty());

        log.info("필터 조건 유무: {}", noConditions ? "조건 없음" : "조건 있음");

        // 최종 Query 구성
        Query finalQuery;
        if (noConditions) {
            // 아무 필터가 없을 때는 match_all
            log.info("필터 조건이 없으므로 match_all 쿼리 사용");
            finalQuery = new Query.Builder()
                    .matchAll(m -> m)
                    .build();
        } else {
            // 기존 BoolQuery 사용
            log.info("구성된 BoolQuery 사용");
            finalQuery = new Query.Builder()
                    .bool(builtBool)
                    .build();
        }

        // 쿼리 JSON 로깅
        try {
            String queryJson = objectMapper.writeValueAsString(finalQuery);
            log.info("최종 쿼리 JSON: {}", queryJson);
        } catch (Exception e) {
            log.error("쿼리 JSON 변환 오류: {}", e.getMessage());
        }

        // NativeQuery 생성 및 정렬 처리
        var nativeQueryBuilder = NativeQuery.builder()
                .withQuery(finalQuery);

        // 정렬 처리 (SortOption)
        if (sortOption != null && sortOption.getField() != null) {
            String rawField = sortOption.getField();

            // 필드명 정규화
            if (rawField.equalsIgnoreCase("releaseDate")) {
                rawField = "releaseDate";
            } else if (rawField.equalsIgnoreCase("interestCount")) {
                rawField = "interestCount";
            } else if (rawField.equalsIgnoreCase("price")) {
                rawField = "minPrice";
            } else {
                rawField = "productId";
            }

            // 정렬 방향 결정
            SortOrder finalOrder = (sortOption.getOrder() != null &&
                    sortOption.getOrder().equalsIgnoreCase("desc"))
                    ? SortOrder.Desc
                    : SortOrder.Asc;

            final String finalField = rawField;

            log.info("정렬 적용 - 필드: {}, 순서: {}", finalField, finalOrder);

            nativeQueryBuilder.withSort(s ->
                    s.field(f -> f.field(finalField).order(finalOrder))
            );
        } else {
            // sortOption이 없거나 field가 없으면 디폴트 정렬: productId asc
            log.info("기본 정렬 적용 - 필드: productId, 순서: ASC");
            nativeQueryBuilder.withSort(s ->
                    s.field(f -> f.field("productId").order(SortOrder.Asc))
            );
        }

        // 페이징 처리
        Pageable finalPageable = pageable != null ? pageable : PageRequest.of(0, 20);
        nativeQueryBuilder.withPageable(finalPageable);

        log.info("최종 페이징 - 페이지: {}, 크기: {}, 오프셋: {}",
                finalPageable.getPageNumber(),
                finalPageable.getPageSize(),
                finalPageable.getOffset());

        // 최종 NativeQuery 빌드
        NativeQuery nativeQuery = nativeQueryBuilder.build();
        log.info("최종 쿼리 생성 완료");

        // 검색 실행 - 인덱스 이름 명시적 지정
        log.info("엘라스틱서치 검색 실행 - 인덱스: product-colors");
        SearchHits<ProductColorIndex> searchHits = esOperations.search(
                nativeQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );

        // 검색 결과 로깅
        log.info("검색 결과 - 총 히트 수: {}, 반환된 히트 수: {}",
                searchHits.getTotalHits(),
                searchHits.getSearchHits().size());

        // SearchHits → 엔티티 목록으로 변환
        List<ProductColorIndex> content = searchHits.getSearchHits().stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());

        // 검색 결과의 ID만 로깅 (디버깅용)
        if (!content.isEmpty()) {
            List<Long> resultIds = content.stream()
                    .map(ProductColorIndex::getProductId)
                    .collect(Collectors.toList());
            log.info("결과 상품 ID (최대 10개): {}",
                    resultIds.size() <= 10 ? resultIds : resultIds.subList(0, 10));

            // 성별 값도 로깅
            content.forEach(item -> {
                log.info("결과 항목 - ID: {}, 성별: {}", item.getProductId(), item.getGender());
            });
        } else {
            log.info("검색 결과가 없습니다.");
        }

        // 총 히트 수
        long totalHits = searchHits.getTotalHits();

        // PageImpl 생성 및 반환
        log.info("=== search 메서드 호출 완료 ===");
        return new PageImpl<>(
                content,
                finalPageable,
                totalHits
        );
    }

    /**
     * RangeQuery 빌더: min / max 인자를 받아 RangeQuery 만들기
     * 안전한 처리와 로깅 강화
     */
    private Query rangeQuery(String field, Integer gte, Integer lte) {
        if (field == null || field.isBlank()) {
            log.warn("범위 쿼리 생성 시 필드명이 비어있음");
            // 기본 필드 "minPrice" 사용
            field = "minPrice";
        }

        final String finalField = field;
        log.debug("범위 쿼리 생성 - 필드: {}, gte: {}, lte: {}", finalField, gte, lte);

        return new Query.Builder()
                .range(r -> {
                    var b = r.field(finalField);
                    if (gte != null) {
                        b.gte(JsonData.of(gte));
                        log.debug("범위 쿼리 - {} >= {}", finalField, gte);
                    }
                    if (lte != null) {
                        b.lte(JsonData.of(lte));
                        log.debug("범위 쿼리 - {} <= {}", finalField, lte);
                    }
                    return b;
                })
                .build();
    }

    /**
     * 성별 키워드를 정규화하여 변환
     * 안전한 변환과 로깅 강화
     */
    private String convertGender(String genderKeyword) {
        if (genderKeyword == null || genderKeyword.isBlank()) {
            log.info("성별 키워드가 비어있어 기본값(UNISEX) 반환");
            return "UNISEX";
        }

        String normalizedKeyword = genderKeyword.trim().toUpperCase();
        String result;

        switch (normalizedKeyword) {
            case "남자":
            case "남성":
            case "MEN":
            case "MAN":
            case "MALE":
                result = "MALE";
                break;
            case "여자":
            case "여성":
            case "WOMEN":
            case "WOMAN":
            case "FEMALE":
                result = "FEMALE";
                break;
            case "어린이":
            case "아동":
            case "키즈":
            case "KIDS":
            case "KID":
            case "CHILD":
            case "CHILDREN":
                result = "KIDS";
                break;
            case "공용":
            case "유니섹스":
            case "UNISEX":
            case "BOTH":
            case "ALL":
                result = "UNISEX";
                break;
            default:
                log.info("알 수 없는 성별 키워드({}), 기본값(UNISEX) 반환", normalizedKeyword);
                result = "UNISEX";
        }

        log.debug("성별 변환: {} -> {}", genderKeyword, result);
        return result;
    }

    /**
     * ProductColorIndex를 ProductSearchResponseDto로 변환
     * null 체크와 로깅 강화
     */
    private ProductSearchResponseDto toDto(ProductColorIndex idx) {
        if (idx == null) {
            log.warn("null 인덱스가 DTO 변환에 전달됨");
            return null;
        }

        log.debug("인덱스 -> DTO 변환 - 상품ID: {}, 컬러ID: {}", idx.getProductId(), idx.getColorId());

        // 안전한 값 추출 (null 체크)
        Long productId = idx.getProductId();
        String productName = idx.getProductName() != null ? idx.getProductName() : "";
        String englishName = idx.getProductEnglishName() != null ? idx.getProductEnglishName() : "";
        int releasePrice = idx.getReleasePrice(); // int는 null이 될 수 없음
        String thumbnailUrl = idx.getThumbnailUrl() != null ? idx.getThumbnailUrl() : "";
        int minPrice = idx.getMinPrice(); // int는 null이 될 수 없음
        String colorName = idx.getColorName() != null ? idx.getColorName() : "";
        Long colorId = idx.getColorId();
        long interestCount = idx.getInterestCount(); // long이라면 null이 될 수 없음
        String brandName = idx.getBrandName() != null ? idx.getBrandName() : "";

        // DTO 생성 및 반환
        return ProductSearchResponseDto.builder()
                .id(productId)                  // productId
                .name(productName)              // productName
                .englishName(englishName)
                .releasePrice(releasePrice)
                .thumbnailImageUrl(thumbnailUrl)
                .price(minPrice)                // 최저 구매가
                .colorName(colorName)
                .colorId(colorId)
                .interestCount(interestCount)
                .brandName(brandName)           // 브랜드명
                .build();
    }

    /**
     * 자동완성 기능
     * 로깅 강화 및 안전한 처리
     */
    public List<String> autocomplete(String query, int limit) {
        log.info("자동완성 요청 - 쿼리: '{}', 제한: {}", query, limit);

        if (query == null || query.isBlank()) {
            log.info("자동완성 쿼리가 비어있어 빈 결과 반환");
            return List.of();
        }

        // 쿼리 정규화 (앞뒤 공백 제거)
        String normalizedQuery = query.trim();

        try {
            // 1) 향상된 MultiMatchQuery 구성
            MultiMatchQuery.Builder multiMatchBuilder = new MultiMatchQuery.Builder()
                    .fields("productName^2", "productEnglishName^2", "brandName^1.5",
                            "collectionName", "colorName")
                    .query(normalizedQuery)
                    .type(TextQueryType.BestFields)    // 가장 잘 매칭되는 필드 우선
                    .fuzziness("AUTO")                // 오타 자동 보정
                    .prefixLength(1)                  // 첫 글자는 정확히 일치해야 함
                    .maxExpansions(50);               // 오타 교정 시 대체 단어 최대치

            // 2) BoolQuery 구성
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.must(new Query.Builder()
                    .multiMatch(multiMatchBuilder.build())
                    .build());

            // 2-1) 접두사 검색 추가 (자동완성에 적합)
            // 제품명과 브랜드명에 대해 접두사 검색
            BoolQuery.Builder prefixBuilder = new BoolQuery.Builder();

            prefixBuilder.should(new Query.Builder()
                    .prefix(p -> p.field("productName").value(normalizedQuery))
                    .build());

            prefixBuilder.should(new Query.Builder()
                    .prefix(p -> p.field("brandName").value(normalizedQuery))
                    .build());

            prefixBuilder.should(new Query.Builder()
                    .prefix(p -> p.field("colorName").value(normalizedQuery))
                    .build());

            // 접두사 검색을 should로 추가 (더 나은 자동완성 결과)
            boolBuilder.should(new Query.Builder()
                    .bool(prefixBuilder.build())
                    .build());

            // 3) 최종 Query 구성
            Query finalQuery = new Query.Builder()
                    .bool(boolBuilder.build())
                    .build();

            // 4) NativeQuery 빌드
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(finalQuery)
                    .withSort(s -> s.field(f -> f.field("interestCount")  // 인기도순 정렬
                            .order(SortOrder.Desc))
                    )
                    .withPageable(PageRequest.of(0, limit))  // 첫 페이지, size=limit
                    .build();

            log.info("자동완성 쿼리 구성 완료");

            // 5) 검색 실행
            SearchHits<ProductColorIndex> searchHits = esOperations.search(
                    nativeQuery,
                    ProductColorIndex.class,
                    IndexCoordinates.of("product-colors")
            );
            log.info("자동완성 검색 결과 - 총 히트: {}", searchHits.getTotalHits());

            // 6) 결과 처리
            List<ProductColorIndex> results = searchHits.getSearchHits().stream()
                    .map(hit -> hit.getContent())
                    .collect(Collectors.toList());

            log.info("자동완성 인덱스 결과 수: {}", results.size());

            // 7) 문자열로 매핑 및 중복 제거
            List<String> suggestions = results.stream()
                    .map(this::makeAutocompleteString)
                    .filter(StringUtils::hasText)  // 비어있는 값 제외
                    .distinct()                   // 중복 제거
                    .collect(Collectors.toList());

            log.info("자동완성 최종 제안 수: {}", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("자동완성 처리 중 오류 발생: {}", e.getMessage(), e);
            return List.of();  // 오류 시 빈 목록 반환
        }
    }

    /**
     * 자동완성용으로 보여줄 문자열 생성 로직
     * null 체크 강화 및 다양한 포맷 제공
     */
    private String makeAutocompleteString(ProductColorIndex idx) {
        if (idx == null) {
            log.warn("null 인덱스가 자동완성 문자열 생성에 전달됨");
            return "";
        }

        // null 체크와 빈 문자열 확인
        String brand = idx.getBrandName() != null ? idx.getBrandName().trim() : "";
        String productName = idx.getProductName() != null ? idx.getProductName().trim() : "";
        String colorName = idx.getColorName() != null ? idx.getColorName().trim() : "";

        // 모든 값이 비어있는 경우 체크
        if (brand.isEmpty() && productName.isEmpty() && colorName.isEmpty()) {
            log.warn("자동완성 문자열 생성에 필요한 모든 값이 비어있음 - productId: {}, colorId: {}",
                    idx.getProductId(), idx.getColorId());
            return "";
        }

        // 키워드 검색에서 더 잘 작동하도록 개선된 포맷
        // 브랜드와 제품명이 모두 있는 경우
        if (!brand.isEmpty() && !productName.isEmpty()) {
            if (!colorName.isEmpty()) {
                // 브랜드명 - 상품명 (컬러명) 포맷
                String suggestion = String.format("%s - %s (%s)", brand, productName, colorName);
                log.debug("자동완성 문자열 생성: '{}'", suggestion);
                return suggestion;
            } else {
                // 브랜드명 - 상품명 포맷
                String suggestion = String.format("%s - %s", brand, productName);
                log.debug("자동완성 문자열 생성: '{}'", suggestion);
                return suggestion;
            }
        }

        // 브랜드명만 있는 경우
        if (!brand.isEmpty()) {
            log.debug("자동완성 문자열 생성 (브랜드만): '{}'", brand);
            return brand;
        }

        // 상품명만 있는 경우
        if (!productName.isEmpty()) {
            log.debug("자동완성 문자열 생성 (상품명만): '{}'", productName);
            return productName;
        }

        // 색상명만 있는 경우
        log.debug("자동완성 문자열 생성 (색상명만): '{}'", colorName);
        return colorName;
    }

    /**
     * 엘라스틱서치에 저장된 모든 ProductColorIndex를 페이징하여 반환 (디버깅용)
     */
    public Page<ProductColorIndex> getAllIndexedColors(Pageable pageable) {
        log.info("전체 인덱스 조회 시작 - 페이지: {}, 크기: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // match_all 쿼리 생성
        Query matchAllQuery = new Query.Builder()
                .matchAll(m -> m)
                .build();

        // NativeQuery 구성
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(matchAllQuery)
                .withPageable(pageable)
                .withSort(s -> s.field(f -> f.field("colorId").order(SortOrder.Asc)))
                .build();

        // 검색 실행
        SearchHits<ProductColorIndex> searchHits = esOperations.search(
                nativeQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );
        log.info("전체 인덱스 조회 결과 - 총 히트: {}", searchHits.getTotalHits());

        // 결과를 Page 객체로 변환
        List<ProductColorIndex> content = searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        long totalHits = searchHits.getTotalHits();

        log.info("전체 인덱스 조회 완료");
        return new PageImpl<>(content, pageable, totalHits);
    }

    /**
     * 특정 카테고리에 속한 인덱스만 조회 (디버깅용)
     */
    public Page<ProductColorIndex> getIndexedColorsByCategory(Long categoryId, Pageable pageable) {
        log.info("카테고리별 인덱스 조회 시작 - 카테고리ID: {}, 페이지: {}, 크기: {}",
                categoryId, pageable.getPageNumber(), pageable.getPageSize());

        if (categoryId == null) {
            log.warn("카테고리 ID가 null이므로 빈 결과 반환");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 특정 카테고리 ID로 필터링하는 쿼리
        Query query = new Query.Builder()
                .term(t -> t.field("categoryId").value(categoryId))
                .build();

        // NativeQuery 구성
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable)
                .withSort(s -> s.field(f -> f.field("colorId").order(SortOrder.Asc)))
                .build();

        // 검색 실행
        SearchHits<ProductColorIndex> searchHits = esOperations.search(
                nativeQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );
        log.info("카테고리별 인덱스 조회 결과 - 총 히트: {}", searchHits.getTotalHits());

        // 결과를 Page 객체로 변환
        List<ProductColorIndex> content = searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        long totalHits = searchHits.getTotalHits();

        log.info("카테고리별 인덱스 조회 완료");
        return new PageImpl<>(content, pageable, totalHits);
    }

    /**
     * 엘라스틱서치 인덱스의 상태 확인 메서드 (디버깅용)
     * 총 문서 수, 카테고리별 문서 수 등 요약 정보 반환
     */
    public Map<String, Object> getIndexStats() {
        log.info("인덱스 통계 정보 조회 시작");
        Map<String, Object> stats = new HashMap<>();

        // 1. 전체 문서 수 계산
        Query matchAllQuery = new Query.Builder()
                .matchAll(m -> m)
                .build();

        NativeQuery countQuery = NativeQuery.builder()
                .withQuery(matchAllQuery)
                .withPageable(PageRequest.of(0, 1))  // 결과는 필요 없고 카운트만 필요
                .build();

        SearchHits<ProductColorIndex> countHits = esOperations.search(
                countQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );
        stats.put("totalDocuments", countHits.getTotalHits());
        log.info("총 문서 수: {}", countHits.getTotalHits());

        // 2. 성별별 문서 수 계산
        // MALE 문서 수
        Query maleQuery = new Query.Builder()
                .match(m -> m.field("gender").query("MALE"))
                .build();

        NativeQuery maleCountQuery = NativeQuery.builder()
                .withQuery(maleQuery)
                .withPageable(PageRequest.of(0, 1))
                .build();

        SearchHits<ProductColorIndex> maleHits = esOperations.search(
                maleCountQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );
        stats.put("maleCount", maleHits.getTotalHits());
        log.info("MALE 성별 문서 수: {}", maleHits.getTotalHits());

        // FEMALE 문서 수
        Query femaleQuery = new Query.Builder()
                .match(m -> m.field("gender").query("FEMALE"))
                .build();

        NativeQuery femaleCountQuery = NativeQuery.builder()
                .withQuery(femaleQuery)
                .withPageable(PageRequest.of(0, 1))
                .build();

        SearchHits<ProductColorIndex> femaleHits = esOperations.search(
                femaleCountQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );
        stats.put("femaleCount", femaleHits.getTotalHits());
        log.info("FEMALE 성별 문서 수: {}", femaleHits.getTotalHits());

        // 3. 카테고리별 문서 수
        // 스니커즈 카테고리
        Long sneakersId = 5L; // 실제 ID로 교체

        Query sneakersQuery = new Query.Builder()
                .term(t -> t.field("categoryId").value(sneakersId))
                .build();

        NativeQuery sneakersCountQuery = NativeQuery.builder()
                .withQuery(sneakersQuery)
                .withPageable(PageRequest.of(0, 1))
                .build();

        SearchHits<ProductColorIndex> sneakersHits = esOperations.search(
                sneakersCountQuery,
                ProductColorIndex.class,
                IndexCoordinates.of("product-colors")
        );
        stats.put("sneakersCount", sneakersHits.getTotalHits());
        log.info("스니커즈 카테고리 문서 수: {}", sneakersHits.getTotalHits());

        log.info("인덱스 통계 정보 조회 완료");
        return stats;
    }
}