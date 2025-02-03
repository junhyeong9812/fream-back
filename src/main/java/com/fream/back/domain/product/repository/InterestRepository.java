package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.Interest;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    boolean existsByUserAndProductColor(User user, ProductColor productColor);

    Optional<Interest> findByUserAndProductColor(User user, ProductColor productColor);

    List<Interest> findAllByUserId(Long userId);
}