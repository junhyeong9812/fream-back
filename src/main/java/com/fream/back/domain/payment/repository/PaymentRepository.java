package com.fream.back.domain.payment.repository;

import com.fream.back.domain.payment.entity.CardPayment;
import com.fream.back.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder_User_EmailAndIsSuccess(String email, boolean isSuccess);
    List<Payment> findBySale_Seller_EmailAndIsSuccess(String email, boolean isSuccess);
    Optional<CardPayment> findByImpUid(String impUid);
}
