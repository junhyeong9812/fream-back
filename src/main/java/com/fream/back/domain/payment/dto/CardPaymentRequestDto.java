package com.fream.back.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 카드 결제 요청 DTO
 * 등록된 카드 정보를 사용한 결제 요청
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class CardPaymentRequestDto extends PaymentRequestDto {

    /**
     * 결제에 사용할 저장된 카드 정보 ID
     */
    @NotNull(message = "결제 정보 ID는 필수 입력 값입니다.")
    private Long paymentInfoId;

    /**
     * 생성자
     * 슈퍼 클래스 paymentType 필드 초기화
     */
    public CardPaymentRequestDto(Long paymentInfoId) {
        super("CARD", "CARD", 0, null, null);
        this.paymentInfoId = paymentInfoId;
    }

    /**
     * 모든 필드 초기화 생성자
     */
    public CardPaymentRequestDto(String paymentType, String resolvedType, double paidAmount,
                                 Long orderId, String userEmail, Long paymentInfoId) {
        super(paymentType, resolvedType, paidAmount, orderId, userEmail);
        this.paymentInfoId = paymentInfoId;
    }
}