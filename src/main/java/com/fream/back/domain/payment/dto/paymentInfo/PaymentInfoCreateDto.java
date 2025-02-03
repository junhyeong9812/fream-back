package com.fream.back.domain.payment.dto.paymentInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // 기본 생성자 추가
public class PaymentInfoCreateDto {
    private String cardNumber;        // 카드 번호
    private String cardPassword;      // 카드 비밀번호 앞 두 자리
    private String expirationDate;    // 유효기간
    private String birthDate;         // 생년월일
}