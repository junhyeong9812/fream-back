package com.fream.back.domain.sale.service.command;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBankAccount;
import com.fream.back.domain.sale.repository.SaleBankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleBankAccountCommandService {

    private final SaleBankAccountRepository saleBankAccountRepository;

    @Transactional
    public SaleBankAccount createSaleBankAccount(String bankName, String accountNumber,
                                                 String accountHolder, Sale sale) {
        SaleBankAccount bankAccount = new SaleBankAccount(bankName, accountNumber, accountHolder, sale);
        return saleBankAccountRepository.save(bankAccount);
    }
}
