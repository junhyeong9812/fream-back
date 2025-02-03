package com.fream.back.domain.shipment.dto;

import com.fream.back.domain.shipment.entity.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentUpdateRequestDto {
    private String trackingNumber; // 운송장 번호 (등록 시 사용)
    private String courierCompany; // 택배사 이름 (등록 시 사용)
    private ShipmentStatus shipmentStatus; // 변경할 배송 상태
}