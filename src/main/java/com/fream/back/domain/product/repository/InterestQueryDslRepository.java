package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관심 상품 관련 QueryDSL 레포지토리
 * 복잡한 관심 상품 조회 쿼리를 처리합니다.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class InterestQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 사용자의 관심 상품 목록 조회
     *
     * @param userId 사용자 ID
     * @param sortOption 정렬 옵션
     * @param pageable 페이징 정보
     * @return 페이징된 상품 검색 응답 DTO
     */
    public Page<ProductSearchResponseDto> findUserInterestProducts(
            Long userId,
            SortOption sortOption,
            Pageable pageable) {
        log.debug("사용자 관심 상품 목록 조회 시작 - 사용자ID: {}, 페이지: {}, 사이즈: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // QueryDSL Q-클래스 초기화
        QInterest interest = QInterest.interest;
        QInterest interest1 = new QInterest("interest1");
        QProduct product = QProduct.product;
        QProductColor productColor = QProductColor.productColor;
        QProductSize productSize = QProductSize.productSize;
        QProductImage productImage = QProductImage.productImage;

        try {
            // 데이터 조회 쿼리 (페이징 처리)
            log.debug("관심 상품 데이터 조회 쿼리 실행 - 사용자ID: {}", userId);
            List<Tuple> results = queryFactory.select(
                            interest.id,
                            product.id,
                            product.name,
                            product.englishName,
                            product.releasePrice, // releasePrice 추가
                            productColor.id,
                            productColor.thumbnailImage.imageUrl,
                            productColor.colorName,
                            productSize.purchasePrice.min(),
                            JPAExpressions
                                    .select(interest1.count())
                                    .from(interest1)
                                    .where(interest1.productColor.eq(productColor))
                    )
                    .from(interest)
                    .leftJoin(interest.productColor, productColor) // 일반 Join
                    .leftJoin(productColor.product, product) // 일반 Join
                    .leftJoin(productColor.thumbnailImage, productImage) // Thumbnail 이미지 조인 추가
                    .leftJoin(productColor.sizes, productSize) // 일반 Join
                    .leftJoin(productColor.interests, interest1)
                    .where(interest.user.id.eq(userId))
                    .groupBy(
                            product.id,
                            productColor.id,
                            productColor.thumbnailImage.imageUrl)
                    .orderBy(getOrderByClause(sortOption, product, productColor, productSize)) // 정렬 추가
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();

            log.debug("관심 상품 데이터 조회 완료 - 결과 수: {}", results.size());

            // 전체 데이터 수 조회
            log.debug("관심 상품 전체 개수 조회 시작 - 사용자ID: {}", userId);
            long total = queryFactory.select(interest.count())
                    .from(interest)
                    .where(interest.user.id.eq(userId))
                    .fetchOne();

            log.debug("관심 상품 전체 개수 조회 완료 - 총 개수: {}", total);

            // 결과 매핑
            log.debug("관심 상품 데이터 DTO 변환 시작");
            List<ProductSearchResponseDto> content = results.stream()
                    .map(tuple -> {
                        Long interestCount = tuple.get(JPAExpressions
                                .select(interest1.count())
                                .from(interest1)
                                .where(interest1.productColor.eq(productColor)));

                        return ProductSearchResponseDto.builder()
                                .id(tuple.get(product.id))
                                .name(tuple.get(product.name))
                                .englishName(tuple.get(product.englishName))
                                .releasePrice(tuple.get(product.releasePrice))
                                .thumbnailImageUrl(tuple.get(productColor.thumbnailImage.imageUrl))
                                .price(tuple.get(productSize.purchasePrice.min()))
                                .colorName(tuple.get(productColor.colorName))
                                .colorId(tuple.get(productColor.id))
                                .interestCount(interestCount) // 관심 수 설정
                                .build();
                    })
                    .collect(Collectors.toList());

            log.debug("관심 상품 데이터 DTO 변환 완료 - DTO 수: {}", content.size());

            Page<ProductSearchResponseDto> resultPage = new PageImpl<>(content, pageable, total);
            log.debug("사용자 관심 상품 목록 조회 완료 - 총 결과 수: {}, 총 페이지 수: {}",
                    resultPage.getTotalElements(), resultPage.getTotalPages());

            return resultPage;
        } catch (Exception e) {
            log.error("사용자 관심 상품 목록 조회 중 예상치 못한 오류 발생 - 사용자ID: {}", userId, e);
            throw e; // 상위 레이어에서 처리할 수 있도록 전파
        }
    }

    /**
     * 정렬 조건 설정
     *
     * @param sortOption 정렬 옵션
     * @param product 상품 Q클래스
     * @param productColor 상품 색상 Q클래스
     * @param productSize 상품 사이즈 Q클래스
     * @return 정렬 조건
     */
    private OrderSpecifier<?> getOrderByClause(
            SortOption sortOption,
            QProduct product,
            QProductColor productColor,
            QProductSize productSize) {
        log.debug("정렬 조건 설정 - 정렬 옵션: {}", sortOption);

        String field = sortOption != null ? sortOption.getField() : null;
        String order = sortOption != null ? sortOption.getOrder() : null;

        if (field == null || order == null) {
            log.debug("기본 정렬 적용 - 상품ID 오름차순");
            return product.id.asc(); // 기본 정렬
        }

        if (sortOption != null) {
            log.debug("정렬 옵션 적용 - 필드: {}, 순서: {}", field, order);
            switch (sortOption.getField()) {
                case "price":
                    return "asc".equalsIgnoreCase(sortOption.getOrder())
                            ? productSize.purchasePrice.asc()
                            : productSize.purchasePrice.desc();
                case "releaseDate":
                    return "asc".equalsIgnoreCase(sortOption.getOrder())
                            ? product.releaseDate.asc()
                            : product.releaseDate.desc();
                case "interestCount":
                    return "asc".equalsIgnoreCase(sortOption.getOrder())
                            ? productColor.interests.size().sum().asc()
                            : productColor.interests.size().sum().desc();
                default:
                    log.debug("알 수 없는 정렬 필드 - 기본 정렬 적용: {}", field);
                    break;
            }
        }
        return product.id.asc(); // 기본 정렬
    }
}