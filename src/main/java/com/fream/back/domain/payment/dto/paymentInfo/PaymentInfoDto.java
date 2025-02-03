package com.fream.back.domain.payment.dto.paymentInfo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentInfoDto {
    private Long id;
    private String cardNumber;        // 카드 번호
    private String cardPassword;      // 카드 비밀번호 앞 두 자리
    private String expirationDate;    // 유효기간
    private String birthDate;         // 생년월일
}