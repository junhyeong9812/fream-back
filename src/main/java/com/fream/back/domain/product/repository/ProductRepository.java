package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.entity.Collection;
import com.fream.back.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 연관된 Product 존재 여부 확인
    boolean existsByBrand(Brand brand);
    boolean existsByCategory(Category category); // 새로운 메서드 추가
    boolean existsByCollection(Collection collection);
}