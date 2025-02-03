package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.ProductDetailImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDetailImageRepository extends JpaRepository<ProductDetailImage, Long> {
    // 특정 ProductColor에 속한 상세 이미지가 존재하는지 확인
    boolean existsByProductColorId(Long productColorId);
    List<ProductDetailImage> findByProductColorId(Long productColorId);
    List<ProductDetailImage> findAllByProductColorId(Long productColorId);
}
