package com.fream.back.domain.user.service.bankaccount;

import com.fream.back.domain.user.dto.BankAccount.BankAccountInfoDto;
import com.fream.back.domain.user.entity.BankAccount;
import com.fream.back.domain.user.exception.BankAccountNotFoundException;
import com.fream.back.domain.user.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountQueryService {
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public BankAccountInfoDto getBankAccount(String email) {
        log.info("계좌 정보 조회 시작 - 사용자: {}", email);

        try {
            BankAccount bankAccount = bankAccountRepository.findByUser_Email(email);

            if (bankAccount == null) {
                log.warn("등록된 계좌 정보 없음 - 사용자: {}", email);
                throw new BankAccountNotFoundException(email);
            }

            log.info("계좌 정보 조회 완료 - 사용자: {}", email);
            return new BankAccountInfoDto(
                    bankAccount.getBankName(),
                    bankAccount.getAccountNumber(),
                    bankAccount.getAccountHolder()
            );
        } catch (BankAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("계좌 정보 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new BankAccountNotFoundException(email);
        }
    }
}