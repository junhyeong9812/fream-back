package com.fream.back.domain.user.controller;

import com.fream.back.domain.user.dto.BankAccount.BankAccountDto;
import com.fream.back.domain.user.dto.BankAccount.BankAccountInfoDto;
import com.fream.back.domain.user.service.bankaccount.BankAccountCommandService;
import com.fream.back.domain.user.service.bankaccount.BankAccountQueryService;
import com.fream.back.domain.user.validate.BankAccountControllerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank-account")
@RequiredArgsConstructor
public class BankAccountController {
    private final BankAccountCommandService bankAccountCommandService;
    private final BankAccountQueryService bankAccountQueryService;


    // SecurityContextHolder에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다."); // 인증 실패 처리
    }

    // 입금 계좌 정보 생성 및 수정
    @PostMapping
    public ResponseEntity<String> createOrUpdateBankAccount(@RequestBody BankAccountDto dto) {
        try {
            // 1) DTO 검증
            BankAccountControllerValidator.validateBankAccountDto(dto);

            // 2) 검증 통과 후 로직
            String email = extractEmailFromSecurityContext();
            bankAccountCommandService.createOrUpdateBankAccount(email, dto);

            return ResponseEntity.ok("판매 정산 계좌가 성공적으로 등록/수정되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("계좌정보 등록/수정 처리 중 문제가 발생했습니다.");
        }
    }

    // 입금 계좌 정보 조회
    @GetMapping
    public ResponseEntity<BankAccountInfoDto> getBankAccount() {
        String email = extractEmailFromSecurityContext();
        BankAccountInfoDto bankAccountInfo = bankAccountQueryService.getBankAccount(email);
        return ResponseEntity.ok(bankAccountInfo);
    }

    // 입금 계좌 정보 삭제
    @DeleteMapping
    public ResponseEntity<String> deleteBankAccount() {
        String email = extractEmailFromSecurityContext();
        bankAccountCommandService.deleteBankAccount(email);
        return ResponseEntity.ok("판매 정산 계좌가 성공적으로 삭제되었습니다.");
    }
}
