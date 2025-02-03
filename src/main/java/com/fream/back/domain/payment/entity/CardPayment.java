package com.fream.back.domain.payment.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {

    private String cardNumber;        // 카드 번호
    private String cardPassword;      // 카드 비밀번호 앞 두 자리
    private String cardExpiration;    // 카드 유효기간
    private String birthDate;         // 생년월일
    private String cardType;          // 카드 타입 (e.g., Visa, MasterCard)
    private String impUid;            // 포트원 거래 고유 번호
    private String receiptUrl;        // 영수증 URL
    private String pgProvider;        // PG사 이름
    private String pgTid;             // PG사 거래 ID

    @Builder
    public CardPayment(String cardNumber, String cardPassword, String cardExpiration, String birthDate,
                       String cardType, double paidAmount, String impUid, String receiptUrl,
                       String pgProvider, String pgTid) {
        this.cardNumber = cardNumber;
        this.cardPassword = cardPassword;
        this.cardExpiration = cardExpiration;
        this.birthDate = birthDate;
        this.cardType = cardType;
        this.impUid = impUid;
        this.receiptUrl = receiptUrl;
        this.pgProvider = pgProvider;
        this.pgTid = pgTid;
        this.setPaidAmount(paidAmount);
    }
    @Override
    public String getImpUid() {
        return this.impUid;
    }
}
