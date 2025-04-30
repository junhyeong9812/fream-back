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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 결제 정보 컨트롤러
 * 결제 수단 관리(등록, 삭제, 조회) API 제공
 */
@RestController
@RequestMapping("/payment-info")
@RequiredArgsConstructor
@Slf4j
public class PaymentInfoController {

    private final PaymentInfoCommandService paymentInfoCommandService;
    private final PaymentInfoQueryService paymentInfoQueryService;
    private final PortOneApiClient portOneApiClient;

    /**
     * SecurityContext에서 사용자 이메일 추출
     * @return 인증된 사용자 이메일
     * @throws PaymentException 인증 정보가 없는 경우
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_ACCESS_DENIED, "인증된 사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 결제 정보 생성 API
     * @param dto 결제 정보 생성 DTO
     * @return 생성 결과 메시지
     */
    @PostMapping
    public ResponseEntity<String> createPaymentInfo(@RequestBody @Validated PaymentInfoCreateDto dto) {
        Instant start = Instant.now();
        try {
            log.info("결제 정보 생성 API 요청 시작");

            String email = extractEmailFromSecurityContext();
            paymentInfoCommandService.createPaymentInfo(email, dto);

            log.info("결제 정보 생성 API 요청 완료: 사용자={}", email);

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 정보 생성 API 처리 시간: {}ms", duration.toMillis());

            return ResponseEntity.ok("결제 정보가 성공적으로 생성되었습니다.");
        } catch (PaymentException e) {
            log.error("결제 정보 생성 API 오류: 에러코드={}, 메시지={}",
                    e.getErrorCode().getCode(), e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        } catch (Exception e) {
            log.error("결제 정보 생성 API 예상치 못한 오류: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_CREATION_FAILED,
                    "결제 정보 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 정보 삭제 API
     * @param id 삭제할 결제 정보 ID
     * @return 삭제 결과 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePaymentInfo(@PathVariable("id") Long id) {
        Instant start = Instant.now();
        try {
            log.info("결제 정보 삭제 API 요청 시작: 결제정보ID={}", id);

            if (id == null || id <= 0) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_DELETION_FAILED,
                        "유효하지 않은 결제 정보 ID입니다.");
            }

            String email = extractEmailFromSecurityContext();
            paymentInfoCommandService.deletePaymentInfo(email, id);

            log.info("결제 정보 삭제 API 요청 완료: 사용자={}, 결제정보ID={}", email, id);

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 정보 삭제 API 처리 시간: {}ms", duration.toMillis());

            return ResponseEntity.ok("결제 정보가 성공적으로 삭제되었습니다.");
        } catch (PaymentException e) {
            log.error("결제 정보 삭제 API 오류: 결제정보ID={}, 에러코드={}, 메시지={}",
                    id, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        } catch (Exception e) {
            log.error("결제 정보 삭제 API 예상치 못한 오류: 결제정보ID={}, 오류={}",
                    id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_INFO_DELETION_FAILED,
                    "결제 정보 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 정보 목록 조회 API
     * @return 사용자의 저장된 결제 정보 목록
     */
    @GetMapping
    public ResponseEntity<List<PaymentInfoDto>> getPaymentInfos() {
        Instant start = Instant.now();
        try {
            log.info("결제 정보 목록 조회 API 요청 시작");

            String email = extractEmailFromSecurityContext();
            List<PaymentInfoDto> paymentInfos = paymentInfoQueryService.getPaymentInfos(email);

            log.info("결제 정보 목록 조회 API 요청 완료: 사용자={}, 조회된 결제정보 수={}",
                    email, paymentInfos.size());

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 정보 목록 조회 API 처리 시간: {}ms", duration.toMillis());

            return ResponseEntity.ok(paymentInfos);
        } catch (PaymentException e) {
            log.error("결제 정보 목록 조회 API 오류: 에러코드={}, 메시지={}",
                    e.getErrorCode().getCode(), e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        } catch (Exception e) {
            log.error("결제 정보 목록 조회 API 예상치 못한 오류: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 정보 단일 조회 API
     * @param id 조회할 결제 정보 ID
     * @return 결제 정보 DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentInfoDto> getPaymentInfo(@PathVariable("id") Long id) {
        Instant start = Instant.now();
        try {
            log.info("단일 결제 정보 조회 API 요청 시작: 결제정보ID={}", id);

            if (id == null || id <= 0) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                        "유효하지 않은 결제 정보 ID입니다.");
            }

            String email = extractEmailFromSecurityContext();
            PaymentInfoDto paymentInfo = paymentInfoQueryService.getPaymentInfo(email, id);

            log.info("단일 결제 정보 조회 API 요청 완료: 사용자={}, 결제정보ID={}", email, id);

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("단일 결제 정보 조회 API 처리 시간: {}ms", duration.toMillis());

            return ResponseEntity.ok(paymentInfo);
        } catch (PaymentException e) {
            log.error("단일 결제 정보 조회 API 오류: 결제정보ID={}, 에러코드={}, 메시지={}",
                    id, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        } catch (Exception e) {
            log.error("단일 결제 정보 조회 API 예상치 못한 오류: 결제정보ID={}, 오류={}",
                    id, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_RETRIEVAL_FAILED,
                    "결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 테스트 결제 및 환불 API
     * 카드 정보 유효성 검증 용도
     * @param dto 결제 정보 생성 DTO
     * @return 테스트 결제 결과 메시지
     */
    @PostMapping("/test-payment")
    public ResponseEntity<String> testPayment(@RequestBody @Validated PaymentInfoCreateDto dto) {
        Instant start = Instant.now();
        try {
            log.info("테스트 결제 API 요청 시작");

            // 1. 테스트 결제 요청
            String impUid = portOneApiClient.requestTestPayment(dto);
            log.info("테스트 결제 성공: impUid={}", impUid);

            // 2. 테스트 결제 환불
            boolean refundSuccess = portOneApiClient.cancelTestPayment(impUid);
            log.info("테스트 결제 환불 결과: 성공={}, impUid={}", refundSuccess, impUid);

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("테스트 결제 API 처리 시간: {}ms", duration.toMillis());

            if (refundSuccess) {
                return ResponseEntity.ok("테스트 결제와 환불이 성공적으로 수행되었습니다. 거래 고유번호: " + impUid);
            } else {
                return ResponseEntity.ok("테스트 결제는 성공했으나 환불에 실패했습니다. 거래 고유번호: " + impUid);
            }
        } catch (PaymentException e) {
            log.error("테스트 결제 API 오류: 에러코드={}, 메시지={}",
                    e.getErrorCode().getCode(), e.getMessage());
            throw e; // 전역 예외 처리기에서 처리하도록 전파
        } catch (Exception e) {
            log.error("테스트 결제 API 예상치 못한 오류: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_API_ERROR,
                    "테스트 결제 또는 환불 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}