package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.ProductColorViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductColorViewLogRepository extends JpaRepository<ProductColorViewLog, Long> {
    // 예: productColorId별 통계나 기간별 검색이 필요하면 커스텀 메서드 추가.
}