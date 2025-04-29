package com.fream.back.domain.order.entity;

import com.fream.back.domain.order.exception.InvalidOrderStatusException;
import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 주문자

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment; // 연관된 결제 정보

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderShipment orderShipment; // 구매자 배송 정보

    private int totalAmount; // 총 금액
    private int discountAmount; // 할인 금액
    private int usedPoints; // 사용된 포인트

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문 상태 (결제대기, 결제완료 등)

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private WarehouseStorage warehouseStorage; // 창고 보관 정보

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderBid orderBid;

    /**
     * 주문 항목을 추가합니다.
     *
     * @param orderItem 주문 항목
     */
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.addOrder(this);
    }

    /**
     * Payment와의 양방향 관계를 설정합니다.
     *
     * @param payment 결제 정보
     */
    public void assignPayment(Payment payment) {
        this.payment = payment;
        if (payment != null && payment.getOrder() != this) {
            payment.assignOrder(this);
        }
    }

    /**
     * OrderShipment와의 양방향 관계를 설정합니다.
     *
     * @param orderShipment 배송 정보
     */
    public void assignOrderShipment(OrderShipment orderShipment) {
        this.orderShipment = orderShipment;
        if (orderShipment != null && orderShipment.getOrder() != this) {
            orderShipment.assignOrder(this);
        }
    }

    /**
     * WarehouseStorage와의 양방향 관계를 설정합니다.
     *
     * @param warehouseStorage 창고 보관 정보
     */
    public void assignWarehouseStorage(WarehouseStorage warehouseStorage) {
        this.warehouseStorage = warehouseStorage;
        if (warehouseStorage != null && warehouseStorage.getOrder() != this) {
            warehouseStorage.assignOrder(this);
        }
    }

    /**
     * 주문 상태를 업데이트합니다.
     * 상태 전이 규칙에 따라 유효한 경우에만 상태가 변경됩니다.
     *
     * @param newStatus 새 주문 상태
     * @throws InvalidOrderStatusException 유효하지 않은 상태 전이인 경우
     */
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == null || this.status.canTransitionTo(newStatus)) {
            this.status = newStatus;
        } else {
            throw new InvalidOrderStatusException(
                    String.format("상태 전환이 허용되지 않습니다: %s -> %s", this.status, newStatus)
            );
        }
    }

    /**
     * OrderBid와의 양방향 관계를 설정합니다.
     *
     * @param orderBid 주문 입찰
     */
    public void assignOrderBid(OrderBid orderBid) {
        this.orderBid = orderBid;
        if (orderBid != null && orderBid.getOrder() != this) {
            orderBid.assignOrder(this);
        }
    }
}