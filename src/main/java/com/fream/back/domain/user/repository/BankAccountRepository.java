package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    BankAccount findByUser_Email(String email); // 사용자 이메일로 계좌 정보 조회
}
