package com.fream.back.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 계좌이체 결제 요청 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class AccountPaymentRequestDto extends PaymentRequestDto {

    /**
     * 은행명
     * Bank enum에 정의된 값 중 하나여야 함
     */
    @NotBlank(message = "은행명은 필수 입력 값입니다.")
    private String bankName;

    /**
     * 계좌번호
     * 숫자와 하이픈(-)만 허용
     */
    @NotBlank(message = "계좌번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^[0-9\\-]+$", message = "계좌번호는 숫자와 하이픈(-)만 포함할 수 있습니다.")
    private String accountNumber;

    /**
     * 예금주
     */
    @NotBlank(message = "예금주는 필수 입력 값입니다.")
    private String accountHolder;

    /**
     * 현금영수증 요청 여부
     */
    private boolean receiptRequested;

    /**
     * 기본 생성자
     * 슈퍼 클래스 paymentType 필드 초기화
     */
    public AccountPaymentRequestDto(String bankName, String accountNumber, String accountHolder, boolean receiptRequested) {
        super("ACCOUNT", "ACCOUNT", 0, null, null);
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.receiptRequested = receiptRequested;
    }

    /**
     * 모든 필드 초기화 생성자
     */
    public AccountPaymentRequestDto(String paymentType, String resolvedType, double paidAmount,
                                    Long orderId, String userEmail, String bankName,
                                    String accountNumber, String accountHolder, boolean receiptRequested) {
        super(paymentType, resolvedType, paidAmount, orderId, userEmail);
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.receiptRequested = receiptRequested;
    }
}