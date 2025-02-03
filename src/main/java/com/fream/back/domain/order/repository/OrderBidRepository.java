package com.fream.back.domain.order.repository;

import com.fream.back.domain.order.entity.OrderBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderBidRepository extends JpaRepository<OrderBid, Long>, OrderBidRepositoryCustom  {
    Optional<OrderBid> findByOrderId(Long orderId);
}
