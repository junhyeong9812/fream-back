package com.fream.back.domain.order.repository;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderStatus;
import com.fream.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_EmailAndStatus(String email, OrderStatus status);
    List<Order> findByUserAndStatus(User user, OrderStatus status);
}
