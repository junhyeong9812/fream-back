package com.fream.back.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AccountPaymentRequestDto extends PaymentRequestDto {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private boolean receiptRequested; // 현금영수증 요청 여부
}

