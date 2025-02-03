package com.fream.back.domain.shipment.dto;

import lombok.Data;

@Data
public class TrackingNumberRequestDto {
    private String courier;         // 배송사 이름
    private String trackingNumber;  // 송장 번호
}
