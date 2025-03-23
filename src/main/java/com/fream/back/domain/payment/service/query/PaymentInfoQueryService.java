package com.fream.back.domain.payment.service.query;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoDto;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.payment.exception.PaymentAccessDeniedException;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.exception.PaymentInfoNotFoundException;
import com.fream.back.domain.payment.repository.PaymentInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentInfoQueryService {

    private final PaymentInfoRepository paymentInfoRepository;

    @Transactional(readOnly = true)
    public List<PaymentInfoDto> getPaymentInfos(String email) {
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_ACCESS_DENIED,
                        "사용자 이메일 정보가 없습니다.");
            }

            log.info("결제 정보 목록 조회 시작: 사용자={}", email);

            List<PaymentInfo> paymentInfos = paymentInfoRepository.findAllByUser_Email(email);

            List<PaymentInfoDto> result = paymentInfos.stream()
                    .map(pi -> new PaymentInfoDto(
                            pi.getId(),
                            maskCardNumber(pi.getCardNumber()),
                            maskCardPassword(pi.getCardPassword()),
                            pi.getExpirationDate(),
                            maskBirthDate(pi.getBirthDate())))
                    .toList();

            log.info("결제 정보 목록 조회 완료: 사용자={}, 조회된 결제정보 수={}", email, result.size());
            return result;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 목록 조회 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 목록 조회 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public PaymentInfoDto getPaymentInfo(String email, Long id) {
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_ACCESS_DENIED,
                        "사용자 이메일 정보가 없습니다.");
            }

            if (id == null) {
                throw new PaymentInfoNotFoundException("조회할 결제 정보 ID가 없습니다.");
            }

            log.info("단일 결제 정보 조회 시작: 사용자={}, 결제정보ID={}", email, id);

            PaymentInfo paymentInfo = paymentInfoRepository.findByIdAndUser_Email(id, email)
                    .orElseThrow(() -> new PaymentInfoNotFoundException(
                            "결제 정보를 찾을 수 없습니다. ID: " + id));

            PaymentInfoDto dto = new PaymentInfoDto(
                    paymentInfo.getId(),
                    maskCardNumber(paymentInfo.getCardNumber()),
                    maskCardPassword(paymentInfo.getCardPassword()),
                    paymentInfo.getExpirationDate(),
                    maskBirthDate(paymentInfo.getBirthDate())
            );

            log.info("단일 결제 정보 조회 완료: 사용자={}, 결제정보ID={}", email, id);
            return dto;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("단일 결제 정보 조회 중 오류 발생: 사용자={}, 결제정보ID={}, 오류={}",
                    email, id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public PaymentInfo getPaymentInfoEntity(String email, Long id) {
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_ACCESS_DENIED,
                        "사용자 이메일 정보가 없습니다.");
            }

            if (id == null) {
                throw new PaymentInfoNotFoundException("조회할 결제 정보 ID가 없습니다.");
            }

            log.info("결제 정보 엔티티 조회 시작: 사용자={}, 결제정보ID={}", email, id);

            PaymentInfo paymentInfo = paymentInfoRepository.findByIdAndUser_Email(id, email)
                    .orElseThrow(() -> new PaymentInfoNotFoundException(
                            "결제 정보를 찾을 수 없습니다. ID: " + id));

            log.info("결제 정보 엔티티 조회 완료: 사용자={}, 결제정보ID={}", email, id);
            return paymentInfo;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 엔티티 조회 중 오류 발생: 사용자={}, 결제정보ID={}, 오류={}",
                    email, id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다.");
        }
    }

    // 카드번호 마스킹 메서드
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "Invalid";
        }
        return cardNumber.substring(0, 4) + "********" +
                (cardNumber.length() > 12 ? cardNumber.substring(cardNumber.length() - 4) : "");
    }

    // 카드 비밀번호 마스킹 메서드
    private String maskCardPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "**";
        }
        return "**";
    }

    // 생년월일 마스킹 메서드
    private String maskBirthDate(String birthDate) {
        if (birthDate == null || birthDate.length() < 6) {
            return "******";
        }
        return "**" + birthDate.substring(2, 4) + "**";
    }
}