package com.fream.back.domain.payment.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@DiscriminatorValue("ACCOUNT")
public class AccountPayment extends Payment {

    private String impUid;            // 포트원 거래 고유 번호
    private String bankName; // 은행 이름
    private String accountNumber; // 계좌 번호
    private String accountHolder; // 예금주
    private boolean receiptRequested; // 현금영수증 요청 여부

    @Builder
    public AccountPayment(String bankName, String accountNumber, String accountHolder,
                          boolean receiptRequested, double paidAmount) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.receiptRequested = receiptRequested;
        this.setPaidAmount(paidAmount);
    }
    @Override
    public String getImpUid() {
        return this.impUid;
    }

}