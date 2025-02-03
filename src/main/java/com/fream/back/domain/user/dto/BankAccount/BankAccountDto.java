package com.fream.back.domain.user.dto.BankAccount;

import lombok.Data;

@Data
public class BankAccountDto {
    private String bankName;       // 은행명
    private String accountNumber;  // 계좌번호
    private String accountHolder;  // 예금주
}