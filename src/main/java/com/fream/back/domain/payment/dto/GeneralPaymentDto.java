package com.fream.back.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fream.back.domain.payment.entity.GeneralPayment;
import com.fream.back.domain.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 일반 결제 응답 DTO
 * 일반(PG 직접) 결제 정보 조회 시 사용
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralPaymentDto implements PaymentDto {
    private Long id;
    private double paidAmount;
    private String paymentType;
    private String impUid;
    private PaymentStatus status;
    private LocalDateTime paymentDate;

    // 일반 결제 고유 필드
    private String pgProvider;    // PG사 이름
    private String receiptUrl;    // 영수증 URL
    private String buyerName;     // 구매자 이름
    private String buyerEmail;    // 구매자 이메일

    /**
     * 엔티티로부터 DTO 객체 생성
     * @param entity GeneralPayment 엔티티
     * @return GeneralPaymentDto 객체
     */
    @JsonIgnore
    public static GeneralPaymentDto fromEntity(GeneralPayment entity) {
        if (entity == null) {
            return null;
        }

        return GeneralPaymentDto.builder()
                .id(entity.getId())
                .paidAmount(entity.getPaidAmount())
                .paymentType("GENERAL")
                .impUid(entity.getImpUid())
                .status(entity.getStatus())
                .paymentDate(entity.getPaymentDate())
                .pgProvider(entity.getPgProvider())
                .receiptUrl(entity.getReceiptUrl())
                .buyerName(entity.getBuyerName())
                .buyerEmail(entity.getBuyerEmail())
                .build();
    }
}