package com.fream.back.domain.payment.dto;

import com.fream.back.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public interface PaymentDto {
    Long getId();
    double getPaidAmount();
    String getPaymentType();
    String getImpUid();
    PaymentStatus getStatus();
    LocalDateTime getPaymentDate();
}
