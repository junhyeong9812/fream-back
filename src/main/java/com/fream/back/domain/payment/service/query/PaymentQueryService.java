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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 결제 조회 서비스
 * 결제 정보 조회 관련 기능 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 정보 상세 조회
     * @param paymentId 조회할 결제 ID
     * @return 결제 정보 DTO
     * @throws PaymentException 결제 정보 조회 실패 시
     */
    public PaymentDto getPaymentDetails(Long paymentId) {
        Instant start = Instant.now();
        try {
            if (paymentId == null) {
                throw new PaymentNotFoundException("결제 ID가 제공되지 않았습니다.");
            }

            log.info("결제 정보 조회 시작: 결제ID={}", paymentId);

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("결제 ID: " + paymentId + "를 찾을 수 없습니다."));

            PaymentDto paymentDto = convertToDto(payment);

            log.info("결제 정보 조회 완료: 결제ID={}, 결제유형={}",
                    paymentId, paymentDto.getPaymentType());
            return paymentDto;
        } catch (PaymentException e) {
            log.error("결제 정보 조회 실패: 결제ID={}, 에러코드={}, 메시지={}",
                    paymentId, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 조회 중 예상치 못한 오류 발생: 결제ID={}, 오류={}",
                    paymentId, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 정보 조회 처리 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * 사용자의 결제 내역 조회
     * @param email 사용자 이메일
     * @param success 성공한 결제만 조회할지 여부
     * @return 결제 정보 DTO 목록
     */
    public List<PaymentDto> getUserPayments(String email, boolean success) {
        Instant start = Instant.now();
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_ACCESS_DENIED,
                        "사용자 이메일 정보가 없습니다.");
            }

            log.info("사용자 결제 내역 조회 시작: 사용자={}, 성공여부={}", email, success);

            List<Payment> payments = paymentRepository.findByOrder_User_EmailAndIsSuccess(email, success);

            List<PaymentDto> result = payments.stream()
                    .map(this::convertToDto)
                    .toList();

            log.info("사용자 결제 내역 조회 완료: 사용자={}, 조회된 결제 수={}", email, result.size());
            return result;
        } catch (PaymentException e) {
            log.error("사용자 결제 내역 조회 실패: 사용자={}, 에러코드={}, 메시지={}",
                    email, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("사용자 결제 내역 조회 중 예상치 못한 오류 발생: 사용자={}, 오류={}",
                    email, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "사용자 결제 내역 조회 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("사용자 결제 내역 조회 처리 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * 판매자의 수금 내역 조회
     * @param email 판매자 이메일
     * @param success 성공한 결제만 조회할지 여부
     * @return 결제 정보 DTO 목록
     */
    public List<PaymentDto> getSellerPayments(String email, boolean success) {
        Instant start = Instant.now();
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_ACCESS_DENIED,
                        "판매자 이메일 정보가 없습니다.");
            }

            log.info("판매자 수금 내역 조회 시작: 판매자={}, 성공여부={}", email, success);

            List<Payment> payments = paymentRepository.findBySale_Seller_EmailAndIsSuccess(email, success);

            List<PaymentDto> result = payments.stream()
                    .map(this::convertToDto)
                    .toList();

            log.info("판매자 수금 내역 조회 완료: 판매자={}, 조회된 결제 수={}", email, result.size());
            return result;
        } catch (PaymentException e) {
            log.error("판매자 수금 내역 조회 실패: 판매자={}, 에러코드={}, 메시지={}",
                    email, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자 수금 내역 조회 중 예상치 못한 오류 발생: 판매자={}, 오류={}",
                    email, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "판매자 수금 내역 조회 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("판매자 수금 내역 조회 처리 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * ImpUid로 결제 정보 조회
     * @param impUid 결제 고유 ID (PG사 발급)
     * @return 카드 결제 정보 (없으면 null)
     */
    public CardPaymentDto findByImpUid(String impUid) {
        try {
            if (impUid == null || impUid.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                        "결제 고유 ID(impUid)가 제공되지 않았습니다.");
            }

            log.info("ImpUid로 결제 정보 조회 시작: impUid={}", impUid);

            CardPayment payment = paymentRepository.findByImpUid(impUid)
                    .orElse(null);

            if (payment == null) {
                log.info("ImpUid에 해당하는 결제 정보 없음: impUid={}", impUid);
                return null;
            }

            CardPaymentDto result = CardPaymentDto.fromEntity(payment);
            log.info("ImpUid로 결제 정보 조회 완료: impUid={}, 결제ID={}", impUid, result.getId());
            return result;
        } catch (PaymentException e) {
            log.error("ImpUid로 결제 정보 조회 실패: impUid={}, 에러코드={}, 메시지={}",
                    impUid, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ImpUid로 결제 정보 조회 중 예상치 못한 오류 발생: impUid={}, 오류={}",
                    impUid, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 고유 ID로 결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Payment 엔티티를 해당 타입의 DTO로 변환
     * @param payment 변환할 Payment 엔티티
     * @return 결제 타입에 맞는 PaymentDto 구현체
     * @throws UnknownPaymentTypeException 알 수 없는 결제 타입인 경우
     */
    private PaymentDto convertToDto(Payment payment) {
        if (payment instanceof GeneralPayment generalPayment) {
            return GeneralPaymentDto.fromEntity(generalPayment);
        } else if (payment instanceof CardPayment cardPayment) {
            return CardPaymentDto.fromEntity(cardPayment);
        } else if (payment instanceof AccountPayment accountPayment) {
            return AccountPaymentDto.fromEntity(accountPayment);
        } else {
            log.error("알 수 없는 결제 유형: 결제ID={}, 결제유형={}",
                    payment.getId(), payment.getClass().getSimpleName());
            throw new UnknownPaymentTypeException(
                    "알 수 없는 결제 유형입니다: " + payment.getClass().getSimpleName());
        }
    }
}