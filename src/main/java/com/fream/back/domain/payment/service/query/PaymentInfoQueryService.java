package com.fream.back.domain.payment.service.query;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoDto;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.exception.PaymentInfoNotFoundException;
import com.fream.back.domain.payment.repository.PaymentInfoRepository;
import com.fream.back.domain.payment.service.PaymentEncryptionService;
import com.fream.back.domain.payment.util.PaymentCardUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 결제 정보 조회 서비스
 * 저장된 결제 정보 조회와 관련된 기능 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentInfoQueryService {

    private final PaymentInfoRepository paymentInfoRepository;
    private final PaymentEncryptionService encryptionService;

    /**
     * 사용자의 모든 결제 정보 목록 조회
     * @param email 사용자 이메일
     * @return 결제 정보 DTO 목록
     * @throws PaymentException 결제 정보 조회 실패 시
     */
    @Transactional(readOnly = true)
    public List<PaymentInfoDto> getPaymentInfos(String email) {
        Instant start = Instant.now();
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_ACCESS_DENIED,
                        "사용자 이메일 정보가 없습니다.");
            }

            log.info("결제 정보 목록 조회 시작: 사용자={}", email);

            List<PaymentInfo> paymentInfos = paymentInfoRepository.findAllByUser_Email(email);

            List<PaymentInfoDto> result = paymentInfos.stream()
                    .map(this::convertToDto)
                    .toList();

            log.info("결제 정보 목록 조회 완료: 사용자={}, 조회된 결제정보 수={}", email, result.size());
            return result;
        } catch (PaymentException e) {
            log.error("결제 정보 목록 조회 실패: 사용자={}, 에러코드={}, 메시지={}",
                    email, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 목록 조회 중 예상치 못한 오류 발생: 사용자={}, 오류={}",
                    email, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 정보 목록 조회 처리 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * 특정 결제 정보 단건 조회
     * @param email 사용자 이메일
     * @param id 조회할 결제 정보 ID
     * @return 결제 정보 DTO
     * @throws PaymentException 결제 정보 조회 실패 시
     */
    @Transactional(readOnly = true)
    public PaymentInfoDto getPaymentInfo(String email, Long id) {
        Instant start = Instant.now();
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

            PaymentInfoDto dto = convertToDto(paymentInfo);

            log.info("단일 결제 정보 조회 완료: 사용자={}, 결제정보ID={}", email, id);
            return dto;
        } catch (PaymentException e) {
            log.error("단일 결제 정보 조회 실패: 사용자={}, 결제정보ID={}, 에러코드={}, 메시지={}",
                    email, id, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("단일 결제 정보 조회 중 예상치 못한 오류 발생: 사용자={}, 결제정보ID={}, 오류={}",
                    email, id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("단일 결제 정보 조회 처리 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * 결제 처리를 위한 결제 정보 엔티티 조회
     * @param email 사용자 이메일
     * @param id 조회할 결제 정보 ID
     * @return 복호화된 정보를 가진 PaymentInfo 엔티티
     * @throws PaymentException 결제 정보 조회 실패 시
     */
    @Transactional(readOnly = true)
    public PaymentInfo getPaymentInfoEntity(String email, Long id) {
        Instant start = Instant.now();
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

            // 암호화된 민감 정보 복호화 (임시 객체 생성 - 영속성 컨텍스트에 영향 없음)
            PaymentInfo decryptedInfo = PaymentInfo.builder()
                    .id(paymentInfo.getId())
                    .user(paymentInfo.getUser())
                    .cardNumber(encryptionService.decrypt(paymentInfo.getCardNumber()))
                    .cardPassword(encryptionService.decrypt(paymentInfo.getCardPassword()))
                    .expirationDate(paymentInfo.getExpirationDate())
                    .birthDate(encryptionService.decrypt(paymentInfo.getBirthDate()))
                    .build();

            log.info("결제 정보 엔티티 조회 완료: 사용자={}, 결제정보ID={}", email, id);
            return decryptedInfo;
        } catch (PaymentException e) {
            log.error("결제 정보 엔티티 조회 실패: 사용자={}, 결제정보ID={}, 에러코드={}, 메시지={}",
                    email, id, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 엔티티 조회 중 예상치 못한 오류 발생: 사용자={}, 결제정보ID={}, 오류={}",
                    email, id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 정보 엔티티 조회 처리 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * PaymentInfo 엔티티를 DTO로 변환
     * 민감 정보는 마스킹 처리
     * @param entity 변환할 PaymentInfo 엔티티
     * @return 마스킹 처리된 PaymentInfoDto
     */
    private PaymentInfoDto convertToDto(PaymentInfo entity) {
        if (entity == null) {
            return null;
        }

        // 암호화된 정보는 복호화 후 마스킹 처리
        String decryptedCardNumber = encryptionService.decrypt(entity.getCardNumber());
        String decryptedBirthDate = encryptionService.decrypt(entity.getBirthDate());

        return PaymentInfoDto.builder()
                .id(entity.getId())
                .cardNumber(PaymentCardUtils.maskCardNumber(decryptedCardNumber))
                .cardPassword(PaymentCardUtils.maskCardPassword(entity.getCardPassword()))
                .expirationDate(entity.getExpirationDate())
                .birthDate(PaymentCardUtils.maskBirthDate(decryptedBirthDate))
                .build();
    }
}