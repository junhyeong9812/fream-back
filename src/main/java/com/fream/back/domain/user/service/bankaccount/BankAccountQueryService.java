package com.fream.back.domain.user.service.bankaccount;


import com.fream.back.domain.user.dto.BankAccount.BankAccountInfoDto;
import com.fream.back.domain.user.entity.BankAccount;
import com.fream.back.domain.user.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankAccountQueryService {
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public BankAccountInfoDto getBankAccount(String email) {
        BankAccount bankAccount = bankAccountRepository.findByUser_Email(email);

        if (bankAccount == null) {
            throw new IllegalArgumentException("등록된 계좌가 없습니다.");
        }

        return new BankAccountInfoDto(
                bankAccount.getBankName(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountHolder()
        );
    }
}