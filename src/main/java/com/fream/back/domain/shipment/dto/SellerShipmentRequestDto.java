package com.fream.back.domain.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerShipmentRequestDto {
    private Long saleId;            // Sale ID
    private String courier;         // 택배사 이름
    private String trackingNumber;  // 송장 번호
}
