package com.fream.back.domain.payment.service.query;

import com.fream.back.domain.payment.dto.AccountPaymentDto;
import com.fream.back.domain.payment.dto.CardPaymentDto;
import com.fream.back.domain.payment.dto.GeneralPaymentDto;
import com.fream.back.domain.payment.dto.PaymentDto;
import com.fream.back.domain.payment.entity.AccountPayment;
import com.fream.back.domain.payment.entity.CardPayment;
import com.fream.back.domain.payment.entity.GeneralPayment;
import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.exception.PaymentNotFoundException;
import com.fream.back.domain.payment.exception.UnknownPaymentTypeException;
import com.fream.back.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentDto getPaymentDetails(Long paymentId) {
        try {
            if (paymentId == null) {
                throw new PaymentNotFoundException("결제 ID가 제공되지 않았습니다.");
            }

            log.info("결제 정보 조회 시작: 결제ID={}", paymentId);

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("결제 ID: " + paymentId + "를 찾을 수 없습니다."));

            PaymentDto paymentDto;

            if (payment instanceof GeneralPayment generalPayment) {
                paymentDto = GeneralPaymentDto.builder()
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
                paymentDto = CardPaymentDto.builder()
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
                paymentDto = AccountPaymentDto.builder()
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
                log.error("알 수 없는 결제 유형: 결제ID={}, 결제유형={}",
                        paymentId, payment.getClass().getSimpleName());
                throw new UnknownPaymentTypeException(
                        "알 수 없는 결제 유형입니다: " + payment.getClass().getSimpleName());
            }

            log.info("결제 정보 조회 완료: 결제ID={}, 결제유형={}",
                    paymentId, paymentDto.getPaymentType());
            return paymentDto;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 조회 중 오류 발생: 결제ID={}, 오류={}",
                    paymentId, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다.");
        }
    }
}