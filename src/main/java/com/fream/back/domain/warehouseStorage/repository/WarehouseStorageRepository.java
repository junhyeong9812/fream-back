package com.fream.back.domain.warehouseStorage.repository;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseStorageRepository extends JpaRepository<WarehouseStorage, Long> {
    Optional<WarehouseStorage> findBySale(Sale sale);
    List<WarehouseStorage> findByUser_Email(String email);
}
