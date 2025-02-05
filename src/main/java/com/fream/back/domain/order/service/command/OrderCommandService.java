package com.fream.back.domain.order.service.command;

import com.fream.back.domain.address.dto.AddressResponseDto;
import com.fream.back.domain.address.service.query.AddressQueryService;
import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.entity.BidStatus;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderItem;
import com.fream.back.domain.order.entity.OrderStatus;
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
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final PaymentCommandService paymentCommandService;
    private final OrderShipmentCommandService orderShipmentCommandService;
    private final OrderItemCommandService orderItemCommandService;
    private final OrderBidQueryService orderBidQueryService;
    private final UserQueryService userQueryService;
    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final AddressQueryService addressQueryService;
    private final EntityManager entityManager; // EntityManager 주입
    @Transactional
    public Order createOrderFromBid(User user, ProductSize productSize, int bidPrice) {
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
    }
    @Transactional
    public void processPaymentAndShipment(Long orderId, String userEmail, PayAndShipmentRequestDto requestDto) {
        // 1. User 및 Order 조회
        User user = userQueryService.findByEmail(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Order를 찾을 수 없습니다: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("해당 사용자는 이 Order를 처리할 권한이 없습니다.");
        }

        // 2. 결제 처리
        Payment payment = paymentCommandService.processPayment(order, user, requestDto.getPaymentRequest());
        order.assignPayment(payment);

        // 3. 배송 정보 생성
        OrderShipment shipment = orderShipmentCommandService.createOrderShipment(
                order,
                requestDto.getReceiverName(),
                requestDto.getReceiverPhone(),
                requestDto.getPostalCode(),
                requestDto.getAddress()
        );
        order.assignOrderShipment(shipment);

        // 4. 상태 업데이트
        if (requestDto.isWarehouseStorage()) {
            // 창고 보관일 경우
            WarehouseStorage warehouseStorage = warehouseStorageCommandService.createOrderStorage(order, user);
            order.assignWarehouseStorage(warehouseStorage);
            order.updateStatus(OrderStatus.PAYMENT_COMPLETED);
            order.updateStatus(OrderStatus.PREPARING);
            order.updateStatus(OrderStatus.IN_WAREHOUSE);
        } else {
            // 실제 배송일 경우
            order.updateStatus(OrderStatus.PAYMENT_COMPLETED);
            order.updateStatus(OrderStatus.PREPARING);
        }

        // 5. OrderBid 상태 업데이트
        orderBidQueryService.findById(orderId).ifPresent(orderBid -> {
            orderBid.updateStatus(BidStatus.MATCHED);
        });
    }

    @Transactional
    public Order createInstantOrder(User buyer, SaleBid saleBid, Long addressId,
                                    boolean isWarehouseStorage, PaymentRequestDto paymentRequest) {
        // 1. 주소 조회
        AddressResponseDto address = addressQueryService.getAddress(buyer.getEmail(), addressId);

        // 2. Order 생성
        Order order = Order.builder()
                .user(buyer)
                .totalAmount(saleBid.getBidPrice())
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        order = orderRepository.save(order);

        // **PaymentRequest에 OrderId 설정**
        paymentRequest.setOrderId(order.getId());
        System.out.println("paymentRequest = " + paymentRequest);
        // 3. OrderItem 추가
        OrderItem orderItem = orderItemCommandService.createOrderItem(order, saleBid.getProductSize(), saleBid.getBidPrice());
        order.addOrderItem(orderItem);

        // 4. 배송 정보 생성 및 연관 설정
        OrderShipment orderShipment = orderShipmentCommandService.createOrderShipment(
                order,
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getZipCode(),
                address.getAddress()
        );
        order.assignOrderShipment(orderShipment);


        saleBid.assignOrder(order);
        saleBid.updateStatus(com.fream.back.domain.sale.entity.BidStatus.MATCHED);
        // **변경 내용 강제 반영**
        entityManager.flush();

        // Sale의 isWarehouseStorage 확인 후 상태 변경
        Sale sale = saleBid.getSale();
        if (sale != null && sale.isWarehouseStorage()) {
            warehouseStorageCommandService.updateWarehouseStatus(sale, WarehouseStatus.ASSOCIATED_WITH_ORDER);
        }


        paymentCommandService.processPayment(order, buyer, paymentRequest);

        return order;
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        // Order 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Order를 찾을 수 없습니다: " + orderId));

        // Order 삭제
        orderRepository.delete(order);
    }



}
