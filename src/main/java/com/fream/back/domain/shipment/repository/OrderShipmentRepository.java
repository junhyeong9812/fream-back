package com.fream.back.domain.shipment.repository;

import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderShipmentRepository extends JpaRepository<OrderShipment, Long> {
    List<OrderShipment> findByStatusIn(List<ShipmentStatus> statuses);
}

