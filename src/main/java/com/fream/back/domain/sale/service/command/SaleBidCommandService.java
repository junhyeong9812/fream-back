package com.fream.back.domain.sale.service.command;

import com.fream.back.domain.order.entity.OrderBid;
import com.fream.back.domain.order.service.query.OrderBidQueryService;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.product.service.productSize.ProductSizeQueryService;
import com.fream.back.domain.sale.entity.BidStatus;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.sale.repository.SaleBidRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleBidCommandService {

    private final SaleBidRepository saleBidRepository;
    private final UserQueryService userQueryService;
    private final ProductSizeQueryService productSizeQueryService;
    private final SaleCommandService saleCommandService;
    private final OrderBidQueryService orderBidQueryService;
    @Transactional
    public SaleBid createSaleBid(String sellerEmail, Long productSizeId, int bidPrice,
                                 String returnAddress, String postalCode, String receiverPhone
                                , boolean isWarehouseStorage) {
        // 1. User 조회
        User seller = userQueryService.findByEmail(sellerEmail);

        // 2. ProductSize 조회
        ProductSize productSize = productSizeQueryService.findById(productSizeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품 사이즈를 찾을 수 없습니다: " + productSizeId));

        // 3. Sale 생성
        Sale sale = saleCommandService.createSale(seller, productSize, returnAddress, postalCode, receiverPhone,isWarehouseStorage);

        // 4. SaleBid 생성
        SaleBid saleBid = SaleBid.builder()
                .seller(seller)
                .productSize(productSize)
                .bidPrice(bidPrice)
                .status(BidStatus.PENDING)
                .sale(sale) // 연관된 Sale 설정
                .build();

        // 5. SaleBid 저장
        saleBid = saleBidRepository.save(saleBid);

        // 6. Sale에 SaleBid 추가 (연관관계 설정)
        sale.assignSaleBid(saleBid);

        // 7. SaleBid 반환
        return saleBid;
    }
    @Transactional
    public Long createInstantSaleBid(Long orderBidId,
                                        String sellerEmail,
                                        String returnAddress,
                                        String postalCode,
                                        String receiverPhone) {

        Sale sale=saleCommandService.createInstantSale(orderBidId,sellerEmail,returnAddress,postalCode,receiverPhone);
        // 1. OrderBid 조회
        OrderBid orderBid = orderBidQueryService.findById(orderBidId)
                .orElseThrow(() -> new IllegalArgumentException("해당 OrderBid를 찾을 수 없습니다."));


        // 3. SaleBid 생성
        SaleBid saleBid = SaleBid.builder()
                .seller(sale.getSeller()) // Sale의 판매자
                .productSize(sale.getProductSize()) // Sale의 ProductSize
                .bidPrice(orderBid.getBidPrice()) // OrderBid의 입찰 가격 사용
                .status(BidStatus.MATCHED) // 즉시 판매는 바로 매칭 상태
                .sale(sale) // Sale과 매핑
                .order(orderBid.getOrder()) // Order와 매핑
                .build();

        // 양방향 관계 설정
        saleBid.assignSale(sale);
        saleBid.assignOrder(orderBid.getOrder());
        sale.assignSaleBid(saleBid); // Sale과 SaleBid 양방향 연관관계 설정

        // SaleBid 저장
        saleBidRepository.save(saleBid);

        // Sale ID 반환
        return sale.getId();
    }

    @Transactional
    public void deleteSaleBid(Long saleBidId) {
        // SaleBid 조회
        SaleBid saleBid = saleBidRepository.findById(saleBidId)
                .orElseThrow(() -> new IllegalArgumentException("해당 SaleBid를 찾을 수 없습니다: " + saleBidId));

        // Order와 연결 여부 확인
        if (saleBid.getOrder() != null) {
            throw new IllegalStateException("SaleBid는 Order와 연결되어 있으므로 삭제할 수 없습니다.");
        }

        // Sale과 연결 여부 확인 후 삭제
        if (saleBid.getSale() != null) {
            saleCommandService.deleteSale(saleBid.getSale().getId());
        }

        // SaleBid 삭제
        saleBidRepository.delete(saleBid);
    }
}
