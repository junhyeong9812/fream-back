package com.fream.back.domain.product.elasticsearch.controller;

import com.fream.back.domain.product.dto.ProductSearchDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.elasticsearch.index.ProductColorIndex;
import com.fream.back.domain.product.elasticsearch.service.ProductColorSearchService;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.product.service.product.ProductQueryService;
import com.fream.back.global.dto.commonDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/es/products")
public class ProductColorSearchController {

    private final ProductColorSearchService productColorSearchService;
    private final ProductQueryService productQueryService;

    @GetMapping
    public ResponseEntity<commonDto.PageDto<ProductSearchResponseDto>> esSearchProducts(
            @ModelAttribute ProductSearchDto searchDto,
            Pageable pageable
    ) {
        log.info("========== 검색 API 호출 시작 ==========");
        log.info("검색 요청 - 페이지: {}, 사이즈: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        log.info("검색 요청 - 키워드: {}", searchDto.getKeyword());
        log.info("검색 요청 - 카테고리: {}", searchDto.getCategoryIds());
        log.info("검색 요청 - 성별: {}", searchDto.getGenders());
        log.info("검색 요청 - 브랜드: {}", searchDto.getBrandIds());
        log.info("검색 요청 - 컬렉션: {}", searchDto.getCollectionIds());
        log.info("검색 요청 - 색상: {}", searchDto.getColors());
        log.info("검색 요청 - 사이즈: {}", searchDto.getSizes());
        log.info("검색 요청 - 가격범위: {} ~ {}", searchDto.getMinPrice(), searchDto.getMaxPrice());
        log.info("검색 요청 - 정렬옵션: {}", searchDto.getSortOption());

        // 유효성 검증 추가
        if (searchDto.getSortOption() == null) {
            log.info("정렬옵션이 null이므로 기본값(interestCount, desc) 설정");
            SortOption defaultSort = new SortOption();
            defaultSort.setField("interestCount");
            defaultSort.setOrder("desc");
            searchDto.setSortOption(defaultSort);
        }

        // 성별 변환 전후 확인 로그
        List<String> convertedGenders = null;
        if (searchDto.getGenders() != null) {
            log.info("변환 전 성별값: {}", searchDto.getGenders());
            convertedGenders = convertGenders(searchDto.getGenders());
            log.info("변환 후 성별값: {}", convertedGenders);
        }

        // 서비스 호출
        Page<ProductSearchResponseDto> resultPage = productColorSearchService.searchToDto(
                searchDto.getKeyword(),
                searchDto.getCategoryIds(),
                convertedGenders,
                searchDto.getBrandIds(),
                searchDto.getCollectionIds(),
                searchDto.getColors(),
                searchDto.getSizes(),
                searchDto.getMinPrice(),
                searchDto.getMaxPrice(),
                searchDto.getSortOption(),
                pageable
        );

        log.info("검색 결과 - 총 항목 수: {}, 총 페이지 수: {}",
                resultPage.getTotalElements(), resultPage.getTotalPages());

        commonDto.PageDto<ProductSearchResponseDto> responseDto = toPageDto(resultPage);
        log.info("========== 검색 API 호출 완료 ==========");
        return ResponseEntity.ok(responseDto);
    }

    private <T> commonDto.PageDto<T> toPageDto(Page<T> pageResult) {
        return new commonDto.PageDto<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),  // 현재 페이지 index(0-based)
                pageResult.getSize()
        );
    }

    /**
     * 1) 자동완성(Autocomplete)용 엔드포인트
     *    - ?q=키워드
     *    - 최대 10건
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocompleteProducts(
            @RequestParam(name = "q", required = false) String query
    ) {
        log.info("자동완성 요청 - 쿼리: {}", query);

        // q가 null 또는 빈 문자열이면 빈 배열 반환
        if (query == null || query.isBlank()) {
            log.info("자동완성 쿼리가 비어있어 빈 결과 반환");
            return ResponseEntity.ok(List.of());
        }

        // 서비스 호출
        List<String> suggestions = productColorSearchService.autocomplete(query, 10);
        log.info("자동완성 결과 - 제안 수: {}", suggestions.size());

        // 문자열 리스트(자동완성용) 반환
        return ResponseEntity.ok(suggestions);
    }

    /**
     * GenderType 열거형을 String으로 변환 (Elasticsearch 인덱스와 일치하도록)
     * 로깅 강화 및 안전한 변환 보장
     */
    private List<String> convertGenders(List<GenderType> genderTypes) {
        // null 체크
        if (genderTypes == null) {
            log.info("성별 목록이 null이므로 빈 리스트 반환");
            return Collections.emptyList();
        }

        // 안전한 매핑 (null 요소가 있을 경우 제외)
        List<String> result = genderTypes.stream()
                .filter(gender -> gender != null)
                .map(gender -> {
                    String genderName = gender.name();
                    log.debug("성별 변환: {} -> {}", gender, genderName);
                    return genderName;
                })
                .collect(Collectors.toList());

        log.info("변환된 성별 목록: {}", result);
        return result;
    }

    /**
     * 엘라스틱서치에 저장된 모든 ProductColorIndex를 페이징하여 직접 반환 (디버깅용)
     */
    @GetMapping("/raw-indices")
    public ResponseEntity<commonDto.PageDto<ProductColorIndex>> getRawIndices(Pageable pageable) {
        log.info("Raw indices 요청 - 페이지: {}, 사이즈: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductColorIndex> indexPage = productColorSearchService.getAllIndexedColors(pageable);
        log.info("Raw indices 결과 - 총 항목 수: {}", indexPage.getTotalElements());

        commonDto.PageDto<ProductColorIndex> responseDto = toPageDto(indexPage);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 카테고리의 인덱스만 조회 (디버깅용)
     */
    @GetMapping("/raw-indices/category/{categoryId}")
    public ResponseEntity<commonDto.PageDto<ProductColorIndex>> getRawIndicesByCategory(
            @PathVariable Long categoryId,
            Pageable pageable) {
        log.info("카테고리별 raw indices 요청 - 카테고리ID: {}, 페이지: {}, 사이즈: {}",
                categoryId, pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductColorIndex> indexPage = productColorSearchService.getIndexedColorsByCategory(categoryId, pageable);
        log.info("카테고리별 raw indices 결과 - 총 항목 수: {}", indexPage.getTotalElements());

        commonDto.PageDto<ProductColorIndex> responseDto = toPageDto(indexPage);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 인덱스 통계 정보 반환 (디버깅용)
     */
    @GetMapping("/index-stats")
    public ResponseEntity<Map<String, Object>> getIndexStats() {
        log.info("인덱스 통계 요청");

        Map<String, Object> stats = productColorSearchService.getIndexStats();
        log.info("인덱스 통계 결과 - 통계 항목 수: {}", stats.size());

        return ResponseEntity.ok(stats);
    }
}