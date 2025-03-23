package com.fream.back.domain.order.service.command;

import com.fream.back.domain.address.dto.AddressResponseDto;
import com.fream.back.domain.address.service.query.AddressQueryService;
import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.entity.BidStatus;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.entity.OrderStatus;
import com.fream.back.domain.order.exception.*;
import com.fream.back.domain.order.repository.OrderRepository;
import com.fream.back.domain.order.service.query.OrderBidQueryService;
import com.fream.back.domain.payment.dto.PaymentRequestDto;
import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.payment.service.command.PaymentCommandService;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStatus;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.exception.WarehouseStorageException;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final PaymentCommandService paymentCommandService;
    private final OrderShipmentCommandService orderShipmentCommandService;
    private final OrderItemCommandService orderItemCommandService;
    private final OrderBidQueryService orderBidQueryService;
    private final UserQueryService userQueryService;
    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final AddressQueryService addressQueryService;
    private final EntityManager entityManager;

    /**
     * 입찰에서 주문을 생성합니다.
     *
     * @param user 사용자 정보
     * @param productSize 상품 사이즈 정보
     * @param bidPrice 입찰 가격
     * @return 생성된 주문
     * @throws OrderCreationFailedException 주문 생성 실패 시
     * @throws InvalidBidPriceException 입찰 가격이 유효하지 않을 경우
     */
    @Transactional
    public Order createOrderFromBid(User user, ProductSize productSize, int bidPrice) {
        try {
            // 입력값 검증
            if (user == null) {
                throw new InvalidOrderDataException("사용자 정보가 없습니다.");
            }
            if (productSize == null) {
                throw new ProductSizeNotFoundException("상품 사이즈 정보가 없습니다.");
            }
            if (bidPrice <= 0) {
                throw new InvalidBidPriceException("입찰 가격은 0보다 커야 합니다. 현재 가격: " + bidPrice);
            }

            // 1. OrderItem 생성
            OrderItem orderItem = orderItemCommandService.createOrderItem(null, productSize, bidPrice);

            // 2. Order 생성
            Order order = Order.builder()
                    .user(user)
                    .totalAmount(bidPrice) // 입찰가를 총 금액으로 설정
                    .discountAmount(0) // 초기값은 0
                    .usedPoints(0) // 초기값은 0
                    .status(OrderStatus.PENDING_PAYMENT) // 결제 대기 상태
                    .build();

            // 3. OrderItem과 연관 설정
            order.addOrderItem(orderItem);

            // 4. Order 저장
            return orderRepository.save(order);
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("주문 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderCreationFailedException("입찰에서 주문 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 주문에 대한 결제 및 배송 정보를 처리합니다.
     *
     * @param orderId 주문 ID
     * @param userEmail 사용자 이메일
     * @param requestDto 결제 및 배송 요청 정보
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우
     * @throws OrderAccessDeniedException 주문에 대한 접근 권한이 없는 경우
     * @throws OrderPaymentProcessingFailedException 결제 처리 실패 시
     * @throws OrderShipmentProcessingFailedException 배송 처리 실패 시
     * @throws OrderWarehouseStorageProcessingFailedException 창고 보관 처리 실패 시
     * @throws InvalidPaymentShipmentDataException 결제 및 배송 정보가 유효하지 않은 경우
     */
    @Transactional
    public void processPaymentAndShipment(Long orderId, String userEmail, PayAndShipmentRequestDto requestDto) {
        if (orderId == null) {
            throw new InvalidOrderDataException("주문 ID가 없습니다.");
        }
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new OrderAccessDeniedException("사용자 정보가 없습니다.");
        }
        if (requestDto == null) {
            throw new InvalidPaymentShipmentDataException("결제 및 배송 정보가 없습니다.");
        }

        try {
            // 1. User 및 Order 조회
            User user = userQueryService.findByEmail(userEmail);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("해당 주문을 찾을 수 없습니다(ID: " + orderId + ")"));

            // 접근 권한 확인
            if (!order.getUser().getId().equals(user.getId())) {
                throw new OrderAccessDeniedException("해당 사용자는 이 주문을 처리할 권한이 없습니다.");
            }

            // 주문 정보 유효성 검사
            validatePaymentShipmentRequest(requestDto);

            try {
                // 2. 결제 처리
                Payment payment = paymentCommandService.processPayment(order, user, requestDto.getPaymentRequest());
                order.assignPayment(payment);
            } catch (Exception e) {
                log.error("결제 처리 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderPaymentProcessingFailedException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            try {
                // 3. 배송 정보 생성
                OrderShipment shipment = orderShipmentCommandService.createOrderShipment(
                        order,
                        requestDto.getReceiverName(),
                        requestDto.getReceiverPhone(),
                        requestDto.getPostalCode(),
                        requestDto.getAddress()
                );
                order.assignOrderShipment(shipment);
            } catch (Exception e) {
                log.error("배송 정보 처리 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderShipmentProcessingFailedException("배송 정보 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            try {
                // 4. 상태 업데이트
                if (requestDto.isWarehouseStorage()) {
                    // 창고 보관일 경우
                    order.updateStatus(OrderStatus.PAYMENT_COMPLETED);
                    order.updateStatus(OrderStatus.PREPARING);
                    order.updateStatus(OrderStatus.IN_WAREHOUSE);

                    // 창고 보관 처리 추가
                    WarehouseStorage warehouseStorage = warehouseStorageCommandService.createOrderStorage(order, user);
                    order.assignWarehouseStorage(warehouseStorage);
                    order.updateStatus(OrderStatus.COMPLETED);  // 주문 완료 상태로 변경
                } else {
                    // 실제 배송일 경우
                    order.updateStatus(OrderStatus.PAYMENT_COMPLETED);
                    order.updateStatus(OrderStatus.PREPARING);
                }
            } catch (WarehouseStorageException e) {
                log.error("창고 보관 처리 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderWarehouseStorageProcessingFailedException("창고 보관 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("주문 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderStatusUpdateFailedException("주문 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            // 5. OrderBid 상태 업데이트
            try {
                orderBidQueryService.findById(orderId).ifPresent(orderBid -> {
                    orderBid.updateStatus(BidStatus.MATCHED);
                });
            } catch (Exception e) {
                log.error("주문 입찰 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
                // 중요 정보 처리가 완료되었으므로 예외를 던지지 않고 로그만 남김
            }
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("결제 및 배송 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderPaymentProcessingFailedException("결제 및 배송 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 즉시 구매 주문을 생성합니다.
     *
     * @param buyer 구매자 정보
     * @param saleBid 판매 입찰 정보
     * @param addressId 배송지 ID
     * @param isWarehouseStorage 창고 보관 여부
     * @param paymentRequest 결제 요청 정보
     * @return 생성된 주문
     * @throws OrderCreationFailedException 주문 생성 실패 시
     * @throws SaleBidNotFoundException 판매 입찰을 찾을 수 없는 경우
     * @throws InvalidOrderDataException 주문 정보가 유효하지 않은 경우
     * @throws OrderPaymentProcessingFailedException 결제 처리 실패 시
     * @throws OrderWarehouseStorageProcessingFailedException 창고 보관 처리 실패 시
     */
    @Transactional
    public Order createInstantOrder(User buyer, SaleBid saleBid, Long addressId,
                                    boolean isWarehouseStorage, PaymentRequestDto paymentRequest) {
        try {
            // 입력값 검증
            if (buyer == null) {
                throw new InvalidOrderDataException("구매자 정보가 없습니다.");
            }
            if (saleBid == null) {
                throw new SaleBidNotFoundException("판매 입찰 정보가 없습니다.");
            }
            if (addressId == null) {
                throw new InvalidOrderDataException("배송지 정보가 없습니다.");
            }
            if (paymentRequest == null) {
                throw new InvalidPaymentShipmentDataException("결제 정보가 없습니다.");
            }

            // 1. 주소 조회
            AddressResponseDto address = addressQueryService.getAddress(buyer.getEmail(), addressId);

            // 2. Order 생성
            Order order = Order.builder()
                    .user(buyer)
                    .totalAmount(saleBid.getBidPrice())
                    .status(OrderStatus.PENDING_PAYMENT)
                    .build();

            order = orderRepository.save(order);

            // PaymentRequest에 OrderId 설정
            paymentRequest.setOrderId(order.getId());
            log.debug("결제 요청 정보: {}", paymentRequest);

            // 3. OrderItem 추가
            try {
                OrderItem orderItem = orderItemCommandService.createOrderItem(order, saleBid.getProductSize(), saleBid.getBidPrice());
                order.addOrderItem(orderItem);
            } catch (Exception e) {
                log.error("주문 항목 생성 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderItemCreationFailedException("주문 항목 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            // 4. 배송 정보 생성 및 연관 설정
            try {
                OrderShipment orderShipment = orderShipmentCommandService.createOrderShipment(
                        order,
                        address.getRecipientName(),
                        address.getPhoneNumber(),
                        address.getZipCode(),
                        address.getAddress()
                );
                order.assignOrderShipment(orderShipment);
            } catch (Exception e) {
                log.error("배송 정보 생성 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderShipmentProcessingFailedException("배송 정보 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            try {
                // 판매 입찰 상태 업데이트
                saleBid.assignOrder(order);
                saleBid.updateStatus(com.fream.back.domain.sale.entity.BidStatus.MATCHED);

                // 변경 내용 강제 반영
                entityManager.flush();
            } catch (Exception e) {
                log.error("판매 입찰 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderBidMatchingFailedException("판매 입찰 매칭 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            // Sale의 isWarehouseStorage 확인 후 상태 변경
            try {
                Sale sale = saleBid.getSale();
                if (sale != null && sale.isWarehouseStorage()) {
                    warehouseStorageCommandService.updateWarehouseStatus(sale, WarehouseStatus.ASSOCIATED_WITH_ORDER);
                }
            } catch (WarehouseStorageException e) {
                log.error("창고 보관 상태 업데이트 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderWarehouseStorageProcessingFailedException("창고 보관 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("창고 보관 상태 업데이트 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
                throw new OrderWarehouseStorageProcessingFailedException("창고 보관 상태 업데이트 중 예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
            }

            // 결제 처리
            try {
                paymentCommandService.processPayment(order, buyer, paymentRequest);
            } catch (Exception e) {
                log.error("결제 처리 중 오류 발생: {}", e.getMessage(), e);
                throw new OrderPaymentProcessingFailedException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
            }

            return order;
        } catch (Exception e) {
            if (e instanceof OrderException) {
                throw e;
            }
            log.error("즉시 구매 주문 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderCreationFailedException("즉시 구매 주문 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 주문을 삭제합니다.
     *
     * @param orderId 주문 ID
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        try {
            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("해당 주문을 찾을 수 없습니다(ID: " + orderId + ")"));

            // 주문 삭제
            orderRepository.delete(order);
        } catch (Exception e) {
            if (e instanceof OrderNotFoundException) {
                throw e;
            }
            log.error("주문 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new OrderDeletionFailedException("주문 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 및 배송 요청 정보의 유효성을 검사합니다.
     *
     * @param requestDto 결제 및 배송 요청 정보
     * @throws InvalidPaymentShipmentDataException 결제 및 배송 정보가 유효하지 않은 경우
     */
    private void validatePaymentShipmentRequest(PayAndShipmentRequestDto requestDto) {
        if (requestDto.getPaymentRequest() == null) {
            throw new InvalidPaymentShipmentDataException("결제 정보가 없습니다.");
        }

        if (requestDto.getReceiverName() == null || requestDto.getReceiverName().trim().isEmpty()) {
            throw new InvalidPaymentShipmentDataException("수령인 이름이 없습니다.");
        }

        if (requestDto.getReceiverPhone() == null || requestDto.getReceiverPhone().trim().isEmpty()) {
            throw new InvalidPaymentShipmentDataException("수령인 전화번호가 없습니다.");
        }

        if (requestDto.getPostalCode() == null || requestDto.getPostalCode().trim().isEmpty()) {
            throw new InvalidPaymentShipmentDataException("우편번호가 없습니다.");
        }

        if (requestDto.getAddress() == null || requestDto.getAddress().trim().isEmpty()) {
            throw new InvalidPaymentShipmentDataException("주소가 없습니다.");
        }
    }
}