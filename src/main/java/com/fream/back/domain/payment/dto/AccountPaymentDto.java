package com.fream.back.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fream.back.domain.payment.entity.AccountPayment;
import com.fream.back.domain.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 계좌이체 결제 응답 DTO
 * 계좌이체 결제 정보 조회 시 사용
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountPaymentDto implements PaymentDto {
    private Long id;
    private double paidAmount;
    private String paymentType;
    private String impUid;
    private PaymentStatus status;
    private LocalDateTime paymentDate;

    // 계좌이체 고유 필드
    private String bankName;        // 은행명
    private String accountNumber;   // 계좌번호
    private String accountHolder;   // 예금주
    private boolean receiptRequested; // 현금영수증 요청 여부

    /**
     * 마스킹된 계좌번호
     * @return 마스킹 처리된 계좌번호
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 6) {
            return "계좌번호 오류";
        }

        // 계좌번호 앞 3자리와 뒤 2자리만 표시, 나머지 '*'로 마스킹
        int length = accountNumber.length();
        return accountNumber.substring(0, 3) +
                "*".repeat(length - 5) +
                accountNumber.substring(length - 2);
    }

    /**
     * 엔티티로부터 DTO 객체 생성
     * @param entity AccountPayment 엔티티
     * @return AccountPaymentDto 객체
     */
    @JsonIgnore
    public static AccountPaymentDto fromEntity(AccountPayment entity) {
        if (entity == null) {
            return null;
        }

        return AccountPaymentDto.builder()
                .id(entity.getId())
                .paidAmount(entity.getPaidAmount())
                .paymentType("ACCOUNT")
                .impUid(entity.getImpUid())
                .status(entity.getStatus())
                .paymentDate(entity.getPaymentDate())
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .accountHolder(entity.getAccountHolder())
                .receiptRequested(entity.isReceiptRequested())
                .build();
    }
}