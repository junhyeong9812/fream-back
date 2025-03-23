package com.fream.back.domain.payment.service.command;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.exception.PaymentInfoNotFoundException;
import com.fream.back.domain.payment.portone.PortOneApiClient;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentInfoCommandService {

    private final UserRepository userRepository;
    private final PortOneApiClient portOneApiClient; // 포트원 API 클라이언트

    @Transactional
    public void createPaymentInfo(String email, PaymentInfoCreateDto dto) {
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_CREATION_FAILED,
                        "사용자 이메일 정보가 없습니다.");
            }

            if (dto == null) {
                throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                        "결제 정보가 없습니다.");
            }

            log.info("결제 정보 생성 시작: 사용자={}", email);

            // 1. 사용자 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_INFO_CREATION_FAILED,
                            "사용자를 찾을 수 없습니다. 이메일: " + email));

            // 2. 최대 결제 정보 저장 개수 확인
            if (user.getPaymentInfos().size() >= 5) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_LIMIT_EXCEEDED,
                        "최대 5개의 결제 정보만 저장할 수 있습니다.");
            }

            // 카드 정보 검증
            validateCardInfo(dto);

            // 3. 테스트 결제 요청
            String impUid;
            try {
                impUid = portOneApiClient.requestTestPayment(dto);
                log.debug("테스트 결제 요청 성공: 사용자={}, impUid={}", email, impUid);
            } catch (PaymentException e) {
                log.error("테스트 결제 요청 실패: 사용자={}, 오류={}", email, e.getMessage());
                throw e; // 재전파
            }

            // 4. 테스트 결제 환불
            boolean refundSuccess;
            try {
                refundSuccess = portOneApiClient.cancelTestPayment(impUid);
                log.debug("테스트 결제 환불 요청 결과: 성공={}, impUid={}", refundSuccess, impUid);
            } catch (PaymentException e) {
                log.error("테스트 결제 환불 요청 실패: 사용자={}, impUid={}, 오류={}",
                        email, impUid, e.getMessage());
                throw e; // 재전파
            }

            // 5. 환불 성공 여부 확인 후 결제 정보 저장
            if (refundSuccess) {
                PaymentInfo paymentInfo = PaymentInfo.builder()
                        .user(user)
                        .cardNumber(dto.getCardNumber())
                        .cardPassword(dto.getCardPassword())
                        .expirationDate(dto.getExpirationDate())
                        .birthDate(dto.getBirthDate())
                        .build();

                user.addPaymentInfo(paymentInfo);
                log.info("결제 정보 생성 완료: 사용자={}, 카드번호={}",
                        email, maskCardNumber(dto.getCardNumber()));
            } else {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_CREATION_FAILED,
                        "환불 처리에 실패하여 결제 정보를 저장할 수 없습니다.");
            }
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 생성 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_CREATION_FAILED,
                    "결제 정보 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public void deletePaymentInfo(String email, Long id) {
        try {
            if (email == null || email.isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_DELETION_FAILED,
                        "사용자 이메일 정보가 없습니다.");
            }

            if (id == null) {
                throw new PaymentInfoNotFoundException("삭제할 결제 정보 ID가 없습니다.");
            }

            log.info("결제 정보 삭제 시작: 사용자={}, 결제정보ID={}", email, id);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_INFO_DELETION_FAILED,
                            "사용자를 찾을 수 없습니다. 이메일: " + email));

            PaymentInfo paymentInfo = user.getPaymentInfos().stream()
                    .filter(pi -> pi.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new PaymentInfoNotFoundException(
                            "삭제할 결제 정보를 찾을 수 없습니다. ID: " + id));

            user.removePaymentInfo(paymentInfo);
            log.info("결제 정보 삭제 완료: 사용자={}, 결제정보ID={}", email, id);
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 정보 삭제 중 오류 발생: 사용자={}, 결제정보ID={}, 오류={}",
                    email, id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_DELETION_FAILED,
                    "결제 정보 삭제 중 오류가 발생했습니다: " + e.getMessage());
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

    // 카드 정보 유효성 검증
    private void validateCardInfo(PaymentInfoCreateDto dto) {
        if (dto.getCardNumber() == null || dto.getCardNumber().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "카드 번호는 필수 입력 값입니다.");
        }

        if (dto.getExpirationDate() == null || dto.getExpirationDate().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "카드 유효기간은 필수 입력 값입니다.");
        }

        if (dto.getBirthDate() == null || dto.getBirthDate().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "생년월일은 필수 입력 값입니다.");
        }

        if (dto.getCardPassword() == null || dto.getCardPassword().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "카드 비밀번호 앞 2자리는 필수 입력 값입니다.");
        }

        // 카드번호 형식 검증 - 숫자만 허용, 특정 길이 확인
        if (!dto.getCardNumber().matches("^[0-9]{15,16}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "카드 번호는 15-16자리 숫자여야 합니다.");
        }

        // 유효기간 형식 검증 - MM/YY 형식
        if (!dto.getExpirationDate().matches("^(0[1-9]|1[0-2])/[0-9]{2}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "카드 유효기간은 MM/YY 형식이어야 합니다.");
        }

        // 생년월일 형식 검증 - YYMMDD 형식
        if (!dto.getBirthDate().matches("^[0-9]{6}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "생년월일은 YYMMDD 형식의 6자리 숫자여야 합니다.");
        }

        // 카드 비밀번호 형식 검증 - 앞 2자리 숫자
        if (!dto.getCardPassword().matches("^[0-9]{2}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO,
                    "카드 비밀번호는 앞 2자리 숫자여야 합니다.");
        }
    }
}