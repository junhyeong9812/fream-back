package com.fream.back.domain.payment.repository;

import com.fream.back.domain.payment.entity.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {
    List<PaymentInfo> findAllByUser_Email(String email); // 사용자 이메일로 결제 정보 조회
    Optional<PaymentInfo> findByIdAndUser_Email(Long id, String email); // 특정 결제 정보 조회
}
