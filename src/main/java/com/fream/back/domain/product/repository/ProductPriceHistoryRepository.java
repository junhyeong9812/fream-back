package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
    List<ProductPriceHistory> findByProductSizeId(Long productSizeId);
}