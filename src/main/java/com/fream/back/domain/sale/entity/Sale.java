package com.fream.back.domain.sale.entity;

import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.shipment.entity.SellerShipment;
import com.fream.back.domain.user.entity.User;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale  extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller; // 판매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize; // 판매 대상 상품 사이즈

    private String returnAddress; // 반송 주소
    private String postalCode; // 반송 우편번호
    private String receiverPhone; // 수령인 전화번호

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment; // 판매 관련 결제 정보

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private SaleBankAccount saleBankAccount;

    private boolean isWarehouseStorage; // 창고 보관 여부

    @Enumerated(EnumType.STRING)
    private SaleStatus status; // 판매 상태

    @Column(length = 36)
    private String matchId; // trade 매칭 식별자(UUID) — BidMatchedEvent로 생성된 거래의 양면 연결 키

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private SaleBid saleBid; // Sale와 연결된 SaleBid

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerShipment sellerShipment;

    public void assignSaleBankAccount(SaleBankAccount saleBankAccount) {
        this.saleBankAccount = saleBankAccount;
        saleBankAccount.assignSale(this);
    }

    public void assignSaleBid(SaleBid saleBid) {
        this.saleBid = saleBid;
        saleBid.assignSale(this);
    }

    public void assignPayment(Payment payment) {
        this.payment = payment;
        payment.assignSale(this);
    }
    public void assignSellerShipment(SellerShipment sellerShipment) {
        this.sellerShipment = sellerShipment;
        sellerShipment.assignSale(this); // 연관관계 설정
    }

    public void updateStatus(SaleStatus newStatus) {
        this.status = newStatus;
    }
}
