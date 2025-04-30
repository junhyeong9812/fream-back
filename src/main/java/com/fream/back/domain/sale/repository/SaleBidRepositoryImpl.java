package com.fream.back.domain.sale.repository;

import com.fream.back.domain.product.entity.QProduct;
import com.fream.back.domain.product.entity.QProductColor;
import com.fream.back.domain.product.entity.QProductImage;
import com.fream.back.domain.product.entity.QProductSize;
import com.fream.back.domain.sale.dto.SaleBidResponseDto;
import com.fream.back.domain.sale.dto.SaleBidStatusCountDto;
import com.fream.back.domain.sale.entity.BidStatus;
import com.fream.back.domain.sale.entity.QSale;
import com.fream.back.domain.sale.entity.QSaleBid;
import com.fream.back.domain.shipment.entity.QSellerShipment;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 판매 입찰 레포지토리 커스텀 구현체
 * QueryDSL을 사용한 복잡한 쿼리 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SaleBidRepositoryImpl implements SaleBidRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 필터 조건에 맞는 판매 입찰 목록 조회
     */
    @Override
    public Page<SaleBidResponseDto> findSaleBidsByFilters(
            String email,
            String saleBidStatus,
            String saleStatus,
            Pageable pageable
    ) {
        log.debug("판매 입찰 목록 조회 - 이메일: {}, 입찰 상태: {}, 판매 상태: {}, 페이지: {}",
                email, saleBidStatus, saleStatus, pageable);

        QSaleBid saleBid = QSaleBid.saleBid;
        QSale sale = QSale.sale;
        QProductSize productSize = QProductSize.productSize;
        QProductColor productColor = QProductColor.productColor;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;
        QSellerShipment sellerShipment = QSellerShipment.sellerShipment;

        // 필터 조건 생성
        BooleanExpression emailCondition = saleBid.seller.email.eq(email);
        BooleanExpression statusCondition = saleBidStatus != null ? saleBid.status.stringValue().eq(saleBidStatus) : null;
        BooleanExpression saleStatusCondition = saleStatus != null ? sale.status.stringValue().eq(saleStatus) : null;

        // 쿼리 실행
        try {
            // Main Query
            List<SaleBidResponseDto> content = queryFactory
                    .select(Projections.constructor(
                            SaleBidResponseDto.class,
                            saleBid.id,
                            product.id,
                            product.name,
                            product.englishName,
                            productSize.size,
                            productColor.colorName,
                            productImage.imageUrl,
                            saleBid.bidPrice,
                            saleBid.status.stringValue(),
                            sale.status.stringValue(),
                            sellerShipment.status.stringValue(),
                            saleBid.createdDate,
                            saleBid.modifiedDate,
                            saleBid.isInstantSale
                    ))
                    .from(saleBid)
                    .join(saleBid.seller).on(emailCondition)
                    .join(saleBid.productSize, productSize)
                    .join(productSize.productColor, productColor)
                    .join(productColor.product, product)
                    .join(productColor.thumbnailImage, productImage)
                    .leftJoin(saleBid.sale, sale)
                    .leftJoin(sale.sellerShipment, sellerShipment)
                    .where(
                            statusCondition,
                            saleStatusCondition
                    )
                    .orderBy(saleBid.createdDate.desc()) // 최신순 정렬
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();

            // Count Query for pagination
            Long countResult = queryFactory
                    .select(saleBid.count())
                    .from(saleBid)
                    .join(saleBid.seller).on(emailCondition)
                    .leftJoin(saleBid.sale, sale)
                    .where(
                            statusCondition,
                            saleStatusCondition
                    )
                    .fetchOne();

            // fetchOne()이 null을 반환할 수 있으므로 안전하게 처리
            long total = countResult != null ? countResult : 0L;

            log.debug("판매 입찰 조회 결과 - 총 {}건, 현재 페이지 {}건", total, content.size());
            return new PageImpl<>(content, pageable, total);
        } catch (Exception e) {
            log.error("판매 입찰 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 사용자별 판매 입찰 상태 카운트 조회
     */
    @Override
    public SaleBidStatusCountDto countSaleBidsByStatus(String email) {
        log.debug("판매 입찰 상태 카운트 조회 - 이메일: {}", email);

        QSaleBid saleBid = QSaleBid.saleBid;
        BooleanExpression emailCondition = saleBid.seller.email.eq(email);

        try {
            // 대기 중인 입찰 수
            Long pendingResult = queryFactory
                    .select(saleBid.count())
                    .from(saleBid)
                    .where(emailCondition.and(saleBid.status.eq(BidStatus.PENDING)))
                    .fetchOne();

            // 매칭 완료된 입찰 수
            Long matchedResult = queryFactory
                    .select(saleBid.count())
                    .from(saleBid)
                    .where(emailCondition.and(saleBid.status.eq(BidStatus.MATCHED)))
                    .fetchOne();

            // 취소 또는 완료된 입찰 수
            Long cancelledOrCompletedResult = queryFactory
                    .select(saleBid.count())
                    .from(saleBid)
                    .where(emailCondition.and(
                            saleBid.status.in(BidStatus.CANCELLED, BidStatus.COMPLETED)))
                    .fetchOne();

            // null 체크 및 기본값 설정 (fetchOne()이 null을 반환할 수 있음)
            long pendingCount = pendingResult != null ? pendingResult : 0L;
            long matchedCount = matchedResult != null ? matchedResult : 0L;
            long cancelledOrCompletedCount = cancelledOrCompletedResult != null ? cancelledOrCompletedResult : 0L;

            log.debug("판매 입찰 상태 카운트 - 대기: {}, 매칭: {}, 취소/완료: {}",
                    pendingCount, matchedCount, cancelledOrCompletedCount);

            return new SaleBidStatusCountDto(pendingCount, matchedCount, cancelledOrCompletedCount);
        } catch (Exception e) {
            log.error("판매 입찰 상태 카운트 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * ID로 판매 입찰 상세 정보 조회
     */
    @Override
    public SaleBidResponseDto findSaleBidById(Long saleBidId, String email) {
        log.debug("ID로 판매 입찰 조회 - ID: {}, 이메일: {}", saleBidId, email);

        QSaleBid saleBid = QSaleBid.saleBid;
        QSale sale = QSale.sale;
        QProductSize productSize = QProductSize.productSize;
        QProductColor productColor = QProductColor.productColor;
        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;
        QSellerShipment sellerShipment = QSellerShipment.sellerShipment;

        try {
            SaleBidResponseDto result = queryFactory
                    .select(Projections.constructor(
                            SaleBidResponseDto.class,
                            saleBid.id,
                            product.id,
                            product.name,
                            product.englishName,
                            productSize.size,
                            productColor.colorName,
                            productImage.imageUrl,
                            saleBid.bidPrice,
                            saleBid.status.stringValue(),
                            sale.status.stringValue(),
                            sellerShipment.status.stringValue(),
                            saleBid.createdDate,
                            saleBid.modifiedDate,
                            saleBid.isInstantSale
                    ))
                    .from(saleBid)
                    .join(saleBid.seller).on(saleBid.seller.email.eq(email))
                    .join(saleBid.productSize, productSize)
                    .join(productSize.productColor, productColor)
                    .join(productColor.product, product)
                    .join(productColor.thumbnailImage, productImage)
                    .leftJoin(saleBid.sale, sale)
                    .leftJoin(sale.sellerShipment, sellerShipment)
                    .where(saleBid.id.eq(saleBidId))
                    .fetchOne();

            log.debug("ID로 판매 입찰 조회 결과 - {}", (result != null ? "조회 성공" : "데이터 없음"));
            return result;
        } catch (Exception e) {
            log.error("ID로 판매 입찰 조회 중 오류 발생 - ID: {}", saleBidId, e);
            throw e;
        }
    }
}