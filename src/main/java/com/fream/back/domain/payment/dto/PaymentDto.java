package com.fream.back.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fream.back.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 정보 응답 공통 인터페이스
 * 모든 결제 응답 DTO가 구현해야 하는 기본 메서드 정의
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "paymentType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = AccountPaymentDto.class, name = "ACCOUNT"),
        @JsonSubTypes.Type(value = GeneralPaymentDto.class, name = "GENERAL")
})
public interface PaymentDto {
    /**
     * 결제 ID
     * @return 결제 고유 ID
     */
    Long getId();

    /**
     * 결제 금액
     * @return 결제된 금액
     */
    double getPaidAmount();

    /**
     * 결제 유형
     * @return 결제 유형 (CARD, ACCOUNT, GENERAL)
     */
    String getPaymentType();

    /**
     * 결제 고유 번호 (PG사)
     * @return PG사에서 발급한 결제 고유 ID
     */
    String getImpUid();

    /**
     * 결제 상태
     * @return 현재 결제 상태 (PENDING, PAID, REFUND_REQUESTED, REFUNDED)
     */
    PaymentStatus getStatus();

    /**
     * 결제 완료 시간
     * @return 결제가 완료된 시간
     */
    LocalDateTime getPaymentDate();
}