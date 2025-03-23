package com.fream.back.domain.payment.controller.command;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoDto;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.portone.PortOneApiClient;
import com.fream.back.domain.payment.service.command.PaymentInfoCommandService;
import com.fream.back.domain.payment.service.query.PaymentInfoQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment-info")
@RequiredArgsConstructor
@Slf4j
public class PaymentInfoController {

    private final PaymentInfoCommandService paymentInfoCommandService;
    private final PaymentInfoQueryService paymentInfoQueryService;
    private final PortOneApiClient portOneApiClient;

    // SecurityContextHolder에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_ACCESS_DENIED, "인증된 사용자 정보를 찾을 수 없습니다.");
    }

    // 결제 정보 생성
    @PostMapping
    public ResponseEntity<String> createPaymentInfo(@RequestBody @Validated PaymentInfoCreateDto dto) {
        try {
            log.info("결제 정보 생성 API 요청 시작");

            // 입력값 검증
            validatePaymentInfoCreateDto(dto);

            String email = extractEmailFromSecurityContext();
            paymentInfoCommandService.createPaymentInfo(email, dto);

            log.info("결제 정보 생성 API 요청 완료: 사용자={}", email);
            return ResponseEntity.ok("결제 정보가 성공적으로 생성되었습니다.");
        } catch (PaymentException e) {
            log.error("결제 정보 생성 API 오류: {}", e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        }
    }

    // 결제 정보 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePaymentInfo(@PathVariable("id") Long id) {
        try {
            log.info("결제 정보 삭제 API 요청 시작: 결제정보ID={}", id);

            if (id == null || id <= 0) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_DELETION_FAILED,
                        "유효하지 않은 결제 정보 ID입니다.");
            }

            String email = extractEmailFromSecurityContext();
            paymentInfoCommandService.deletePaymentInfo(email, id);

            log.info("결제 정보 삭제 API 요청 완료: 사용자={}, 결제정보ID={}", email, id);
            return ResponseEntity.ok("결제 정보가 성공적으로 삭제되었습니다.");
        } catch (PaymentException e) {
            log.error("결제 정보 삭제 API 오류: 결제정보ID={}, {}", id, e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        }
    }

    // 결제 정보 목록 조회
    @GetMapping
    public ResponseEntity<List<PaymentInfoDto>> getPaymentInfos() {
        try {
            log.info("결제 정보 목록 조회 API 요청 시작");

            String email = extractEmailFromSecurityContext();
            List<PaymentInfoDto> paymentInfos = paymentInfoQueryService.getPaymentInfos(email);

            log.info("결제 정보 목록 조회 API 요청 완료: 사용자={}, 조회된 결제정보 수={}",
                    email, paymentInfos.size());
            return ResponseEntity.ok(paymentInfos);
        } catch (PaymentException e) {
            log.error("결제 정보 목록 조회 API 오류: {}", e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        }
    }

    // 결제 정보 단일 조회
    @GetMapping("/{id}")
    public ResponseEntity<PaymentInfoDto> getPaymentInfo(@PathVariable("id") Long id) {
        try {
            log.info("단일 결제 정보 조회 API 요청 시작: 결제정보ID={}", id);

            if (id == null || id <= 0) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                        "유효하지 않은 결제 정보 ID입니다.");
            }

            String email = extractEmailFromSecurityContext();
            PaymentInfoDto paymentInfo = paymentInfoQueryService.getPaymentInfo(email, id);

            log.info("단일 결제 정보 조회 API 요청 완료: 사용자={}, 결제정보ID={}", email, id);
            return ResponseEntity.ok(paymentInfo);
        } catch (PaymentException e) {
            log.error("단일 결제 정보 조회 API 오류: 결제정보ID={}, {}", id, e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        }
    }

    // 테스트 결제 요청 및 환불 로직 테스트
    @PostMapping("/test-payment")
    public ResponseEntity<String> testPayment(@RequestBody PaymentInfoCreateDto dto) {
        try {
            log.info("테스트 결제 API 요청 시작");

            // 입력값 검증
            validatePaymentInfoCreateDto(dto);

            // 1. 테스트 결제 요청
            String impUid = portOneApiClient.requestTestPayment(dto);
            log.info("테스트 결제 성공: impUid={}", impUid);

            // 2. 테스트 결제 환불
            boolean refundSuccess = portOneApiClient.cancelTestPayment(impUid);
            log.info("테스트 결제 환불 결과: 성공={}, impUid={}", refundSuccess, impUid);

            if (refundSuccess) {
                return ResponseEntity.ok("테스트 결제와 환불이 성공적으로 수행되었습니다. 거래 고유번호: " + impUid);
            } else {
                return ResponseEntity.ok("테스트 결제는 성공했으나 환불에 실패했습니다. 거래 고유번호: " + impUid);
            }
        } catch (PaymentException e) {
            log.error("테스트 결제 API 오류: {}", e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        } catch (Exception e) {
            log.error("테스트 결제 API 예상치 못한 오류: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_API_ERROR,
                    "테스트 결제 또는 환불 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 결제 정보 유효성 검증
    private void validatePaymentInfoCreateDto(PaymentInfoCreateDto dto) {
        if (dto == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "결제 정보가 없습니다.");
        }

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
    }
}