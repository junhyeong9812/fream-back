package com.fream.back.domain.payment.service.query;

import com.fream.back.domain.payment.dto.AccountPaymentDto;
import com.fream.back.domain.payment.dto.CardPaymentDto;
import com.fream.back.domain.payment.dto.GeneralPaymentDto;
import com.fream.back.domain.payment.dto.PaymentDto;
import com.fream.back.domain.payment.entity.AccountPayment;
import com.fream.back.domain.payment.entity.CardPayment;
import com.fream.back.domain.payment.entity.GeneralPayment;
import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentDto getPaymentDetails(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        if (payment instanceof GeneralPayment generalPayment) {
            return GeneralPaymentDto.builder()
                    .id(generalPayment.getId())
                    .paidAmount(generalPayment.getPaidAmount())
                    .paymentType("GENERAL")
                    .impUid(generalPayment.getImpUid())
                    .status(generalPayment.getStatus())
                    .paymentDate(generalPayment.getPaymentDate())
                    .pgProvider(generalPayment.getPgProvider())
                    .receiptUrl(generalPayment.getReceiptUrl())
                    .buyerName(generalPayment.getBuyerName())
                    .buyerEmail(generalPayment.getBuyerEmail())
                    .build();
        } else if (payment instanceof CardPayment cardPayment) {
            return CardPaymentDto.builder()
                    .id(cardPayment.getId())
                    .paidAmount(cardPayment.getPaidAmount())
                    .paymentType("CARD")
                    .impUid(cardPayment.getImpUid())
                    .status(cardPayment.getStatus())
                    .paymentDate(cardPayment.getPaymentDate())
                    .cardType(cardPayment.getCardType())
                    .receiptUrl(cardPayment.getReceiptUrl())
                    .pgProvider(cardPayment.getPgProvider())
                    .pgTid(cardPayment.getPgTid())
                    .build();
        } else if (payment instanceof AccountPayment accountPayment) {
            return AccountPaymentDto.builder()
                    .id(accountPayment.getId())
                    .paidAmount(accountPayment.getPaidAmount())
                    .paymentType("ACCOUNT")
                    .impUid(accountPayment.getImpUid())
                    .status(accountPayment.getStatus())
                    .paymentDate(accountPayment.getPaymentDate())
                    .bankName(accountPayment.getBankName())
                    .accountNumber(accountPayment.getAccountNumber())
                    .accountHolder(accountPayment.getAccountHolder())
                    .receiptRequested(accountPayment.isReceiptRequested())
                    .build();
        } else {
            throw new IllegalArgumentException("알 수 없는 결제 타입입니다.");
        }
    }
}

