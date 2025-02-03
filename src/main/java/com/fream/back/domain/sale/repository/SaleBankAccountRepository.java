package com.fream.back.domain.sale.repository;

import com.fream.back.domain.sale.entity.SaleBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleBankAccountRepository extends JpaRepository<SaleBankAccount, Long> {
}
