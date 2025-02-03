package com.fream.back.domain.shipment.entity;

import com.fream.back.domain.sale.entity.Sale;
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
public class SellerShipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    private String courier; // 택배사 이름
    private String trackingNumber; // 송장 번호

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status; // 배송 상태 (예: 배송 준비 중, 배송 중, 배송 완료 등)

    public void assignSale(Sale sale) {
        this.sale = sale;
    }

    public void updateTrackingInfo(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.status = ShipmentStatus.IN_TRANSIT; // 배송 중으로 상태 업데이트
    }

    public void updateStatus(ShipmentStatus newStatus) {
        if (this.status == null || this.status.canTransitionTo(newStatus)) {
            this.status = newStatus;
        } else {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + newStatus
            );
        }
    }
}
