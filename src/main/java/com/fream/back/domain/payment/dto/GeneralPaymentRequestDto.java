package com.fream.back.domain.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 일반 결제 요청 DTO
 * 외부 PG사를 통해 이미 진행된 결제의 정보를 전달받을 때 사용
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class GeneralPaymentRequestDto extends PaymentRequestDto {

    /**
     * PortOne 결제 고유 ID
     */
    @NotBlank(message = "결제 고유 ID(impUid)는 필수 입력 값입니다.")
    private String impUid;

    /**
     * PG사 이름 (예: nice, kakaopay, tosspay 등)
     */
    private String pgProvider;

    /**
     * 영수증 URL
     */
    private String receiptUrl;

    /**
     * 구매자 이름
     */
    private String buyerName;

    /**
     * 구매자 이메일
     */
    @Email(message = "유효한 이메일 형식이어야 합니다.")
    private String buyerEmail;

    /**
     * 기본 생성자
     * 슈퍼 클래스 paymentType 필드 초기화
     */
    public GeneralPaymentRequestDto(String impUid, String pgProvider, String receiptUrl, String buyerName, String buyerEmail) {
        super("GENERAL", "GENERAL", 0, null, null);
        this.impUid = impUid;
        this.pgProvider = pgProvider;
        this.receiptUrl = receiptUrl;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
    }

    /**
     * 모든 필드 초기화 생성자
     */
    public GeneralPaymentRequestDto(String paymentType, String resolvedType, double paidAmount,
                                    Long orderId, String userEmail, String impUid, String pgProvider,
                                    String receiptUrl, String buyerName, String buyerEmail) {
        super(paymentType, resolvedType, paidAmount, orderId, userEmail);
        this.impUid = impUid;
        this.pgProvider = pgProvider;
        this.receiptUrl = receiptUrl;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
    }

    /**
     * 타입 설정 편의 메서드
     */
    public void setPaymentTypeToGeneral() {
        this.setPaymentType("GENERAL");
        this.setResolvedType("GENERAL");
    }
}