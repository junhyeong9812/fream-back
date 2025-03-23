package com.fream.back.domain.order.service.command;

import com.fream.back.domain.order.entity.BidStatus;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderBid;
import com.fream.back.domain.order.exception.*;
import com.fream.back.domain.order.repository.OrderBidRepository;
import com.fream.back.domain.payment.dto.PaymentRequestDto;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.product.service.productSize.ProductSizeQueryService;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.sale.service.query.SaleBidQueryService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBidCommandService {

    private final OrderBidRepository orderBidRepository;
    private final OrderCommandService orderCommandService;
    private final ProductSizeQueryService productSizeQueryService;
    private final UserQueryService userQueryService;
    private final SaleBidQueryService saleBidQueryService;

    /**
     * 주문 입찰을 생성합니다.
     *
     * @param email 사용자 이메일
     * @param productSizeId 상품 사이즈 ID
     * @param bidPrice 입찰 가격
     * @return 생성된 주문 입찰
     * @throws OrderBidCreationFailedException 주문 입찰 생성 실패 시
     * @throws ProductSizeNotFoundException 상품 사이즈를 찾을 수 없는 경우
     * @throws InvalidBidPriceException 입찰 가격이 유효하지 않은 경우
     */
    @Transactional
    public OrderBid createOrderBid(String email, Long productSizeId, int bidPrice) {
        try {
            // 입력값 검증
            if (email == null || email.trim().isEmpty()) {
                throw new OrderBidAccessDeniedException("사용자 이메일이 없습니다.");
            }
            if (productSizeId == null) {
                throw new ProductSizeNotFoundException("상품 사이즈 ID가 없습니다.");
            }
            if (bidPrice <= 0) {
                throw new InvalidBidPriceException("입찰 가격은 0보다 커야 합니다. 현재 가격: " + bidPrice);
            }

            // 1. User 조회
            User user = userQueryService.findByEmail(email);

            // 2. ProductSize 조회
            ProductSize productSize = productSizeQueryService.findById(productSizeId)
                    .orElseThrow(() -> new ProductSizeNotFoundException("해당 사이즈를 찾을 수 없습니다(ID: " + productSizeId + ")"));

            // 3. Order 생성
            Order order = orderCommandService.createOrderFromBid(user, productSize, bidPrice);

            // 4. OrderBid 생성
            OrderBid orderBid = OrderBid.builder()
                    .user(user)
                    .productSize(productSize)
                    .bidPrice(bidPrice)
                    .status(BidStatus.PENDING)
                    .order(order) // Order 매핑
                    .build();

            // 양방향 관계 설정
            orderBid.assignOrder(order);

            // 5. OrderBid 저장
            return orderBidRepository.save(orderBid);
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("주문 입찰 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderBidCreationFailedException("주문 입찰 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 즉시 구매 주문 입찰을 생성합니다.
     *
     * @param buyerEmail 구매자 이메일
     * @param saleBidId 판매 입찰 ID
     * @param addressId 배송지 ID
     * @param isWarehouseStorage 창고 보관 여부
     * @param paymentRequest 결제 요청 정보
     * @return 생성된 주문 입찰
     * @throws OrderBidCreationFailedException 주문 입찰 생성 실패 시
     * @throws SaleBidNotFoundException 판매 입찰을 찾을 수 없는 경우
     * @throws InvalidOrderBidDataException 주문 입찰 정보가 유효하지 않은 경우
     */
    @Transactional
    public OrderBid createInstantOrderBid(String buyerEmail, Long saleBidId, Long addressId,
                                          boolean isWarehouseStorage, PaymentRequestDto paymentRequest) {
        try {
            // 입력값 검증
            if (buyerEmail == null || buyerEmail.trim().isEmpty()) {
                throw new OrderBidAccessDeniedException("구매자 이메일이 없습니다.");
            }
            if (saleBidId == null) {
                throw new SaleBidNotFoundException("판매 입찰 ID가 없습니다.");
            }
            if (addressId == null) {
                throw new InvalidOrderBidDataException("배송지 ID가 없습니다.");
            }
            if (paymentRequest == null) {
                throw new InvalidPaymentShipmentDataException("결제 정보가 없습니다.");
            }

            // 1. 유저 조회
            User buyer = userQueryService.findByEmail(buyerEmail);

            // 2. SaleBid 조회
            SaleBid saleBid = saleBidQueryService.findById(saleBidId);
            if (saleBid == null) {
                throw new SaleBidNotFoundException("판매 입찰을 찾을 수 없습니다(ID: " + saleBidId + ")");
            }

            Sale sale = saleBid.getSale();
            if (sale == null) {
                throw new InvalidOrderBidDataException("판매 입찰과 연결된 판매 정보가 없습니다.");
            }

            // 3. Order 생성
            Order order = orderCommandService.createInstantOrder(
                    buyer,
                    saleBid,
                    addressId,
                    isWarehouseStorage,
                    paymentRequest
            );

            // 4. OrderBid 생성
            ProductSize productSize = saleBid.getProductSize();

            OrderBid orderBid = OrderBid.builder()
                    .user(order.getUser()) // 구매자
                    .productSize(productSize)
                    .bidPrice(saleBid.getBidPrice())
                    .status(BidStatus.MATCHED) // 즉시 구매는 바로 매칭 상태
                    .order(order) // Order와 매핑
                    .sale(sale) // 셀러 정보 추가
                    .build();

            // 양방향 관계 설정
            orderBid.assignOrder(order);

            // 플래그 설정 및 저장
            orderBid.markAsInstantPurchase();
            return orderBidRepository.save(orderBid);
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("즉시 구매 주문 입찰 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderBidCreationFailedException("즉시 구매 주문 입찰 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 입찰을 매칭 상태로 업데이트합니다.
     *
     * @param orderBid 주문 입찰
     * @throws InvalidOrderBidStatusException 주문 입찰 상태가 유효하지 않은 경우
     * @throws OrderBidMatchingFailedException 주문 입찰 매칭 실패 시
     */
    @Transactional
    public void matchOrderBid(OrderBid orderBid) {
        try {
            if (orderBid == null) {
                throw new OrderBidNotFoundException("주문 입찰 정보가 없습니다.");
            }

            if (orderBid.getStatus() == BidStatus.MATCHED) {
                throw new OrderBidAlreadyMatchedException("이미 매칭된 주문 입찰입니다(ID: " + orderBid.getId() + ")");
            }

            orderBid.updateStatus(BidStatus.MATCHED);
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("주문 입찰 매칭 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderBidMatchingFailedException("주문 입찰 매칭 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 입찰을 삭제합니다.
     *
     * @param orderBidId 주문 입찰 ID
     * @throws OrderBidNotFoundException 주문 입찰을 찾을 수 없는 경우
     * @throws OrderBidAlreadyMatchedException 이미 매칭된 주문 입찰인 경우
     * @throws OrderBidDeletionFailedException 주문 입찰 삭제 실패 시
     */
    @Transactional
    public void deleteOrderBid(Long orderBidId) {
        try {
            // OrderBid 조회
            OrderBid orderBid = orderBidRepository.findById(orderBidId)
                    .orElseThrow(() -> new OrderBidNotFoundException("해당 주문 입찰을 찾을 수 없습니다(ID: " + orderBidId + ")"));

            // Sale과 연결 여부 확인
            if (orderBid.getSale() != null) {
                throw new OrderBidAlreadyMatchedException("주문 입찰이 판매와 연결되어 있어 삭제할 수 없습니다.");
            }

            // 매칭 상태 확인
            if (orderBid.getStatus() == BidStatus.MATCHED) {
                throw new OrderBidAlreadyMatchedException("이미 매칭된 주문 입찰은 삭제할 수 없습니다.");
            }

            // Order와 연결 여부 확인 후 삭제
            if (orderBid.getOrder() != null) {
                orderCommandService.deleteOrder(orderBid.getOrder().getId());
            }

            // OrderBid 삭제
            orderBidRepository.delete(orderBid);
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("주문 입찰 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderBidDeletionFailedException("주문 입찰 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}