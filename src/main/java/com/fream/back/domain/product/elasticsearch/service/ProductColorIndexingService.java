//package com.fream.back.domain.product.elasticsearch.service;
//
//import com.fream.back.domain.product.elasticsearch.dto.ProductColorIndexDto;
//import com.fream.back.domain.product.elasticsearch.dto.ProductColorSizeRow;
//import com.fream.back.domain.product.elasticsearch.index.ProductColorIndex;
//import com.fream.back.domain.product.elasticsearch.repository.ProductColorEsRepository;
//import com.fream.back.domain.product.elasticsearch.repository.ProductColorIndexQueryRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class ProductColorIndexingService {
//
//    private final ProductColorIndexQueryRepository queryRepository;
//    private final ProductColorEsRepository productColorEsRepository;
//
//    @Transactional
//    public void indexAllColors() {
//        // 1) DTO 목록 (기본 필드 + minPrice, maxPrice, interestCount)
//        List<ProductColorIndexDto> dtoList = queryRepository.findAllForIndexingDto();
//
//        // 2) 사이즈 목록 (colorId, size)
//        List<ProductColorSizeRow> sizeRows = queryRepository.findAllSizesForIndexing();
//
//        // 3) 사이즈를 colorId별로 grouping
//        Map<Long, List<String>> sizeMap = sizeRows.stream()
//                .collect(Collectors.groupingBy(
//                        ProductColorSizeRow::getColorId,
//                        Collectors.mapping(ProductColorSizeRow::getSize, Collectors.toList())
//                ));
//
//        // 4) 두 정보를 합쳐서 ProductColorIndex 생성
//        List<ProductColorIndex> indexList = dtoList.stream()
//                .map(dto -> {
//                    List<String> sizes = sizeMap.getOrDefault(dto.getColorId(), Collections.emptyList());
//
//                    return ProductColorIndex.builder()
//                            .colorId(dto.getColorId())
//                            .productId(dto.getProductId())
//                            .productName(dto.getProductName())
//                            .productEnglishName(dto.getProductEnglishName())
//                            .brandName(dto.getBrandName())
//                            .categoryName(dto.getCategoryName())
//                            .collectionName(dto.getCollectionName())
//                            .brandId(dto.getBrandId())
//                            .categoryId(dto.getCategoryId())
//                            .collectionId(dto.getCollectionId())
//                            .colorName(dto.getColorName())
//                            .gender(dto.getGender())
//                            .releasePrice(dto.getReleasePrice())
//                            .minPrice(dto.getMinPrice())
//                            .maxPrice(dto.getMaxPrice())
//                            .interestCount(dto.getInterestCount())
//                            .releaseDate(dto.getReleaseDate()) // 새로 추가
//                            .thumbnailUrl(dto.getThumbnailUrl())
//                            .sizes(sizes)
//                            .build();
//                })
//                .collect(Collectors.toList());
//
//        // 5) Elasticsearch에 저장
//        productColorEsRepository.saveAll(indexList);
//    }
//
//    /**
//     * 단일 colorId 대상 인덱싱
//     * (상품 색상 추가/업데이트 시 호출)
//     */
//    @Transactional
//    public void indexColorById(Long colorId) {
//        // 1) 기본 정보 (minPrice, maxPrice, interestCount 등)
//        ProductColorIndexDto dto = queryRepository.findOneForIndexingDto(colorId);
//        if (dto == null) {
//            throw new IllegalArgumentException("해당 colorId가 존재하지 않습니다. colorId=" + colorId);
//        }
//
//        // 2) 사이즈 목록
//        List<ProductColorSizeRow> sizeRows = queryRepository.findSizesByColorId(colorId);
//        // colorId 하나이므로, sizeRows 전체가 동일 colorId
//        List<String> sizes = sizeRows.stream()
//                .map(ProductColorSizeRow::getSize)
//                .toList();
//
//        // 3) ProductColorIndex 빌드
//        ProductColorIndex indexObj = ProductColorIndex.builder()
//                .colorId(dto.getColorId())
//                .productId(dto.getProductId())
//                .productName(dto.getProductName())
//                .productEnglishName(dto.getProductEnglishName())
//                .brandName(dto.getBrandName())
//                .categoryName(dto.getCategoryName())
//                .collectionName(dto.getCollectionName())
//                .brandId(dto.getBrandId())
//                .categoryId(dto.getCategoryId())
//                .collectionId(dto.getCollectionId())
//                .colorName(dto.getColorName())
//                .gender(dto.getGender())
//                .releasePrice(dto.getReleasePrice())
//                .minPrice(dto.getMinPrice())
//                .maxPrice(dto.getMaxPrice())
//                .interestCount(dto.getInterestCount())
//                .releaseDate(dto.getReleaseDate()) // 새로 추가
//                .thumbnailUrl(dto.getThumbnailUrl())
//                .sizes(sizes) // 사이즈 목록을 최종 주입
//                .build();
//
//        // 4) Elasticsearch에 저장 (upsert)
//        productColorEsRepository.save(indexObj);
//    }
//    //colorId 문서를 인덱스에서 삭제
//    @Transactional
//    public void deleteColorFromIndex(Long colorId) {
//        // colorId는 Elasticsearch 문서의 @Id로 쓰이므로
//        productColorEsRepository.deleteById(colorId);
//    }
//
//
//
////    @Transactional(readOnly = true)
////    public void indexAllColors() {
////        // 1) DB에서 ProductColor + 연관 정보 조회
////        List<ProductColor> colorList = queryRepository.findAllForIndexing();
////
////        // 2) 변환 (Entity -> Index DTO)
////        List<ProductColorIndex> indexList = colorList.stream()
////                .map(this::toIndex)
////                .collect(Collectors.toList());
////
////        // 3) Elasticsearch에 저장
////        productColorEsRepository.saveAll(indexList);
////    }
////
////    @Transactional(readOnly = true)
////    public void indexColorById(Long colorId) {
////        // 1) DB에서 해당 colorId만 조회
////        ProductColor pc = queryRepository.findOneForIndexing(colorId);
////        if (pc == null) {
////            throw new IllegalArgumentException("해당 ProductColor가 존재하지 않습니다. id=" + colorId);
////        }
////
////        // 2) 변환(엔티티 -> 인덱스 DTO)
////        ProductColorIndex indexObj = toIndex(pc);
////
////        // 3) Elasticsearch에 save (단건)
////        productColorEsRepository.save(indexObj);
////    }
//
//
////    private ProductColorIndex toIndex(ProductColor pc) {
////        Product p = pc.getProduct();
////        int minPrice = pc.getSizes().stream()
////                .mapToInt(ProductSize::getPurchasePrice)
////                .min()
////                .orElse(p.getReleasePrice());
////        int maxPrice = pc.getSizes().stream()
////                .mapToInt(ProductSize::getPurchasePrice)
////                .max()
////                .orElse(p.getReleasePrice());
////
////        // 사이즈 목록
////        List<String> sizes = pc.getSizes().stream()
////                .map(ProductSize::getSize)
////                .collect(Collectors.toList());
////
////        // 관심 수
////        long interestCount = (long) pc.getInterests().size();
////
////        return ProductColorIndex.builder()
////                .colorId(pc.getId())
////                .productId(p.getId())
////                .productName(p.getName())
////                .productEnglishName(p.getEnglishName())
////                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
////                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
////                .collectionName(p.getCollection() != null ? p.getCollection().getName() : null)
////                .brandId(p.getBrand() != null ? p.getBrand().getId() : null)
////                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
////                .collectionId(p.getCollection() != null ? p.getCollection().getId() : null)
////
////                .colorName(pc.getColorName())
////                .gender(p.getGender().name()) // MALE/FEMALE/KIDS/UNISEX
////                .releasePrice(p.getReleasePrice())
////                .minPrice(minPrice)
////                .maxPrice(maxPrice)
////                .interestCount(interestCount)
////
////                .sizes(sizes)
////                .build();
////    }
//}