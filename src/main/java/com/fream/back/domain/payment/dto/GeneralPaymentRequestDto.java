package com.fream.back.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class GeneralPaymentRequestDto extends PaymentRequestDto {
    private String impUid; // PortOne 고유 ID
    private String pgProvider; // PG사 이름
    private String receiptUrl; // 영수증 URL
    private String buyerName; // 구매자 이름
    private String buyerEmail; // 구매자 이메일
    // 부모 필드 설정 메서드 추가
    public void setPaymentTypeToGeneral() {
        this.setPaymentType("GENERAL");
    }
}
