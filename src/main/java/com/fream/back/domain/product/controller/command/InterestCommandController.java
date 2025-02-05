package com.fream.back.domain.product.controller.command;

import com.fream.back.domain.product.service.interest.InterestCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestCommandController {

    private final InterestCommandService interestCommandService;

    // SecurityContext에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    /**
     * 관심 상품 토글 (추가/삭제)
     *
     * @param productColorId  관심 상품의 ProductColor ID
     * @return 상태 메시지
     */
    @PostMapping("/{productColorId}/toggle")
    public ResponseEntity<String> toggleInterest(@PathVariable("productColorId") Long productColorId) {
        String userEmail = extractEmailFromSecurityContext();
        interestCommandService.toggleInterest(userEmail, productColorId);
        return ResponseEntity.ok("Interest toggled successfully.");
    }
}

