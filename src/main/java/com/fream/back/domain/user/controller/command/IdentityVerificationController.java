package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.VerifiedCustomerDto;
import com.fream.back.domain.user.service.command.IdentityVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/identity-verification")
@RequiredArgsConstructor
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    /**
     * 본인인증 검증 API
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyIdentity(@RequestBody Map<String, String> request) {
        String identityVerificationId = request.get("identityVerificationId");

        if (identityVerificationId == null || identityVerificationId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "본인인증 ID가 필요합니다."
            ));
        }

        try {
            // 본인인증 서비스를 통해 검증
            VerifiedCustomerDto verifiedCustomer = identityVerificationService.verifyIdentity(identityVerificationId);

            // 민감한 정보 필터링
            Map<String, Object> safeCustomerInfo = new HashMap<>();
            safeCustomerInfo.put("name", verifiedCustomer.getName());
            safeCustomerInfo.put("phoneNumber", verifiedCustomer.getPhoneNumber());
            safeCustomerInfo.put("gender", verifiedCustomer.getGender());
            safeCustomerInfo.put("birthDate", verifiedCustomer.getBirthDate());
            safeCustomerInfo.put("isForeigner", verifiedCustomer.getIsForeigner());

            // 성공 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "본인인증이 완료되었습니다.");
            response.put("verifiedCustomer", safeCustomerInfo);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("본인인증 검증 중 오류 발생", e);

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}