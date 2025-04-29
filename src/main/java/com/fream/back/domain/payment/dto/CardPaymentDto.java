package com.fream.back.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fream.back.domain.payment.entity.CardPayment;
import com.fream.back.domain.payment.entity.PaymentStatus;
import com.fream.back.domain.payment.util.PaymentCardUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카드 결제 응답 DTO
 * 카드 결제 정보 조회 시 사용
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardPaymentDto implements PaymentDto {
    private Long id;
    private double paidAmount;
    private String paymentType;
    private String impUid;
    private PaymentStatus status;
    private LocalDateTime paymentDate;

    // 카드 결제 고유 필드
    private String cardType;        // 카드 종류 (예: Visa, MasterCard)
    private String receiptUrl;      // 영수증 URL
    private String pgProvider;      // PG사 이름
    private String pgTid;           // PG사 거래 ID

    /**
     * 마스킹된 카드 번호 (선택적)
     */
    private String maskedCardNumber;

    /**
     * 엔티티로부터 DTO 객체 생성
     * @param entity CardPayment 엔티티
     * @return CardPaymentDto 객체
     */
    @JsonIgnore
    public static CardPaymentDto fromEntity(CardPayment entity) {
        if (entity == null) {
            return null;
        }

        return CardPaymentDto.builder()
                .id(entity.getId())
                .paidAmount(entity.getPaidAmount())
                .paymentType("CARD")
                .impUid(entity.getImpUid())
                .status(entity.getStatus())
                .paymentDate(entity.getPaymentDate())
                .cardType(entity.getCardType())
                .receiptUrl(entity.getReceiptUrl())
                .pgProvider(entity.getPgProvider())
                .pgTid(entity.getPgTid())
                .maskedCardNumber(entity.getCardNumber() != null ?
                        PaymentCardUtils.maskCardNumber(entity.getCardNumber()) : null)
                .build();
    }
}