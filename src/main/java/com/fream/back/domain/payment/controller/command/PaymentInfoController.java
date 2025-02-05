package com.fream.back.domain.payment.controller.command;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoDto;
import com.fream.back.domain.payment.portone.PortOneApiClient;
import com.fream.back.domain.payment.service.command.PaymentInfoCommandService;
import com.fream.back.domain.payment.service.query.PaymentInfoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-info")
@RequiredArgsConstructor
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
        throw new IllegalStateException("인증된 사용자가 없습니다."); // 인증 실패 처리
    }

    // 결제 정보 생성
    @PostMapping
    public ResponseEntity<String> createPaymentInfo(@RequestBody PaymentInfoCreateDto dto) {
        String email = extractEmailFromSecurityContext();
        paymentInfoCommandService.createPaymentInfo(email, dto);
        return ResponseEntity.ok("결제 정보가 성공적으로 생성되었습니다.");
    }

    // 결제 정보 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePaymentInfo(@PathVariable("id") Long id) {
        String email = extractEmailFromSecurityContext();
        paymentInfoCommandService.deletePaymentInfo(email, id);
        return ResponseEntity.ok("결제 정보가 성공적으로 삭제되었습니다.");
    }

    // 결제 정보 목록 조회
    @GetMapping
    public ResponseEntity<List<PaymentInfoDto>> getPaymentInfos() {
        String email = extractEmailFromSecurityContext();
        List<PaymentInfoDto> paymentInfos = paymentInfoQueryService.getPaymentInfos(email);
        return ResponseEntity.ok(paymentInfos);
    }

    // 결제 정보 단일 조회
    @GetMapping("/{id}")
    public ResponseEntity<PaymentInfoDto> getPaymentInfo(@PathVariable("id") Long id) {
        String email = extractEmailFromSecurityContext();
        PaymentInfoDto paymentInfo = paymentInfoQueryService.getPaymentInfo(email, id);
        return ResponseEntity.ok(paymentInfo);
    }

    // 테스트 결제 요청 및 환불 로직 테스트
    @PostMapping("/test-payment")
    public ResponseEntity<String> testPayment(@RequestBody PaymentInfoCreateDto dto) {
        try {
            System.out.println("Received DTO: " + dto);
            // 1. 테스트 결제 요청
            String impUid = portOneApiClient.requestTestPayment(dto);
            System.out.println(" 환불시작 ");
            // 2. 테스트 결제 환불
            portOneApiClient.cancelTestPayment(impUid);

            return ResponseEntity.ok("테스트 결제와 환불이 성공적으로 수행되었습니다. 거래 고유번호: " + impUid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("테스트 결제 또는 환불 실패: " + e.getMessage());
        }
    }
}
