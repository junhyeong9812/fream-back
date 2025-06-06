package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByName(String name);

    boolean existsByName(String name);

    List<Brand> findAllByOrderByNameDesc();

    Page<Brand> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}