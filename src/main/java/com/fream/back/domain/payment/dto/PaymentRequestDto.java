package com.fream.back.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 결제 요청 공통 DTO
 * 모든 결제 요청 타입의 부모 클래스
 */
@Data
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "paymentType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentRequestDto.class, name = "CARD"),
        @JsonSubTypes.Type(value = AccountPaymentRequestDto.class, name = "ACCOUNT"),
        @JsonSubTypes.Type(value = GeneralPaymentRequestDto.class, name = "GENERAL")
})
@AllArgsConstructor
@NoArgsConstructor
public abstract class PaymentRequestDto {

    /**
     * 결제 유형
     * CARD: 카드 결제
     * ACCOUNT: 계좌이체
     * GENERAL: 일반 결제 (PG사 직접 결제)
     */
    @NotBlank(message = "결제 유형은 필수 입력 값입니다.")
    private String paymentType;

    /**
     * 실제 타입 저장
     * 런타임에 결정되는 실제 결제 유형
     */
    @JsonIgnore
    private String resolvedType;

    /**
     * 결제 금액
     * 0보다 커야 함
     */
    @NotNull(message = "결제 금액은 필수 입력 값입니다.")
    @Min(value = 100, message = "결제 금액은 100원 이상이어야 합니다.")
    private double paidAmount;

    /**
     * 주문 ID
     * 결제와 연결된 주문 정보
     */
    @NotNull(message = "주문 ID는 필수 입력 값입니다.")
    private Long orderId;

    /**
     * 사용자 이메일
     * 결제를 요청한 사용자 식별
     */
    @NotBlank(message = "사용자 이메일은 필수 입력 값입니다.")
    private String userEmail;

    /**
     * 결제 유형 검증 및 설정
     * JSON 직렬화/역직렬화 과정에서 실제 타입을 결정하고 저장
     * @return 실제 결제 유형
     */
    @JsonIgnore
    public String getResolvedPaymentType() {
        if (resolvedType == null) {
            if (this instanceof CardPaymentRequestDto) {
                resolvedType = "CARD";
            } else if (this instanceof AccountPaymentRequestDto) {
                resolvedType = "ACCOUNT";
            } else if (this instanceof GeneralPaymentRequestDto) {
                resolvedType = "GENERAL";
            } else {
                resolvedType = paymentType;
            }
        }
        return resolvedType;
    }
}