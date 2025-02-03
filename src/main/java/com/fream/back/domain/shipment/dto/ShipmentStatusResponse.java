package com.fream.back.domain.shipment.dto;

import com.fream.back.domain.shipment.entity.ShipmentStatus;

public class ShipmentStatusResponse {
    private ShipmentStatus status;
    public ShipmentStatusResponse(ShipmentStatus status) {
        this.status = status;
    }
    public ShipmentStatus getStatus() { return status; }
}
