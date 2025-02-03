package com.fream.back.domain.payment.entity;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.user.entity.User;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale; // 판매와 연결

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double paidAmount; // 결제 금액
    @Column(nullable = false)
    private boolean isSuccess = false; // 기본값 false

    private LocalDateTime paymentDate; // 결제 완료 시간

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING; // 기본값 PENDING


    public void assignOrder(Order order) {
        this.order = order;
        this.sale = null; // 서로 배타적 관계
    }

    public void assignSale(Sale sale) {
        this.sale = sale;
        this.order = null; // 서로 배타적 관계
    }
    public void assignUser(User user) {
        this.user = user;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
        this.isSuccess = paidAmount > 0; // 결제 금액이 양수면 성공으로 간주
    }
    public void updateSuccessStatus(boolean success) {
        this.isSuccess = success;
        if (success) {
            this.paymentDate = LocalDateTime.now(); // 성공 시 결제 완료 시간 갱신
        }
    }
    public void updateStatus(PaymentStatus newStatus) {
        if (this.status.canTransitionTo(newStatus)) {
            this.status = newStatus;
        } else {
            throw new IllegalStateException("결제 상태 전환이 허용되지 않습니다: " + this.status + " -> " + newStatus);
        }
    }
    // 하위 클래스에서 구현해야 하는 메서드
    public abstract String getImpUid();
}
