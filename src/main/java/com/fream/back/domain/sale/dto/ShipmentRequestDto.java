package com.fream.back.domain.sale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentRequestDto {
    private String courier; // 택배사
    private String trackingNumber; // 운송장 번호
}
