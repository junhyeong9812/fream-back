package com.fream.back.domain.sale.repository;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findBySeller_EmailAndStatus(String email, SaleStatus status);
}
