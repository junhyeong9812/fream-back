package com.fream.back.domain.user.service.bankaccount;

import com.fream.back.domain.user.dto.BankAccount.BankAccountDto;
import com.fream.back.domain.user.entity.BankAccount;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.BankAccountNotFoundException;
import com.fream.back.domain.user.exception.InvalidBankAccountException;
import com.fream.back.domain.user.exception.UserNotFoundException;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountCommandService {
    private final UserRepository userRepository;

    @Transactional
    public void createOrUpdateBankAccount(String email, BankAccountDto dto) {
        log.info("계좌 정보 등록/수정 시작 - 사용자: {}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));

            if (user.getBankAccount() == null) {
                // 계좌 정보 생성
                log.debug("새 계좌 정보 생성 - 사용자: {}, 은행: {}", email, dto.getBankName());

                BankAccount bankAccount = BankAccount.builder()
                        .user(user)
                        .bankName(dto.getBankName())
                        .accountNumber(dto.getAccountNumber())
                        .accountHolder(dto.getAccountHolder())
                        .build();
                user.assignBankAccount(bankAccount);
            } else {
                // 계좌 정보 수정
                log.debug("기존 계좌 정보 수정 - 사용자: {}, 은행: {}", email, dto.getBankName());

                user.getBankAccount().updateBankAccount(
                        dto.getBankName(),
                        dto.getAccountNumber(),
                        dto.getAccountHolder()
                );
            }

            log.info("계좌 정보 등록/수정 완료 - 사용자: {}", email);
        } catch (UserNotFoundException e) {
            log.warn("계좌 정보 등록/수정 실패 - 사용자 없음: {}", email);
            throw e;
        } catch (Exception e) {
            log.error("계좌 정보 등록/수정 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new InvalidBankAccountException(dto.getBankName(), dto.getAccountNumber());
        }
    }

    @Transactional
    public void deleteBankAccount(String email) {
        log.info("계좌 정보 삭제 시작 - 사용자: {}", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));

            if (user.getBankAccount() != null) {
                log.debug("계좌 정보 삭제 진행 - 사용자: {}", email);
                user.removeBankAccount();
                log.info("계좌 정보 삭제 완료 - 사용자: {}", email);
            } else {
                log.warn("삭제할 계좌 정보 없음 - 사용자: {}", email);
                throw new BankAccountNotFoundException(email);
            }
        } catch (UserNotFoundException | BankAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("계좌 정보 삭제 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new BankAccountNotFoundException(email);
        }
    }
}