package com.fream.back.domain.payment.dto.paymentInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.payment.util.PaymentCardUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 결제 정보 응답 DTO
 * 카드 정보 조회 시 사용
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInfoDto {

    /**
     * 결제 정보 ID
     */
    private Long id;

    /**
     * 마스킹 처리된 카드 번호
     * 앞 4자리와 뒤 4자리만 노출
     */
    private String cardNumber;

    /**
     * 마스킹 처리된 카드 비밀번호
     * 항상 "**"로 표시
     */
    private String cardPassword;

    /**
     * 카드 유효기간 (MM/YY 형식)
     */
    private String expirationDate;

    /**
     * 마스킹 처리된 생년월일
     * 가운데 2자리만 노출, 앞뒤 2자리는 "*"로 처리
     */
    private String birthDate;

    /**
     * 카드 유형 (예: Visa, MasterCard)
     * 자동 감지 또는 결제 처리 후 설정되는 값
     */
    private String cardType;

    /**
     * 엔티티로부터 DTO 객체 생성
     * 민감 정보는 마스킹 처리
     * @param entity PaymentInfo 엔티티
     * @return 마스킹 처리된 PaymentInfoDto
     */
    @JsonIgnore
    public static PaymentInfoDto fromEntity(PaymentInfo entity) {
        if (entity == null) {
            return null;
        }

        return PaymentInfoDto.builder()
                .id(entity.getId())
                .cardNumber(PaymentCardUtils.maskCardNumber(entity.getCardNumber()))
                .cardPassword(PaymentCardUtils.maskCardPassword(entity.getCardPassword()))
                .expirationDate(entity.getExpirationDate())
                .birthDate(PaymentCardUtils.maskBirthDate(entity.getBirthDate()))
                .build();
    }
}