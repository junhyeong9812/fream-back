package com.fream.back.domain.sale.repository;

import com.fream.back.domain.sale.entity.SaleBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleBidRepository extends JpaRepository<SaleBid, Long>, SaleBidRepositoryCustom  {
    Optional<SaleBid> findById(Long id);
    Optional<SaleBid> findByOrder_Id(Long orderId);
    Optional<SaleBid> findBySale_Id(Long saleId);
}
