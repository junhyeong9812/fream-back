package com.fream.back.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "paymentType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentRequestDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = AccountPaymentRequestDto.class, name = "ACCOUNT"),
        @JsonSubTypes.Type(value = GeneralPaymentRequestDto.class, name = "GENERAL")
})
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDto {
    private String paymentType; // CARD, ACCOUNT, GENERAL
    private String resolvedType; // 실제 타입 저장
    private double paidAmount;
    private Long orderId; // 주문 ID
    private String userEmail; // 사용자 이메일
}
