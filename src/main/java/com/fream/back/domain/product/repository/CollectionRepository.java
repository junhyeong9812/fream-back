package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    Optional<Collection> findByName(String name);

    boolean existsByName(String name);

    List<Collection> findAllByOrderByNameDesc();

    Page<Collection> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}