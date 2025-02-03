package com.fream.back.domain.payment.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@DiscriminatorValue("GENERAL")
public class GeneralPayment extends Payment {

    private String impUid; // PortOne 고유 ID
    private String pgProvider; // PG사 이름
    private String receiptUrl; // 영수증 URL
    private String buyerName; // 구매자 이름
    private String buyerEmail; // 구매자 이메일


    @Builder
    public GeneralPayment(String impUid, String pgProvider, String receiptUrl,
                          String buyerName, String buyerEmail, String status, double paidAmount) {
        this.impUid = impUid;
        this.pgProvider = pgProvider;
        this.receiptUrl = receiptUrl;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.setPaidAmount(paidAmount);
    }
    @Override
    public String getImpUid() {
        return this.impUid;
    }

}
